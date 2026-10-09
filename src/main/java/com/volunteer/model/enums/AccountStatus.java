package com.volunteer.model.enums;

/**
 * Enumeration representing user account lifecycle states.
 */
public enum AccountStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    SUSPENDED("Suspended");

    private final String displayName;

    AccountStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AccountStatus fromString(String statusStr) {
        if (statusStr == null) return ACTIVE;
        for (AccountStatus s : values()) {
            if (s.name().equalsIgnoreCase(statusStr.trim())) {
                return s;
            }
        }
        return ACTIVE;
    }
}
