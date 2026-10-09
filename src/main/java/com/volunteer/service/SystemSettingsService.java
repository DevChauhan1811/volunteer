package com.volunteer.service;

import com.volunteer.exception.DatabaseException;
import com.volunteer.exception.UnauthorizedException;
import com.volunteer.exception.ValidationException;
import com.volunteer.model.SystemSetting;
import com.volunteer.model.User;

import java.util.List;
import java.util.Map;

/**
 * Service contract for administrative settings management and participation statistics overview.
 * Demonstrates Collections and Generics through Map<String, Object> and List<SystemSetting>.
 */
public interface SystemSettingsService {

    /**
     * Gets a system setting value by key, returning defaultValue if not configured.
     */
    String getSetting(String key, String defaultValue) throws DatabaseException;

    /**
     * Gets an integer setting value (e.g. max capacity).
     */
    int getIntSetting(String key, int defaultValue) throws DatabaseException;

    /**
     * Gets a boolean setting value (e.g. requires approval).
     */
    boolean getBooleanSetting(String key, boolean defaultValue) throws DatabaseException;

    /**
     * Updates a system setting. Restricted to Administrators.
     */
    void updateSetting(User initiator, String key, String value)
        throws UnauthorizedException, ValidationException, DatabaseException;

    /**
     * Retrieves all system settings.
     */
    List<SystemSetting> getAllSettings(User initiator)
        throws UnauthorizedException, DatabaseException;

    /**
     * Retrieves live platform overview statistics for the Admin Dashboard.
     * Returned as a type-safe Map<String, Object> containing real database metrics:
     * - totalUsers
     * - totalOrganizations
     * - totalVolunteers
     * - totalOpportunities
     * - pendingOpportunities
     * - totalSignups
     * - totalVerifiedHours
     */
    Map<String, Object> getPlatformOverviewStats(User initiator)
        throws UnauthorizedException, DatabaseException;
}
