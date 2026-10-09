package com.volunteer.model.enums;

/**
 * Enumeration representing verification status of logged volunteer hours.
 */
public enum VerificationStatus {
    PENDING("Pending Verification"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String displayName;

    VerificationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static VerificationStatus fromString(String statusStr) {
        if (statusStr == null) return PENDING;
        for (VerificationStatus s : values()) {
            if (s.name().equalsIgnoreCase(statusStr.trim())) {
                return s;
            }
        }
        return PENDING;
    }
}
