package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.PaymentAccountRequest;
import com.subscriptiontracker.dto.PaymentAccountResponse;
import com.subscriptiontracker.dto.PaymentSyncLogResponse;
import com.subscriptiontracker.dto.TransactionImportRequest;
import com.subscriptiontracker.entity.PaymentAccount;
import com.subscriptiontracker.entity.PaymentSyncLog;
import com.subscriptiontracker.entity.Subscription;
import com.subscriptiontracker.entity.Transaction;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.repository.PaymentAccountRepository;
import com.subscriptiontracker.repository.PaymentSyncLogRepository;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.repository.TransactionRepository;
import com.subscriptiontracker.repository.UserRepository;
import com.subscriptiontracker.security.UserDetailsImpl;
import com.subscriptiontracker.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentIntegrationService {

    private final PaymentAccountRepository paymentAccountRepository;
    private final PaymentSyncLogRepository paymentSyncLogRepository;
    private final TransactionRepository transactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final Random random = new Random();
    private final Map<String, String> otpStore = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> otpVerifiedUntilStore = new ConcurrentHashMap<>();

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    public PaymentIntegrationService(
            PaymentAccountRepository paymentAccountRepository,
            PaymentSyncLogRepository paymentSyncLogRepository,
            TransactionRepository transactionRepository,
            SubscriptionRepository subscriptionRepository,
            UserRepository userRepository,
            RestTemplate restTemplate) {
        this.paymentAccountRepository = paymentAccountRepository;
        this.paymentSyncLogRepository = paymentSyncLogRepository;
        this.transactionRepository = transactionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }

    private User getCurrentUser() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public PaymentAccountResponse connectAccount(PaymentAccountRequest request) {
        User user = getCurrentUser();
        PaymentAccount account = new PaymentAccount();
        account.setAccountType(request.getAccountType());
        account.setAccountIdentifier(EncryptionUtil.encrypt(request.getAccountIdentifier() != null 
                ? request.getAccountIdentifier() : ""));
        account.setAccessToken(EncryptionUtil.encrypt(request.getAccessToken() != null 
                ? request.getAccessToken() : ""));
        account.setRefreshToken(EncryptionUtil.encrypt(request.getRefreshToken() != null 
                ? request.getRefreshToken() : ""));
        account.setUser(user);
        account.setIsActive(true);
        account.setSyncStatus("CONNECTED");
        account.setAutoSync(false);
        return mapToResponse(paymentAccountRepository.save(account));
    }

    public List<PaymentAccountResponse> getUserAccounts() {
        User user = getCurrentUser();
        return paymentAccountRepository.findByUserIdAndIsActiveTrue(user.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public void syncTransactions(Long accountId) {
        startSync(accountId);
    }

    public Map<String, String> requestSyncOtp(Long accountId) {
        PaymentAccount account = getAuthorizedAccount(accountId);
        String key = getOtpKey(account.getUser().getId(), accountId);
        String otp = String.format("%06d", random.nextInt(1_000_000));
        otpStore.put(key, otp);
        otpVerifiedUntilStore.remove(key);

        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP generated. Use this code to verify sync.");
        // Demo-only: returning OTP for local testing.
        response.put("otp", otp);
        return response;
    }

    public Map<String, String> verifySyncOtp(Long accountId, String code) {
        PaymentAccount account = getAuthorizedAccount(accountId);
        String key = getOtpKey(account.getUser().getId(), accountId);
        String expected = otpStore.get(key);
        if (expected == null || code == null || !expected.equals(code.trim())) {
            throw new RuntimeException("Invalid OTP");
        }
        otpVerifiedUntilStore.put(key, LocalDateTime.now().plusMinutes(5));

        return Map.of("message", "OTP verified. You can sync for next 5 minutes.");
    }

    public Map<String, String> startSync(Long accountId) {
        PaymentAccount account = getAuthorizedAccount(accountId);
        ensureOtpVerified(account.getUser().getId(), accountId);
        if ("SYNCING".equalsIgnoreCase(account.getSyncStatus())) {
            return Map.of("message", "Sync already in progress");
        }

        account.setSyncStatus("SYNCING");
        account.setLastSyncError(null);
        paymentAccountRepository.save(account);

        PaymentSyncLog log = new PaymentSyncLog();
        log.setUser(account.getUser());
        log.setPaymentAccount(account);
        log.setStatus("SYNCING");
        log.setDetails("Sync started");
        log.setStartedAt(LocalDateTime.now());
        PaymentSyncLog persistedLog = paymentSyncLogRepository.save(log);

        CompletableFuture.runAsync(() -> runSyncJob(accountId, persistedLog.getId()));
        return Map.of("message", "Sync started");
    }

    public PaymentAccountResponse toggleAutoSync(Long accountId, Boolean enabled) {
        PaymentAccount account = getAuthorizedAccount(accountId);
        account.setAutoSync(Boolean.TRUE.equals(enabled));
        return mapToResponse(paymentAccountRepository.save(account));
    }

    public List<PaymentSyncLogResponse> getSyncHistory() {
        User user = getCurrentUser();
        return paymentSyncLogRepository.findTop30ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapSyncLogResponse)
                .toList();
    }

    private void runSyncJob(Long accountId, Long logId) {
        PaymentAccount account = paymentAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        PaymentSyncLog log = paymentSyncLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Sync log not found"));

        try {
            List<Transaction> transactions = fetchTransactionsFromProvider(account);
            for (Transaction tx : transactions) {
                tx.setUser(account.getUser());
                tx.setPaymentAccount(account);
                transactionRepository.save(tx);
            }

            account.setLastSync(LocalDateTime.now());
            if (!transactions.isEmpty()) {
                account.setLastTransactionAt(transactions.get(0).getTransactionDate());
            }
            account.setSyncStatus("CONNECTED");
            account.setLastSyncError(null);
            paymentAccountRepository.save(account);

            detectSubscriptionsFromTransactions(account.getUser().getId());

            log.setStatus("CONNECTED");
            log.setDetails("Fetched " + transactions.size() + " transactions");
            log.setFinishedAt(LocalDateTime.now());
            paymentSyncLogRepository.save(log);
        } catch (Exception e) {
            account.setSyncStatus("FAILED");
            account.setLastSyncError(e.getMessage());
            paymentAccountRepository.save(account);

            log.setStatus("FAILED");
            log.setErrorMessage(e.getMessage());
            log.setFinishedAt(LocalDateTime.now());
            paymentSyncLogRepository.save(log);
        }
    }

    private List<Transaction> fetchTransactionsFromProvider(PaymentAccount account) {
        LocalDateTime now = LocalDateTime.now();
        Transaction recurring1 = new Transaction();
        recurring1.setTransactionId("tx-" + System.nanoTime());
        recurring1.setAmount(BigDecimal.valueOf(499));
        recurring1.setMerchantName("Netflix");
        recurring1.setDescription("Monthly Subscription");
        recurring1.setTransactionDate(now.minusDays(1));
        recurring1.setPaymentMethod(account.getAccountType());
        recurring1.setIsRecurring(true);

        Transaction recurring2 = new Transaction();
        recurring2.setTransactionId("tx-" + (System.nanoTime() + 1));
        recurring2.setAmount(BigDecimal.valueOf(149));
        recurring2.setMerchantName("Spotify");
        recurring2.setDescription("Premium Plan");
        recurring2.setTransactionDate(now.minusDays(2));
        recurring2.setPaymentMethod(account.getAccountType());
        recurring2.setIsRecurring(true);

        Transaction oneTime = new Transaction();
        oneTime.setTransactionId("tx-" + (System.nanoTime() + 2));
        oneTime.setAmount(BigDecimal.valueOf(899));
        oneTime.setMerchantName("Amazon");
        oneTime.setDescription("Shopping Order");
        oneTime.setTransactionDate(now.minusDays(3));
        oneTime.setPaymentMethod(account.getAccountType());
        oneTime.setIsRecurring(false);

        return List.of(recurring1, recurring2, oneTime);
    }

    public void importTransactions(List<TransactionImportRequest> transactions) {
        User user = getCurrentUser();
        for (TransactionImportRequest req : transactions) {
            Transaction tx = new Transaction();
            tx.setTransactionId(req.getTransactionId());
            tx.setAmount(req.getAmount());
            tx.setMerchantName(req.getMerchantName());
            tx.setDescription(req.getDescription());
            tx.setTransactionDate(req.getTransactionDate());
            tx.setPaymentMethod(req.getPaymentMethod());
            tx.setUser(user);
            transactionRepository.save(tx);
        }
        
        // Detect subscriptions using AI
        detectSubscriptionsFromTransactions(user.getId());
    }

    private void detectSubscriptionsFromTransactions(Long userId) {
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);

        createSubscriptionsFromRecurringTransactions(userId, transactions);
        
        // Call AI service to detect recurring patterns
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = Map.of(
                "transactions", transactions.stream()
                    .map(t -> Map.of(
                        "merchantName", t.getMerchantName() != null ? t.getMerchantName() : "",
                        "amount", t.getAmount().toString(),
                        "date", t.getTransactionDate().toString(),
                        "description", t.getDescription() != null ? t.getDescription() : ""
                    ))
                    .toList()
            );
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(
                aiServiceUrl + "/detect-subscriptions", 
                request, 
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> detected = (List<Map<String, Object>>) response.getBody().get("subscriptions");
                if (detected != null) {
                    for (@SuppressWarnings("unused") Map<String, Object> sub : detected) {
                        // Create subscription from AI detection
                        // This would call subscriptionService.createSubscription()
                    }
                }
            }
        } catch (Exception e) {
            // AI service not available - log and continue
            System.err.println("AI service unavailable: " + e.getMessage());
        }
    }

    private void createSubscriptionsFromRecurringTransactions(Long userId, List<Transaction> transactions) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<Subscription> existing = subscriptionRepository.findByUserIdOrderByNextRenewalDateAsc(userId);
        List<String> existingNames = existing.stream()
                .map(s -> s.getName().toLowerCase())
                .toList();

        for (Transaction tx : transactions) {
            if (!Boolean.TRUE.equals(tx.getIsRecurring()) || tx.getMerchantName() == null || tx.getAmount() == null) {
                continue;
            }
            String normalized = tx.getMerchantName().trim().toLowerCase();
            if (existingNames.contains(normalized)) {
                continue;
            }

            Subscription subscription = new Subscription();
            subscription.setUser(user);
            subscription.setName(tx.getMerchantName());
            subscription.setDescription("Auto-detected from " + tx.getPaymentMethod() + " transactions");
            subscription.setAmount(tx.getAmount());
            subscription.setBillingCycle("MONTHLY");
            LocalDate startDate = tx.getTransactionDate() != null
                    ? tx.getTransactionDate().toLocalDate()
                    : LocalDate.now();
            subscription.setStartDate(startDate);
            subscription.setNextRenewalDate(startDate.plusMonths(1));
            subscription.setCategory("Auto Detected");
            subscriptionRepository.save(subscription);
            existingNames.add(normalized);
        }
    }

    private PaymentAccount getAuthorizedAccount(Long accountId) {
        PaymentAccount account = paymentAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (!account.getUser().getId().equals(getCurrentUser().getId())) {
            throw new RuntimeException("Unauthorized");
        }
        return account;
    }

    private String getOtpKey(Long userId, Long accountId) {
        return userId + ":" + accountId;
    }

    private void ensureOtpVerified(Long userId, Long accountId) {
        LocalDateTime validUntil = otpVerifiedUntilStore.get(getOtpKey(userId, accountId));
        if (validUntil == null || validUntil.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP verification required before sync");
        }
    }

    private PaymentAccountResponse mapToResponse(PaymentAccount account) {
        PaymentAccountResponse response = new PaymentAccountResponse();
        response.setId(account.getId());
        response.setAccountType(account.getAccountType());
        response.setIsActive(account.getIsActive());
        response.setLastSync(account.getLastSync());
        response.setLastTransactionAt(account.getLastTransactionAt());
        response.setSyncStatus(account.getSyncStatus() != null ? account.getSyncStatus() : "CONNECTED");
        response.setLastSyncError(account.getLastSyncError());
        response.setAutoSync(account.getAutoSync());
        response.setCreatedAt(account.getCreatedAt());
        return response;
    }

    private PaymentSyncLogResponse mapSyncLogResponse(PaymentSyncLog log) {
        PaymentSyncLogResponse response = new PaymentSyncLogResponse();
        response.setId(log.getId());
        response.setPaymentAccountId(log.getPaymentAccount() != null ? log.getPaymentAccount().getId() : null);
        response.setPaymentAccountType(log.getPaymentAccount() != null ? log.getPaymentAccount().getAccountType() : null);
        response.setStatus(log.getStatus());
        response.setDetails(log.getDetails());
        response.setErrorMessage(log.getErrorMessage());
        response.setStartedAt(log.getStartedAt());
        response.setFinishedAt(log.getFinishedAt());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}
