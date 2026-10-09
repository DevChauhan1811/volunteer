package com.volunteer.model;

import com.volunteer.model.enums.AccountStatus;
import com.volunteer.model.enums.Role;

import java.util.List;

/**
 * Concrete User subclass representing an Organization.
 * Contains both user credentials and organization profile information.
 * Demonstrates OOP Inheritance and Polymorphic method implementation.
 */
public class Organization extends User {

    private Long organizationId;
    private String organizationName;
    private String description;
    private String contactInformation;

    public Organization() {
        super();
        this.role = Role.ORGANIZATION;
    }

    public Organization(Long userId, String fullName, String email, String passwordHash, AccountStatus accountStatus,
                        Long organizationId, String organizationName, String description, String contactInformation) {
        super(userId, fullName, email, passwordHash, Role.ORGANIZATION, accountStatus);
        this.organizationId = organizationId;
        this.organizationName = organizationName;
        this.description = description;
        this.contactInformation = contactInformation;
    }

    // ====================================================================
    // Polymorphic Method Overrides
    // ====================================================================

    @Override
    public String getDashboardWelcomeMessage() {
        String displayName = (organizationName != null && !organizationName.isBlank()) ? organizationName : fullName;
        return "Welcome, " + displayName + "! Post community opportunities, track participants, and verify service hours.";
    }

    @Override
    public List<String> getPermittedCapabilities() {
        return List.of(
            "CREATE_OPPORTUNITY",
            "EDIT_OWN_OPPORTUNITY",
            "CANCEL_OWN_OPPORTUNITY",
            "VIEW_PARTICIPANTS",
            "VERIFY_HOURS",
            "MESSAGE_VOLUNTEERS",
            "UPDATE_ORG_PROFILE"
        );
    }

    @Override
    public boolean canPerformAction(String actionKey) {
        if (actionKey == null) return false;
        return getPermittedCapabilities().contains(actionKey.trim().toUpperCase());
    }

    @Override
    public String getRoleBadgeColorHex() {
        return "#2563EB"; // Royal Blue
    }

    // ====================================================================
    // Role-Specific Getters and Setters
    // ====================================================================

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactInformation() {
        return contactInformation;
    }

    public void setContactInformation(String contactInformation) {
        this.contactInformation = contactInformation;
    }
}
