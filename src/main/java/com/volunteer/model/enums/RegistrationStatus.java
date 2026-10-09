package com.volunteer.model.enums;

/**
 * Enumeration representing volunteer registration statuses for an opportunity.
 */
public enum RegistrationStatus {
    REGISTERED("Registered"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed"),
    ATTENDED("Attended"),
    NO_SHOW("No Show");

    private final String displayName;

    RegistrationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RegistrationStatus fromString(String statusStr) {
        if (statusStr == null) return REGISTERED;
        for (RegistrationStatus s : values()) {
            if (s.name().equalsIgnoreCase(statusStr.trim())) {
                return s;
            }
        }
        return REGISTERED;
    }
}
