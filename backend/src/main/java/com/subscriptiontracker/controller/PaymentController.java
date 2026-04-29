package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.PaymentAccountRequest;
import com.subscriptiontracker.dto.PaymentAccountResponse;
import com.subscriptiontracker.dto.PaymentSyncLogResponse;
import com.subscriptiontracker.dto.OtpVerificationRequest;
import com.subscriptiontracker.dto.ToggleAutoSyncRequest;
import com.subscriptiontracker.dto.TransactionImportRequest;
import com.subscriptiontracker.service.PaymentIntegrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentIntegrationService paymentService;

    public PaymentController(PaymentIntegrationService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/connect")
    public ResponseEntity<PaymentAccountResponse> connectAccount(@Valid @RequestBody PaymentAccountRequest request) {
        return ResponseEntity.ok(paymentService.connectAccount(request));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<PaymentAccountResponse>> getUserAccounts() {
        return ResponseEntity.ok(paymentService.getUserAccounts());
    }

    @PostMapping("/sync/{accountId}")
    public ResponseEntity<Map<String, String>> syncTransactions(@PathVariable Long accountId) {
        return ResponseEntity.ok(paymentService.startSync(accountId));
    }

    @PostMapping("/sync/{accountId}/request-otp")
    public ResponseEntity<Map<String, String>> requestSyncOtp(@PathVariable Long accountId) {
        return ResponseEntity.ok(paymentService.requestSyncOtp(accountId));
    }

    @PostMapping("/sync/{accountId}/verify-otp")
    public ResponseEntity<Map<String, String>> verifySyncOtp(
            @PathVariable Long accountId,
            @RequestBody OtpVerificationRequest request) {
        return ResponseEntity.ok(paymentService.verifySyncOtp(accountId, request.getCode()));
    }

    @PatchMapping("/accounts/{accountId}/auto-sync")
    public ResponseEntity<PaymentAccountResponse> toggleAutoSync(
            @PathVariable Long accountId,
            @RequestBody ToggleAutoSyncRequest request) {
        return ResponseEntity.ok(paymentService.toggleAutoSync(accountId, request.getEnabled()));
    }

    @GetMapping("/sync-history")
    public ResponseEntity<List<PaymentSyncLogResponse>> getSyncHistory() {
        return ResponseEntity.ok(paymentService.getSyncHistory());
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, String>> importTransactions(
            @RequestBody List<TransactionImportRequest> transactions) {
        paymentService.importTransactions(transactions);
        return ResponseEntity.ok(Map.of("message", "Transactions imported and analyzed"));
    }
}
