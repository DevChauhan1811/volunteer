package com.volunteer.dao;

import com.volunteer.exception.DatabaseException;
import com.volunteer.model.Organization;
import com.volunteer.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Organization entity profiles.
 */
public class OrganizationDAO implements BaseDAO<Organization, Long> {

    @Override
    public Organization create(Organization org) throws DatabaseException {
        String sql = "INSERT INTO organizations (user_id, organization_name, description, contact_information) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, org.getId()); // user_id
            ps.setString(2, org.getOrganizationName());
            ps.setString(3, org.getDescription());
            ps.setString(4, org.getContactInformation());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Creating organization profile failed, no rows affected.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    org.setOrganizationId(rs.getLong(1));
                }
            }
            return org;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to persist organization profile: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Organization> findById(Long id) throws DatabaseException {
        String sql = """
            SELECT o.id AS org_id, o.user_id, o.organization_name, o.description AS org_desc, o.contact_information,
                   u.full_name, u.email, u.password_hash, u.account_status, u.created_at, u.updated_at
            FROM organizations o
            JOIN users u ON o.user_id = u.id
            WHERE o.id = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToOrganization(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding organization by ID " + id + ": " + e.getMessage(), e);
        }
    }

    public Optional<Organization> findByUserId(Long userId) throws DatabaseException {
        String sql = """
            SELECT o.id AS org_id, o.user_id, o.organization_name, o.description AS org_desc, o.contact_information,
                   u.full_name, u.email, u.password_hash, u.account_status, u.created_at, u.updated_at
            FROM organizations o
            JOIN users u ON o.user_id = u.id
            WHERE o.user_id = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToOrganization(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding organization for user ID " + userId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Organization> findAll() throws DatabaseException {
        String sql = """
            SELECT o.id AS org_id, o.user_id, o.organization_name, o.description AS org_desc, o.contact_information,
                   u.full_name, u.email, u.password_hash, u.account_status, u.created_at, u.updated_at
            FROM organizations o
            JOIN users u ON o.user_id = u.id
            ORDER BY o.organization_name ASC
        """;

        List<Organization> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToOrganization(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed retrieving organizations: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Organization org) throws DatabaseException {
        String sql = "UPDATE organizations SET organization_name = ?, description = ?, contact_information = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, org.getOrganizationName());
            ps.setString(2, org.getDescription());
            ps.setString(3, org.getContactInformation());
            ps.setLong(4, org.getOrganizationId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed updating organization: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) throws DatabaseException {
        String sql = "DELETE FROM organizations WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed deleting organization: " + e.getMessage(), e);
        }
    }

    private Organization mapResultSetToOrganization(ResultSet rs) throws SQLException {
        Organization org = new Organization();
        org.setId(rs.getLong("user_id"));
        org.setOrganizationId(rs.getLong("org_id"));
        org.setFullName(rs.getString("full_name"));
        org.setEmail(rs.getString("email"));
        org.setPasswordHash(rs.getString("password_hash"));
        org.setAccountStatus(com.volunteer.model.enums.AccountStatus.fromString(rs.getString("account_status")));
        org.setOrganizationName(rs.getString("organization_name"));
        org.setDescription(rs.getString("org_desc"));
        org.setContactInformation(rs.getString("contact_information"));

        Timestamp c = rs.getTimestamp("created_at");
        if (c != null) org.setCreatedAt(c.toLocalDateTime());
        Timestamp u = rs.getTimestamp("updated_at");
        if (u != null) org.setUpdatedAt(u.toLocalDateTime());

        return org;
    }
}
