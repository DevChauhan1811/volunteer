package com.volunteer.service.impl;

import com.volunteer.dao.SystemSettingsDAO;
import com.volunteer.exception.DatabaseException;
import com.volunteer.exception.UnauthorizedException;
import com.volunteer.exception.ValidationException;
import com.volunteer.model.SystemSetting;
import com.volunteer.model.User;
import com.volunteer.service.SystemSettingsService;
import com.volunteer.util.ValidationUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service implementation for administrative settings and live database statistics overview.
 */
public class SystemSettingsServiceImpl implements SystemSettingsService {

    private final SystemSettingsDAO settingsDAO;

    public SystemSettingsServiceImpl() {
        this.settingsDAO = new SystemSettingsDAO();
    }

    public SystemSettingsServiceImpl(SystemSettingsDAO settingsDAO) {
        this.settingsDAO = settingsDAO;
    }

    @Override
    public String getSetting(String key, String defaultValue) throws DatabaseException {
        if (key == null) return defaultValue;
        Optional<String> val = settingsDAO.getSetting(key);
        return val.orElse(defaultValue);
    }

    @Override
    public int getIntSetting(String key, int defaultValue) throws DatabaseException {
        String val = getSetting(key, null);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public boolean getBooleanSetting(String key, boolean defaultValue) throws DatabaseException {
        String val = getSetting(key, null);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val.trim());
    }

    @Override
    public void updateSetting(User initiator, String key, String value)
            throws UnauthorizedException, ValidationException, DatabaseException {

        if (initiator == null || !initiator.isAdmin()) {
            throw new UnauthorizedException("Only administrators can modify system settings.");
        }
        ValidationUtil.validateNotEmpty(key, "settingKey", "Setting key");
        ValidationUtil.validateNotEmpty(value, "settingValue", "Setting value");

        if ("max_volunteer_capacity".equalsIgnoreCase(key.trim())) {
            try {
                int cap = Integer.parseInt(value.trim());
                if (cap <= 0) {
                    throw new ValidationException("settingValue", "Maximum volunteer capacity must be greater than zero.");
                }
            } catch (NumberFormatException nfe) {
                throw new ValidationException("settingValue", "Maximum volunteer capacity must be a valid integer.");
            }
        }

        settingsDAO.updateSetting(key.trim(), value.trim(), initiator.getId());
    }

    @Override
    public List<SystemSetting> getAllSettings(User initiator)
            throws UnauthorizedException, DatabaseException {

        if (initiator == null || !initiator.isAdmin()) {
            throw new UnauthorizedException("Only administrators can view platform configuration.");
        }
        return settingsDAO.getAllSettings();
    }

    @Override
    public Map<String, Object> getPlatformOverviewStats(User initiator)
            throws UnauthorizedException, DatabaseException {

        if (initiator == null || !initiator.isAdmin()) {
            throw new UnauthorizedException("Only administrators can view platform overview statistics.");
        }
        return settingsDAO.getPlatformStatistics();
    }
}
