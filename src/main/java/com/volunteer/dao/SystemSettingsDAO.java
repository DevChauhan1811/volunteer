package com.volunteer.dao;

import com.volunteer.exception.DatabaseException;
import com.volunteer.model.SystemSetting;
import com.volunteer.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data Access Object for system-wide configuration settings and dashboard statistics.
 * Generates live database-driven participation metrics without hardcoded numbers.
 */
public class SystemSettingsDAO {

    public Optional<String> getSetting(String key) throws DatabaseException {
        String sql = "SELECT setting_value FROM system_settings WHERE setting_key = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("setting_value"));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed getting system setting '" + key + "': " + e.getMessage(), e);
        }
    }

    public List<SystemSetting> getAllSettings() throws DatabaseException {
        String sql = "SELECT setting_key, setting_value, updated_at, updated_by FROM system_settings ORDER BY setting_key ASC";

        List<SystemSetting> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                SystemSetting s = new SystemSetting();
                s.setKey(rs.getString("setting_key"));
                s.setValue(rs.getString("setting_value"));
                Timestamp u = rs.getTimestamp("updated_at");
                if (u != null) s.setUpdatedAt(u.toLocalDateTime());
                long by = rs.getLong("updated_by");
                if (!rs.wasNull()) s.setUpdatedBy(by);
                list.add(s);
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed retrieving system settings: " + e.getMessage(), e);
        }
    }

    public boolean updateSetting(String key, String value, Long updatedBy) throws DatabaseException {
        String sql = """
            INSERT INTO system_settings (setting_key, setting_value, updated_by)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_by = VALUES(updated_by)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, key);
            ps.setString(2, value);
            if (updatedBy != null) {
                ps.setLong(3, updatedBy);
            } else {
                ps.setNull(3, java.sql.Types.BIGINT);
            }

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed updating system setting '" + key + "': " + e.getMessage(), e);
        }
    }

    /**
     * Executes real SQL aggregation queries to compute live system overview statistics.
     * Demonstrates Generics & Collections with Map<String, Object>.
     */
    public Map<String, Object> getPlatformStatistics() throws DatabaseException {
        Map<String, Object> stats = new HashMap<>();

        String sql = """
            SELECT
                (SELECT COUNT(*) FROM users) AS total_users,
                (SELECT COUNT(*) FROM users WHERE role = 'ORGANIZATION') AS total_orgs,
                (SELECT COUNT(*) FROM users WHERE role = 'VOLUNTEER') AS total_volunteers,
                (SELECT COUNT(*) FROM opportunities) AS total_opportunities,
                (SELECT COUNT(*) FROM opportunities WHERE approval_status = 'PENDING') AS pending_opportunities,
                (SELECT COUNT(*) FROM signups WHERE registration_status = 'REGISTERED') AS total_active_signups,
                (SELECT COALESCE(SUM(hours_worked), 0.0) FROM volunteer_hours WHERE verification_status = 'APPROVED') AS total_verified_hours
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                stats.put("totalUsers", rs.getLong("total_users"));
                stats.put("totalOrganizations", rs.getLong("total_orgs"));
                stats.put("totalVolunteers", rs.getLong("total_volunteers"));
                stats.put("totalOpportunities", rs.getLong("total_opportunities"));
                stats.put("pendingOpportunities", rs.getLong("pending_opportunities"));
                stats.put("totalSignups", rs.getLong("total_active_signups"));
                stats.put("totalVerifiedHours", rs.getDouble("total_verified_hours"));
            }
            return stats;
        } catch (SQLException e) {
            throw new DatabaseException("Failed calculating live platform statistics: " + e.getMessage(), e);
        }
    }
}
