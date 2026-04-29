package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.PaymentAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentAccountRepository extends JpaRepository<PaymentAccount, Long> {
    List<PaymentAccount> findByUserIdAndIsActiveTrue(Long userId);
    List<PaymentAccount> findByUserId(Long userId);
}
