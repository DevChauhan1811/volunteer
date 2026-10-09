package com.volunteer.dao;

import com.volunteer.exception.CapacityExceededException;
import com.volunteer.exception.DatabaseException;
import com.volunteer.exception.DuplicateResourceException;
import com.volunteer.model.Signup;
import com.volunteer.model.enums.RegistrationStatus;
import com.volunteer.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for volunteer Signups.
 * Implements atomic transactional registration with pessimistic locking (SELECT FOR UPDATE)
 * to prevent overbooking and race conditions under concurrent requests.
 */
public class SignupDAO implements BaseDAO<Signup, Long> {

    private static final String BASE_SELECT = """
        SELECT s.id, s.volunteer_id, u.full_name AS volunteer_name, u.email AS volunteer_email,
               s.opportunity_id, o.title AS opportunity_title, org.organization_name,
               o.event_date, o.location, s.registration_status, s.registered_at, s.updated_at
        FROM signups s
        JOIN users u ON s.volunteer_id = u.id
        JOIN opportunities o ON s.opportunity_id = o.id
        JOIN organizations org ON o.organization_id = org.id
    """;

    @Override
    public Signup create(Signup entity) throws DatabaseException {
        String sql = "INSERT INTO signups (volunteer_id, opportunity_id, registration_status) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, entity.getVolunteerId());
            ps.setLong(2, entity.getOpportunityId());
            ps.setString(3, entity.getRegistrationStatus().name());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Failed inserting signup record.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    entity.setId(rs.getLong(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            throw new DatabaseException("Failed creating signup: " + e.getMessage(), e);
        }
    }

    /**
     * Atomically executes a registration check and insert using a database transaction
     * and pessimistic row locking (SELECT ... FOR UPDATE).
     * Prevents race conditions and capacity overbooking under concurrent access.
     */
    public Signup registerWithCapacityLock(Long volunteerId, Long opportunityId)
            throws DatabaseException, DuplicateResourceException, CapacityExceededException {

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Begin ACID Transaction

            try {
                // 1. Lock the opportunity row and fetch current capacity & approval status
                String lockOppSql = "SELECT capacity, approval_status FROM opportunities WHERE id = ? FOR UPDATE";
                int capacity;
                String approvalStatus;
                try (PreparedStatement ps = conn.prepareStatement(lockOppSql)) {
                    ps.setLong(1, opportunityId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new DatabaseException("Opportunity #" + opportunityId + " does not exist.");
                        }
                        capacity = rs.getInt("capacity");
                        approvalStatus = rs.getString("approval_status");
                    }
                }

                if (!"APPROVED".equalsIgnoreCase(approvalStatus)) {
                    throw new DatabaseException("Cannot register: Opportunity is not approved for participation.");
                }

                // 2. Lock and check if volunteer has an existing active registration
                String checkExistingSql = "SELECT id, registration_status FROM signups WHERE volunteer_id = ? AND opportunity_id = ? FOR UPDATE";
                Long existingSignupId = null;
                String existingStatus = null;
                try (PreparedStatement ps = conn.prepareStatement(checkExistingSql)) {
                    ps.setLong(1, volunteerId);
                    ps.setLong(2, opportunityId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            existingSignupId = rs.getLong("id");
                            existingStatus = rs.getString("registration_status");
                        }
                    }
                }

                if (existingSignupId != null && "REGISTERED".equalsIgnoreCase(existingStatus)) {
                    throw new DuplicateResourceException("You are already actively registered for this opportunity.");
                }

                // 3. Count currently registered participants under the transaction lock
                String countSql = "SELECT COUNT(*) FROM signups WHERE opportunity_id = ? AND registration_status = 'REGISTERED' FOR UPDATE";
                int currentActiveCount = 0;
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    ps.setLong(1, opportunityId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            currentActiveCount = rs.getInt(1);
                        }
                    }
                }

                if (currentActiveCount >= capacity) {
                    throw new CapacityExceededException(opportunityId, capacity);
                }

                // 4. Insert new signup or reactivate previously cancelled registration
                Signup result = new Signup();
                result.setVolunteerId(volunteerId);
                result.setOpportunityId(opportunityId);
                result.setRegistrationStatus(RegistrationStatus.REGISTERED);

                if (existingSignupId != null) {
                    // Reactivate cancelled registration
                    String reactivateSql = "UPDATE signups SET registration_status = 'REGISTERED', registered_at = CURRENT_TIMESTAMP WHERE id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(reactivateSql)) {
                        ps.setLong(1, existingSignupId);
                        ps.executeUpdate();
                    }
                    result.setId(existingSignupId);
                } else {
                    // Insert new signup row
                    String insertSql = "INSERT INTO signups (volunteer_id, opportunity_id, registration_status) VALUES (?, ?, 'REGISTERED')";
                    try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setLong(1, volunteerId);
                        ps.setLong(2, opportunityId);
                        ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) {
                                result.setId(rs.getLong(1));
                            }
                        }
                    }
                }

                conn.commit(); // Transaction successful
                return result;
            } catch (Exception ex) {
                conn.rollback(); // Transaction rolled back on any failure
                if (ex instanceof DuplicateResourceException dre) throw dre;
                if (ex instanceof CapacityExceededException cee) throw cee;
                if (ex instanceof DatabaseException dbe) throw dbe;
                throw new DatabaseException("Transaction failed while registering: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException sqle) {
            throw new DatabaseException("Database transaction error: " + sqle.getMessage(), sqle);
        }
    }

    @Override
    public Optional<Signup> findById(Long id) throws DatabaseException {
        String sql = BASE_SELECT + " WHERE s.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSignup(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding signup #" + id + ": " + e.getMessage(), e);
        }
    }

    public List<Signup> findByVolunteer(Long volunteerId) throws DatabaseException {
        String sql = BASE_SELECT + " WHERE s.volunteer_id = ? ORDER BY o.event_date DESC, s.registered_at DESC";

        List<Signup> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, volunteerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSignup(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed retrieving signups for volunteer " + volunteerId + ": " + e.getMessage(), e);
        }
    }

    public List<Signup> findByOpportunity(Long opportunityId) throws DatabaseException {
        String sql = BASE_SELECT + " WHERE s.opportunity_id = ? ORDER BY s.registered_at ASC";

        List<Signup> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, opportunityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSignup(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed retrieving roster for opportunity " + opportunityId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Signup> findAll() throws DatabaseException {
        String sql = BASE_SELECT + " ORDER BY s.registered_at DESC";

        List<Signup> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToSignup(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed retrieving signups: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Signup signup) throws DatabaseException {
        return updateStatus(signup.getId(), signup.getRegistrationStatus());
    }

    public boolean updateStatus(Long signupId, RegistrationStatus status) throws DatabaseException {
        String sql = "UPDATE signups SET registration_status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setLong(2, signupId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed updating signup status: " + e.getMessage(), e);
        }
    }

    public int countActiveSignupsForOpportunity(Long opportunityId) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM signups WHERE opportunity_id = ? AND registration_status = 'REGISTERED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, opportunityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed counting active signups: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) throws DatabaseException {
        String sql = "DELETE FROM signups WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed deleting signup: " + e.getMessage(), e);
        }
    }

    private Signup mapResultSetToSignup(ResultSet rs) throws SQLException {
        Signup signup = new Signup();
        signup.setId(rs.getLong("id"));
        signup.setVolunteerId(rs.getLong("volunteer_id"));
        signup.setVolunteerName(rs.getString("volunteer_name"));
        signup.setVolunteerEmail(rs.getString("volunteer_email"));
        signup.setOpportunityId(rs.getLong("opportunity_id"));
        signup.setOpportunityTitle(rs.getString("opportunity_title"));
        signup.setOrganizationName(rs.getString("organization_name"));

        Date d = rs.getDate("event_date");
        if (d != null) signup.setEventDate(d.toLocalDate());

        signup.setLocation(rs.getString("location"));
        signup.setRegistrationStatus(RegistrationStatus.fromString(rs.getString("registration_status")));

        Timestamp reg = rs.getTimestamp("registered_at");
        if (reg != null) signup.setRegisteredAt(reg.toLocalDateTime());

        Timestamp upd = rs.getTimestamp("updated_at");
        if (upd != null) signup.setUpdatedAt(upd.toLocalDateTime());

        return signup;
    }
}
