package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.*;
import com.subscriptiontracker.service.SubscriptionService;
import com.subscriptiontracker.service.ActivityLogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final ActivityLogService activityLogService;

    public SubscriptionController(SubscriptionService subscriptionService, ActivityLogService activityLogService) {
        this.subscriptionService = subscriptionService;
        this.activityLogService = activityLogService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(subscriptionService.getDashboard());
    }

    @GetMapping("/recommendations")
    public ResponseEntity<AiRecommendationResponse> getRecommendations() {
        return ResponseEntity.ok(subscriptionService.getRecommendations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> getSubscription(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(subscriptionService.getSubscription(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @Valid @RequestBody SubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.createSubscription(request);
        activityLogService.log(null, "SUBSCRIPTION_CREATED", "Created: " + response.getName(), null);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> updateSubscription(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionRequest request) {
        try {
            SubscriptionResponse response = subscriptionService.updateSubscription(id, request);
            activityLogService.log(null, "SUBSCRIPTION_UPDATED", "Updated: " + response.getName(), null);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(@PathVariable Long id) {
        try {
            subscriptionService.deleteSubscription(id);
            activityLogService.log(null, "SUBSCRIPTION_DELETED", "Deleted subscription id: " + id, null);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
