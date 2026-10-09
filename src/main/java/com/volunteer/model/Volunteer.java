package com.volunteer.model;

import com.volunteer.model.enums.AccountStatus;
import com.volunteer.model.enums.Role;

import java.util.List;

/**
 * Concrete User subclass representing a Volunteer.
 * Tracks volunteer participation statistics (verified hours, registered activities).
 * Demonstrates OOP Inheritance and Polymorphic method implementation.
 */
public class Volunteer extends User {

    private double totalVerifiedHours;
    private double pendingHours;
    private int activeSignupsCount;

    public Volunteer() {
        super();
        this.role = Role.VOLUNTEER;
        this.totalVerifiedHours = 0.0;
        this.pendingHours = 0.0;
        this.activeSignupsCount = 0;
    }

    public Volunteer(Long id, String fullName, String email, String passwordHash, AccountStatus accountStatus) {
        super(id, fullName, email, passwordHash, Role.VOLUNTEER, accountStatus);
        this.totalVerifiedHours = 0.0;
        this.pendingHours = 0.0;
        this.activeSignupsCount = 0;
    }

    // ====================================================================
    // Polymorphic Method Overrides
    // ====================================================================

    @Override
    public String getDashboardWelcomeMessage() {
        return "Welcome, " + fullName + "! Discover meaningful causes, register for events, and log your service hours.";
    }

    @Override
    public List<String> getPermittedCapabilities() {
        return List.of(
            "BROWSE_OPPORTUNITIES",
            "SIGNUP_OPPORTUNITY",
            "CANCEL_OWN_SIGNUP",
            "VIEW_MY_SIGNUPS",
            "SUBMIT_VOLUNTEER_HOURS",
            "VIEW_VOLUNTEER_HOURS",
            "MESSAGE_ORGANIZATION"
        );
    }

    @Override
    public boolean canPerformAction(String actionKey) {
        if (actionKey == null) return false;
        return getPermittedCapabilities().contains(actionKey.trim().toUpperCase());
    }

    @Override
    public String getRoleBadgeColorHex() {
        return "#16A34A"; // Emerald Green
    }

    // ====================================================================
    // Role-Specific Getters and Setters
    // ====================================================================

    public double getTotalVerifiedHours() {
        return totalVerifiedHours;
    }

    public void setTotalVerifiedHours(double totalVerifiedHours) {
        this.totalVerifiedHours = totalVerifiedHours;
    }

    public double getPendingHours() {
        return pendingHours;
    }

    public void setPendingHours(double pendingHours) {
        this.pendingHours = pendingHours;
    }

    public int getActiveSignupsCount() {
        return activeSignupsCount;
    }

    public void setActiveSignupsCount(int activeSignupsCount) {
        this.activeSignupsCount = activeSignupsCount;
    }
}
