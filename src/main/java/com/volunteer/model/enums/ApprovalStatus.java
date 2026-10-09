package com.volunteer.model.enums;

/**
 * Enumeration representing admin approval status for volunteer opportunities.
 */
public enum ApprovalStatus {
    PENDING("Pending Moderation"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String displayName;

    ApprovalStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ApprovalStatus fromString(String statusStr) {
        if (statusStr == null) return PENDING;
        for (ApprovalStatus s : values()) {
            if (s.name().equalsIgnoreCase(statusStr.trim())) {
                return s;
            }
        }
        return PENDING;
    }
}
