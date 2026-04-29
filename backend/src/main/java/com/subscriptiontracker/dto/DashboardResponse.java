package com.subscriptiontracker.dto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardResponse {

    private List<SubscriptionResponse> subscriptions;
    private BigDecimal totalMonthlyExpense;
    private List<SubscriptionResponse> upcomingReminders;

    public DashboardResponse() {
    }

    public DashboardResponse(List<SubscriptionResponse> subscriptions,
                            BigDecimal totalMonthlyExpense,
                            List<SubscriptionResponse> upcomingReminders) {
        this.subscriptions = subscriptions;
        this.totalMonthlyExpense = totalMonthlyExpense;
        this.upcomingReminders = upcomingReminders;
    }

    public List<SubscriptionResponse> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<SubscriptionResponse> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public BigDecimal getTotalMonthlyExpense() {
        return totalMonthlyExpense;
    }

    public void setTotalMonthlyExpense(BigDecimal totalMonthlyExpense) {
        this.totalMonthlyExpense = totalMonthlyExpense;
    }

    public List<SubscriptionResponse> getUpcomingReminders() {
        return upcomingReminders;
    }

    public void setUpcomingReminders(List<SubscriptionResponse> upcomingReminders) {
        this.upcomingReminders = upcomingReminders;
    }
}
