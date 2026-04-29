package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.ActivityLogResponse;
import com.subscriptiontracker.entity.ActivityLog;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.repository.ActivityLogRepository;
import com.subscriptiontracker.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityLogService {
    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository, UserRepository userRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
    }

    public void log(Long userId, String action, String details, String sourceIp) {
        ActivityLog log = new ActivityLog();
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            log.setUser(user);
        }
        log.setAction(action);
        log.setDetails(details);
        log.setSourceIp(sourceIp);
        activityLogRepository.save(log);
    }

    public List<ActivityLogResponse> getLatestLogs() {
        return activityLogRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ActivityLogResponse toResponse(ActivityLog log) {
        ActivityLogResponse response = new ActivityLogResponse();
        response.setId(log.getId());
        response.setUserId(log.getUser() != null ? log.getUser().getId() : null);
        response.setUsername(log.getUser() != null ? log.getUser().getUsername() : "System");
        response.setAction(log.getAction());
        response.setDetails(log.getDetails());
        response.setSourceIp(log.getSourceIp());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}
