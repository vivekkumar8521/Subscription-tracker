package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findTop100ByOrderByCreatedAtDesc();
    List<ActivityLog> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);
}
