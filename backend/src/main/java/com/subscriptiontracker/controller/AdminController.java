package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.ActivityLogResponse;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.repository.UserRepository;
import com.subscriptiontracker.service.ActivityLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ActivityLogService activityLogService;

    public AdminController(UserRepository userRepository,
                           SubscriptionRepository subscriptionRepository,
                           ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.activityLogService = activityLogService;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats() {
        return ResponseEntity.ok(Map.of(
                "totalUsers", userRepository.count(),
                "totalSubscriptions", subscriptionRepository.count()
        ));
    }

    @GetMapping("/activity-logs")
    public ResponseEntity<java.util.List<ActivityLogResponse>> logs() {
        return ResponseEntity.ok(activityLogService.getLatestLogs());
    }
}
