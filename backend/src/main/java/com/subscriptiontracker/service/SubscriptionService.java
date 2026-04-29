package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.*;
import com.subscriptiontracker.entity.Subscription;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.repository.UserRepository;
import com.subscriptiontracker.security.UserDetailsImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                              UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public SubscriptionResponse createSubscription(SubscriptionRequest request) {
        User user = getCurrentUser();
        Subscription subscription = mapToEntity(request);
        subscription.setUser(user);
        subscription.setNextRenewalDate(calculateNextRenewalDate(
                subscription.getStartDate(), subscription.getBillingCycle()));
        return mapToResponse(subscriptionRepository.save(subscription));
    }

    public SubscriptionResponse updateSubscription(Long id, SubscriptionRequest request) {
        Subscription subscription = getSubscriptionForUser(id);
        subscription.setName(request.getName());
        subscription.setDescription(request.getDescription());
        subscription.setAmount(request.getAmount());
        subscription.setBillingCycle(request.getBillingCycle());
        subscription.setStartDate(request.getStartDate());
        subscription.setCategory(request.getCategory());
        subscription.setNextRenewalDate(calculateNextRenewalDate(
                subscription.getStartDate(), subscription.getBillingCycle()));
        return mapToResponse(subscriptionRepository.save(subscription));
    }

    public void deleteSubscription(Long id) {
        Subscription subscription = getSubscriptionForUser(id);
        subscriptionRepository.delete(subscription);
    }

    public SubscriptionResponse getSubscription(Long id) {
        return mapToResponse(getSubscriptionForUser(id));
    }

    public DashboardResponse getDashboard() {
        User user = getCurrentUser();
        List<Subscription> allSubscriptions = subscriptionRepository
                .findByUserIdOrderByNextRenewalDateAsc(user.getId());

        BigDecimal totalMonthlyExpense = calculateTotalMonthlyExpense(allSubscriptions);
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(14);
        List<Subscription> upcoming = subscriptionRepository
                .findByUserIdAndNextRenewalDateBetween(user.getId(), today, endDate);

        List<SubscriptionResponse> subscriptionResponses = allSubscriptions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        List<SubscriptionResponse> reminderResponses = upcoming.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new DashboardResponse(subscriptionResponses, totalMonthlyExpense, reminderResponses);
    }

    public AiRecommendationResponse getRecommendations() {
        User user = getCurrentUser();
        List<Subscription> subscriptions = subscriptionRepository.findByUserIdOrderByNextRenewalDateAsc(user.getId());
        List<String> suggestions = new ArrayList<>();

        Map<String, Integer> keywordCount = new HashMap<>();
        for (Subscription s : subscriptions) {
            String name = s.getName() == null ? "" : s.getName().toLowerCase(Locale.ROOT);
            if (name.contains("spotify")) keywordCount.merge("music", 1, Integer::sum);
            if (name.contains("youtube")) keywordCount.merge("music", 1, Integer::sum);
            if (name.contains("netflix")) keywordCount.merge("ott", 1, Integer::sum);
            if (name.contains("prime")) keywordCount.merge("ott", 1, Integer::sum);
        }

        if (keywordCount.getOrDefault("music", 0) > 1) {
            suggestions.add("You may have duplicate music subscriptions. Compare Spotify vs YouTube Premium plans.");
        }
        if (keywordCount.getOrDefault("ott", 0) > 1) {
            suggestions.add("Multiple OTT services detected. Consider rotating plans monthly to save costs.");
        }

        for (Subscription s : subscriptions) {
            if (s.getAmount() != null && s.getAmount().doubleValue() > 30) {
                suggestions.add("High monthly spend on " + s.getName() + ". Check annual billing discount.");
                break;
            }
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Spending looks healthy. Enable auto-sync to unlock stronger AI insights.");
        }

        return new AiRecommendationResponse(suggestions);
    }

    private Subscription getSubscriptionForUser(Long id) {
        User user = getCurrentUser();
        return subscriptionRepository.findById(id)
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
    }

    private LocalDate calculateNextRenewalDate(LocalDate startDate, String billingCycle) {
        LocalDate today = LocalDate.now();
        LocalDate nextDate = startDate;

        while (nextDate.isBefore(today)) {
            switch (billingCycle.toUpperCase()) {
                case "MONTHLY" -> nextDate = nextDate.plusMonths(1);
                case "QUARTERLY" -> nextDate = nextDate.plusMonths(3);
                case "YEARLY" -> nextDate = nextDate.plusYears(1);
                case "WEEKLY" -> nextDate = nextDate.plusWeeks(1);
                default -> nextDate = nextDate.plusMonths(1);
            }
        }
        return nextDate;
    }

    private BigDecimal calculateTotalMonthlyExpense(List<Subscription> subscriptions) {
        BigDecimal total = BigDecimal.ZERO;
        for (Subscription s : subscriptions) {
            BigDecimal monthlyAmount = switch (s.getBillingCycle().toUpperCase()) {
                case "MONTHLY" -> s.getAmount();
                case "QUARTERLY" -> s.getAmount().divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
                case "YEARLY" -> s.getAmount().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
                case "WEEKLY" -> s.getAmount().multiply(BigDecimal.valueOf(52)).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
                default -> s.getAmount();
            };
            total = total.add(monthlyAmount);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private Subscription mapToEntity(SubscriptionRequest request) {
        Subscription subscription = new Subscription();
        subscription.setName(request.getName());
        subscription.setDescription(request.getDescription());
        subscription.setAmount(request.getAmount());
        subscription.setBillingCycle(request.getBillingCycle());
        subscription.setStartDate(request.getStartDate());
        subscription.setCategory(request.getCategory());
        return subscription;
    }

    private SubscriptionResponse mapToResponse(Subscription subscription) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setId(subscription.getId());
        response.setName(subscription.getName());
        response.setDescription(subscription.getDescription());
        response.setAmount(subscription.getAmount());
        response.setBillingCycle(subscription.getBillingCycle());
        response.setStartDate(subscription.getStartDate());
        response.setNextRenewalDate(subscription.getNextRenewalDate());
        response.setCategory(subscription.getCategory());
        response.setCreatedAt(subscription.getCreatedAt());
        return response;
    }
}
