package com.subscriptiontracker.service;

import com.subscriptiontracker.entity.Notification;
import com.subscriptiontracker.entity.Subscription;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.repository.NotificationRepository;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.repository.UserRepository;
import com.subscriptiontracker.security.UserDetailsImpl;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    private User getCurrentUser() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<Notification> getUserNotifications() {
        User user = getCurrentUser();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public Long getUnreadCount() {
        User user = getCurrentUser();
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!notification.getUser().getId().equals(getCurrentUser().getId())) {
            throw new RuntimeException("Unauthorized");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Scheduled(cron = "0 0 9 * * *") // Run daily at 9 AM
    public void checkUpcomingRenewals() {
        List<User> users = userRepository.findAll();
        LocalDate today = LocalDate.now();
        LocalDate threeDaysLater = today.plusDays(3);

        for (User user : users) {
            try {
                List<Subscription> subscriptions = subscriptionRepository
                        .findByUserIdAndNextRenewalDateBetween(user.getId(), today, threeDaysLater);
                
                for (Subscription sub : subscriptions) {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setSubscription(sub);
                    notification.setTitle("Upcoming Renewal: " + sub.getName());
                    notification.setMessage(sub.getName() + " will renew on " + sub.getNextRenewalDate() + 
                                         " for $" + sub.getAmount());
                    notification.setType("RENEWAL_REMINDER");
                    notificationRepository.save(notification);
                }
            } catch (Exception e) {
                // Skip user if error
            }
        }
    }
}
