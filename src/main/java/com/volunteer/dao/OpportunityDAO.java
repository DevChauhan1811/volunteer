package com.volunteer.dao;

import com.volunteer.exception.DatabaseException;
import com.volunteer.model.Opportunity;
import com.volunteer.model.enums.ApprovalStatus;
import com.volunteer.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Volunteer Opportunities.
 * Features parameterized queries, dynamic filtering, and subquery signup counting.
 */
public class OpportunityDAO implements BaseDAO<Opportunity, Long> {

    private static final String BASE_SELECT = """
        SELECT o.id, o.organization_id, org.organization_name, o.title, o.description, o.category,
               o.event_date, o.start_time, o.end_time, o.location, o.capacity,
               o.approval_status, o.rejection_reason, o.created_at, o.updated_at,
               (SELECT COUNT(*) FROM signups s WHERE s.opportunity_id = o.id AND s.registration_status = 'REGISTERED') AS current_signups
        FROM opportunities o
        JOIN organizations org ON o.organization_id = org.id
    """;

    @Override
    public Opportunity create(Opportunity opp) throws DatabaseException {
        String sql = """
            INSERT INTO opportunities (organization_id, title, description, category, event_date,
                                       start_time, end_time, location, capacity, approval_status, rejection_reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, opp.getOrganizationId());
            ps.setString(2, opp.getTitle());
            ps.setString(3, opp.getDescription());
            ps.setString(4, opp.getCategory());
            ps.setDate(5, Date.valueOf(opp.getEventDate()));
            ps.setTime(6, Time.valueOf(opp.getStartTime()));
            ps.setTime(7, Time.valueOf(opp.getEndTime()));
            ps.setString(8, opp.getLocation());
            ps.setInt(9, opp.getCapacity());
            ps.setString(10, opp.getApprovalStatus().name());
            ps.setString(11, opp.getRejectionReason());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Failed to insert opportunity, no rows affected.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    opp.setId(rs.getLong(1));
                }
            }
            return opp;
        } catch (SQLException e) {
            throw new DatabaseException("Failed persisting opportunity: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Opportunity> findById(Long id) throws DatabaseException {
        String sql = BASE_SELECT + " WHERE o.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToOpportunity(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding opportunity #" + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Opportunity> findAll() throws DatabaseException {
        String sql = BASE_SELECT + " ORDER BY o.event_date ASC, o.start_time ASC";

        List<Opportunity> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToOpportunity(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed retrieving all opportunities: " + e.getMessage(), e);
        }
    }

    public List<Opportunity> findApproved(String keyword, String category, String location, LocalDate date) throws DatabaseException {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE o.approval_status = 'APPROVED'");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (LOWER(o.title) LIKE ? OR LOWER(o.description) LIKE ? OR LOWER(org.organization_name) LIKE ?)");
            String term = "%" + keyword.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (category != null && !category.trim().isEmpty() && !"All Categories".equalsIgnoreCase(category.trim())) {
            sql.append(" AND LOWER(o.category) = ?");
            params.add(category.trim().toLowerCase());
        }

        if (location != null && !location.trim().isEmpty()) {
            sql.append(" AND LOWER(o.location) LIKE ?");
            params.add("%" + location.trim().toLowerCase() + "%");
        }

        if (date != null) {
            sql.append(" AND o.event_date >= ?");
            params.add(Date.valueOf(date));
        } else {
            // Default: show events today or in the future
            sql.append(" AND o.event_date >= CURRENT_DATE");
        }

        sql.append(" ORDER BY o.event_date ASC, o.start_time ASC");

        List<Opportunity> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToOpportunity(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed querying approved opportunities: " + e.getMessage(), e);
        }
    }

    public List<Opportunity> findByOrganization(Long organizationId) throws DatabaseException {
        String sql = BASE_SELECT + " WHERE o.organization_id = ? ORDER BY o.event_date DESC";

        List<Opportunity> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, organizationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToOpportunity(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding opportunities for organization " + organizationId + ": " + e.getMessage(), e);
        }
    }

    public List<Opportunity> findByApprovalStatus(ApprovalStatus status) throws DatabaseException {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        if (status != null) {
            sql.append(" WHERE o.approval_status = ?");
        }
        sql.append(" ORDER BY o.created_at DESC");

        List<Opportunity> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (status != null) {
                ps.setString(1, status.name());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToOpportunity(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding opportunities by status: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Opportunity opp) throws DatabaseException {
        String sql = """
            UPDATE opportunities
            SET title = ?, description = ?, category = ?, event_date = ?,
                start_time = ?, end_time = ?, location = ?, capacity = ?,
                approval_status = ?, rejection_reason = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, opp.getTitle());
            ps.setString(2, opp.getDescription());
            ps.setString(3, opp.getCategory());
            ps.setDate(4, Date.valueOf(opp.getEventDate()));
            ps.setTime(5, Time.valueOf(opp.getStartTime()));
            ps.setTime(6, Time.valueOf(opp.getEndTime()));
            ps.setString(7, opp.getLocation());
            ps.setInt(8, opp.getCapacity());
            ps.setString(9, opp.getApprovalStatus().name());
            ps.setString(10, opp.getRejectionReason());
            ps.setLong(11, opp.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed updating opportunity: " + e.getMessage(), e);
        }
    }

    public boolean updateApprovalStatus(Long id, ApprovalStatus status, String rejectionReason) throws DatabaseException {
        String sql = "UPDATE opportunities SET approval_status = ?, rejection_reason = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setString(2, rejectionReason);
            ps.setLong(3, id);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed updating opportunity moderation status: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) throws DatabaseException {
        String sql = "DELETE FROM opportunities WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed deleting opportunity: " + e.getMessage(), e);
        }
    }

    private Opportunity mapResultSetToOpportunity(ResultSet rs) throws SQLException {
        Opportunity opp = new Opportunity();
        opp.setId(rs.getLong("id"));
        opp.setOrganizationId(rs.getLong("organization_id"));
        opp.setOrganizationName(rs.getString("organization_name"));
        opp.setTitle(rs.getString("title"));
        opp.setDescription(rs.getString("description"));
        opp.setCategory(rs.getString("category"));

        Date d = rs.getDate("event_date");
        if (d != null) opp.setEventDate(d.toLocalDate());

        Time st = rs.getTime("start_time");
        if (st != null) opp.setStartTime(st.toLocalTime());

        Time et = rs.getTime("end_time");
        if (et != null) opp.setEndTime(et.toLocalTime());

        opp.setLocation(rs.getString("location"));
        opp.setCapacity(rs.getInt("capacity"));
        opp.setCurrentSignups(rs.getInt("current_signups"));
        opp.setApprovalStatus(ApprovalStatus.fromString(rs.getString("approval_status")));
        opp.setRejectionReason(rs.getString("rejection_reason"));

        Timestamp c = rs.getTimestamp("created_at");
        if (c != null) opp.setCreatedAt(c.toLocalDateTime());
        Timestamp u = rs.getTimestamp("updated_at");
        if (u != null) opp.setUpdatedAt(u.toLocalDateTime());

        return opp;
    }
}
