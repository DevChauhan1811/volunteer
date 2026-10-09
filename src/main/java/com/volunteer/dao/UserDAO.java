package com.volunteer.dao;

import com.volunteer.exception.DatabaseException;
import com.volunteer.model.Admin;
import com.volunteer.model.Organization;
import com.volunteer.model.User;
import com.volunteer.model.Volunteer;
import com.volunteer.model.enums.AccountStatus;
import com.volunteer.model.enums.Role;
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
 * Data Access Object for User entities.
 * Demonstrates PreparedStatement parameterization, SQL injection prevention,
 * and polymorphic instantiation of Admin, Organization, and Volunteer domain objects.
 */
public class UserDAO implements BaseDAO<User, Long> {

    @Override
    public User create(User user) throws DatabaseException {
        String sql = "INSERT INTO users (full_name, email, password_hash, role, account_status) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            ps.setString(5, user.getAccountStatus().name());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                } else {
                    throw new DatabaseException("Creating user failed, no generated ID obtained.");
                }
            }
            return user;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to persist user: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(Long id) throws DatabaseException {
        String sql = """
            SELECT u.id, u.full_name, u.email, u.password_hash, u.role, u.account_status, u.created_at, u.updated_at,
                   o.id AS org_id, o.organization_name, o.description AS org_desc, o.contact_information
            FROM users u
            LEFT JOIN organizations o ON u.id = o.user_id
            WHERE u.id = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find user by ID " + id + ": " + e.getMessage(), e);
        }
    }

    public Optional<User> findByEmail(String email) throws DatabaseException {
        if (email == null) return Optional.empty();

        String sql = """
            SELECT u.id, u.full_name, u.email, u.password_hash, u.role, u.account_status, u.created_at, u.updated_at,
                   o.id AS org_id, o.organization_name, o.description AS org_desc, o.contact_information
            FROM users u
            LEFT JOIN organizations o ON u.id = o.user_id
            WHERE LOWER(u.email) = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find user by email: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> findAll() throws DatabaseException {
        String sql = """
            SELECT u.id, u.full_name, u.email, u.password_hash, u.role, u.account_status, u.created_at, u.updated_at,
                   o.id AS org_id, o.organization_name, o.description AS org_desc, o.contact_information
            FROM users u
            LEFT JOIN organizations o ON u.id = o.user_id
            ORDER BY u.id ASC
        """;

        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve all users: " + e.getMessage(), e);
        }
    }

    public List<User> search(String keyword, Role roleFilter, AccountStatus statusFilter) throws DatabaseException {
        StringBuilder sql = new StringBuilder("""
            SELECT u.id, u.full_name, u.email, u.password_hash, u.role, u.account_status, u.created_at, u.updated_at,
                   o.id AS org_id, o.organization_name, o.description AS org_desc, o.contact_information
            FROM users u
            LEFT JOIN organizations o ON u.id = o.user_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (LOWER(u.full_name) LIKE ? OR LOWER(u.email) LIKE ? OR LOWER(o.organization_name) LIKE ?)");
            String term = "%" + keyword.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (roleFilter != null) {
            sql.append(" AND u.role = ?");
            params.add(roleFilter.name());
        }

        if (statusFilter != null) {
            sql.append(" AND u.account_status = ?");
            params.add(statusFilter.name());
        }

        sql.append(" ORDER BY u.id ASC");

        List<User> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToUser(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed searching users: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(User user) throws DatabaseException {
        String sql = "UPDATE users SET full_name = ?, email = ?, account_status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, user.getAccountStatus().name());
            ps.setLong(4, user.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed updating user: " + e.getMessage(), e);
        }
    }

    public boolean updateAccountStatus(Long userId, AccountStatus status) throws DatabaseException {
        String sql = "UPDATE users SET account_status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setLong(2, userId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed updating account status for user " + userId + ": " + e.getMessage(), e);
        }
    }

    public int countActiveAdmins() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND account_status = 'ACTIVE'";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count active administrators: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) throws DatabaseException {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed deleting user: " + e.getMessage(), e);
        }
    }

    // ====================================================================
    // Polymorphic ResultSet Mapper
    // ====================================================================
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        Role role = Role.fromString(rs.getString("role"));
        AccountStatus status = AccountStatus.fromString(rs.getString("account_status"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");

        User user;
        switch (role) {
            case ADMIN -> {
                Admin admin = new Admin(id, fullName, email, passwordHash, status);
                user = admin;
            }
            case ORGANIZATION -> {
                long orgId = rs.getLong("org_id");
                String orgName = rs.getString("organization_name");
                String orgDesc = rs.getString("org_desc");
                String contact = rs.getString("contact_information");
                Organization org = new Organization(id, fullName, email, passwordHash, status,
                    rs.wasNull() ? null : orgId, orgName, orgDesc, contact);
                user = org;
            }
            case VOLUNTEER -> {
                Volunteer vol = new Volunteer(id, fullName, email, passwordHash, status);
                user = vol;
            }
            default -> throw new IllegalArgumentException("Unknown role: " + role);
        }

        if (createdAt != null) user.setCreatedAt(createdAt.toLocalDateTime());
        if (updatedAt != null) user.setUpdatedAt(updatedAt.toLocalDateTime());

        return user;
    }
}
