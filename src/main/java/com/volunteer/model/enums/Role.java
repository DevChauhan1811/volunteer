package com.volunteer.model.enums;

/**
 * Enumeration representing user roles in the platform.
 */
public enum Role {
    ADMIN("Administrator"),
    ORGANIZATION("Organization"),
    VOLUNTEER("Volunteer");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Role fromString(String roleStr) {
        if (roleStr == null) return VOLUNTEER;
        for (Role r : values()) {
            if (r.name().equalsIgnoreCase(roleStr.trim())) {
                return r;
            }
        }
        return VOLUNTEER;
    }
}
