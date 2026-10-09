package com.volunteer.model;

import com.volunteer.model.enums.AccountStatus;
import com.volunteer.model.enums.Role;

import java.util.List;

/**
 * Concrete User subclass representing an Administrator.
 * Demonstrates OOP Inheritance and Polymorphic method implementation.
 */
public class Admin extends User {

    private String adminLevel;

    public Admin() {
        super();
        this.role = Role.ADMIN;
        this.adminLevel = "SUPER_ADMIN";
    }

    public Admin(Long id, String fullName, String email, String passwordHash, AccountStatus accountStatus) {
        super(id, fullName, email, passwordHash, Role.ADMIN, accountStatus);
        this.adminLevel = "SUPER_ADMIN";
    }

    // ====================================================================
    // Polymorphic Method Overrides
    // ====================================================================

    @Override
    public String getDashboardWelcomeMessage() {
        return "Welcome, Administrator " + fullName + "! Platform moderation, user management, and system settings are active.";
    }

    @Override
    public List<String> getPermittedCapabilities() {
        return List.of(
            "MANAGE_USERS",
            "MODERATE_OPPORTUNITIES",
            "APPROVE_OPPORTUNITY",
            "REJECT_OPPORTUNITY",
            "SYSTEM_SETTINGS",
            "VIEW_STATISTICS",
            "DEACTIVATE_USER"
        );
    }

    @Override
    public boolean canPerformAction(String actionKey) {
        if (actionKey == null) return false;
        return getPermittedCapabilities().contains(actionKey.trim().toUpperCase());
    }

    @Override
    public String getRoleBadgeColorHex() {
        return "#DC2626"; // Crimson Red
    }

    // ====================================================================
    // Role-Specific Getters and Setters
    // ====================================================================

    public String getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(String adminLevel) {
        this.adminLevel = adminLevel;
    }
}
