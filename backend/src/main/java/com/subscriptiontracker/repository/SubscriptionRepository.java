package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserIdOrderByNextRenewalDateAsc(Long userId);
    List<Subscription> findByUserIdAndNextRenewalDateBetween(Long userId, LocalDate start, LocalDate end);
}
