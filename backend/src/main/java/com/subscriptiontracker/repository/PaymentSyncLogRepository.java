package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.PaymentSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentSyncLogRepository extends JpaRepository<PaymentSyncLog, Long> {
    List<PaymentSyncLog> findTop30ByUserIdOrderByCreatedAtDesc(Long userId);
}
