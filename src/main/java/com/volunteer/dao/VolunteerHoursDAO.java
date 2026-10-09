package com.volunteer.dao;

import com.volunteer.exception.DatabaseException;
import com.volunteer.model.VolunteerHours;
import com.volunteer.model.enums.VerificationStatus;
import com.volunteer.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for VolunteerHours entities.
 * Handles hour submissions, verification status updates, and total hour aggregations.
 */
public class VolunteerHoursDAO implements BaseDAO<VolunteerHours, Long> {

    private static final String BASE_SELECT = """
        SELECT vh.id, vh.volunteer_id, u.full_name AS volunteer_name,
               vh.opportunity_id, o.title AS opportunity_title,
               vh.hours_worked, vh.work_date, vh.description,
               vh.verification_status, vh.verified_by,
               org.organization_name AS verifier_org_name, vh.created_at
        FROM volunteer_hours vh
        JOIN users u ON vh.volunteer_id = u.id
        JOIN opportunities o ON vh.opportunity_id = o.id
        LEFT JOIN organizations org ON vh.verified_by = org.id
    """;

    @Override
    public VolunteerHours create(VolunteerHours record) throws DatabaseException {
        String sql = """
            INSERT INTO volunteer_hours (volunteer_id, opportunity_id, hours_worked, work_date, description, verification_status, verified_by)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, record.getVolunteerId());
            ps.setLong(2, record.getOpportunityId());
            ps.setDouble(3, record.getHoursWorked());
            ps.setDate(4, Date.valueOf(record.getWorkDate()));
            ps.setString(5, record.getDescription());
            ps.setString(6, record.getVerificationStatus().name());

            if (record.getVerifiedBy() != null) {
                ps.setLong(7, record.getVerifiedBy());
            } else {
                ps.setNull(7, Types.BIGINT);
            }

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Failed inserting volunteer hours record.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    record.setId(rs.getLong(1));
                }
            }
            return record;
        } catch (SQLException e) {
            throw new DatabaseException("Failed recording volunteer hours: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<VolunteerHours> findById(Long id) throws DatabaseException {
        String sql = BASE_SELECT + " WHERE vh.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToHours(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding volunteer hours record #" + id + ": " + e.getMessage(), e);
        }
    }

    public List<VolunteerHours> findByVolunteer(Long volunteerId) throws DatabaseException {
        String sql = BASE_SELECT + " WHERE vh.volunteer_id = ? ORDER BY vh.work_date DESC, vh.created_at DESC";

        List<VolunteerHours> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, volunteerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToHours(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding hours for volunteer " + volunteerId + ": " + e.getMessage(), e);
        }
    }

    public List<VolunteerHours> findPendingByOrganization(Long organizationId) throws DatabaseException {
        String sql = BASE_SELECT + " WHERE o.organization_id = ? AND vh.verification_status = 'PENDING' ORDER BY vh.work_date ASC";

        List<VolunteerHours> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, organizationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToHours(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding pending hours for org " + organizationId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<VolunteerHours> findAll() throws DatabaseException {
        String sql = BASE_SELECT + " ORDER BY vh.created_at DESC";

        List<VolunteerHours> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToHours(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed retrieving volunteer hours: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(VolunteerHours record) throws DatabaseException {
        String sql = "UPDATE volunteer_hours SET hours_worked = ?, work_date = ?, description = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, record.getHoursWorked());
            ps.setDate(2, Date.valueOf(record.getWorkDate()));
            ps.setString(3, record.getDescription());
            ps.setLong(4, record.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed updating hours record: " + e.getMessage(), e);
        }
    }

    public boolean updateVerificationStatus(Long recordId, VerificationStatus status, Long verifiedByOrgId) throws DatabaseException {
        String sql = "UPDATE volunteer_hours SET verification_status = ?, verified_by = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            if (verifiedByOrgId != null) {
                ps.setLong(2, verifiedByOrgId);
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setLong(3, recordId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed updating hour verification status: " + e.getMessage(), e);
        }
    }

    public double getTotalVerifiedHoursForVolunteer(Long volunteerId) throws DatabaseException {
        String sql = "SELECT COALESCE(SUM(hours_worked), 0.0) FROM volunteer_hours WHERE volunteer_id = ? AND verification_status = 'APPROVED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, volunteerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
            return 0.0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed calculating verified hours: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) throws DatabaseException {
        String sql = "DELETE FROM volunteer_hours WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed deleting hours record: " + e.getMessage(), e);
        }
    }

    private VolunteerHours mapResultSetToHours(ResultSet rs) throws SQLException {
        VolunteerHours vh = new VolunteerHours();
        vh.setId(rs.getLong("id"));
        vh.setVolunteerId(rs.getLong("volunteer_id"));
        vh.setVolunteerName(rs.getString("volunteer_name"));
        vh.setOpportunityId(rs.getLong("opportunity_id"));
        vh.setOpportunityTitle(rs.getString("opportunity_title"));
        vh.setHoursWorked(rs.getDouble("hours_worked"));

        Date d = rs.getDate("work_date");
        if (d != null) vh.setWorkDate(d.toLocalDate());

        vh.setDescription(rs.getString("description"));
        vh.setVerificationStatus(VerificationStatus.fromString(rs.getString("verification_status")));

        long verifiedBy = rs.getLong("verified_by");
        if (!rs.wasNull()) {
            vh.setVerifiedBy(verifiedBy);
        }
        vh.setVerifierOrgName(rs.getString("verifier_org_name"));

        Timestamp c = rs.getTimestamp("created_at");
        if (c != null) vh.setCreatedAt(c.toLocalDateTime());

        return vh;
    }
}
