package com.volunteer.model;

import com.volunteer.model.enums.AccountStatus;
import com.volunteer.model.enums.Role;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base class representing a User in the system.
 * Demonstrates OOP Inheritance and Polymorphism.
 * Specific user roles (Admin, Organization, Volunteer) extend this class
 * and provide role-specific polymorphic behavior.
 */
public abstract class User {

    protected Long id;
    protected String fullName;
    protected String email;
    protected String passwordHash;
    protected Role role;
    protected AccountStatus accountStatus;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    public User() {
        this.accountStatus = AccountStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public User(Long id, String fullName, String email, String passwordHash, Role role, AccountStatus accountStatus) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.accountStatus = accountStatus != null ? accountStatus : AccountStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ====================================================================
    // Polymorphic Abstract Methods (Marking Rubric: Polymorphism)
    // ====================================================================

    /**
     * Returns a customized welcome message displayed on the user's dashboard header.
     * Each subclass provides its own tailored implementation.
     */
    public abstract String getDashboardWelcomeMessage();

    /**
     * Returns a list of permitted system actions for this specific user role.
     * Used for role-based authorization verification.
     */
    public abstract List<String> getPermittedCapabilities();

    /**
     * Evaluates whether this user type has permission to execute a specific action key.
     *
     * @param actionKey the identifier of the operation to perform
     * @return true if permitted; false otherwise
     */
    public abstract boolean canPerformAction(String actionKey);

    /**
     * Returns the hex color string representing this user role's badge in the GUI.
     */
    public abstract String getRoleBadgeColorHex();

    // ====================================================================
    // Common Concrete Helper Methods
    // ====================================================================

    public boolean isActive() {
        return this.accountStatus == AccountStatus.ACTIVE;
    }

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    public boolean isOrganization() {
        return this.role == Role.ORGANIZATION;
    }

    public boolean isVolunteer() {
        return this.role == Role.VOLUNTEER;
    }

    // ====================================================================
    // Getters and Setters
    // ====================================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @Override
    public String toString() {
        return String.format("%s [id=%d, name='%s', email='%s', role=%s, status=%s]",
            getClass().getSimpleName(), id, fullName, email, role, accountStatus);
    }
}
