package com.volunteer.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility to initialize the MySQL database schema and default seed data.
 * Can be run independently via Maven or called programmatically during setup.
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    /**
     * Executes the DDL commands in sql/schema.sql using a raw server connection.
     */
    public static void initializeSchema() throws SQLException {
        System.out.println("[DB-INIT] Executing schema.sql to initialize database and tables...");
        Path schemaPath = Paths.get("sql", "schema.sql");
        if (!Files.exists(schemaPath)) {
            throw new IllegalStateException("schema.sql not found at: " + schemaPath.toAbsolutePath());
        }

        try (Connection serverConn = DatabaseConnection.getServerConnection();
             Statement stmt = serverConn.createStatement()) {

            String content = Files.readString(schemaPath, StandardCharsets.UTF_8);
            String[] statements = content.split(";");

            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
            System.out.println("[DB-INIT] Database and tables created successfully!");
        } catch (Exception e) {
            System.err.println("[DB-INIT] Schema initialization failed: " + e.getMessage());
            throw new SQLException(e);
        }
    }

    /**
     * Seeds the database with default admin, organizations, opportunities, and settings
     * if the tables are currently empty.
     */
    public static void seedInitialData() throws SQLException {
        System.out.println("[DB-INIT] Checking if initial seed data is required...");

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if users already exist
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("[DB-INIT] Data already exists in database. Skipping seed.");
                    return;
                }
            }

            System.out.println("[DB-INIT] Seeding initial users, organizations, opportunities, and settings...");
            conn.setAutoCommit(false);

            try {
                String defaultPasswordHash = PasswordHasher.hash("Password@123");

                // 1. Insert Admin
                long adminId = insertUser(conn, "System Administrator", "admin@platform.com", defaultPasswordHash, "ADMIN", "ACTIVE");

                // 2. Insert Organizations
                long orgUser1 = insertUser(conn, "Green Earth Alliance", "contact@greenearth.org", defaultPasswordHash, "ORGANIZATION", "ACTIVE");
                long orgId1 = insertOrganization(conn, orgUser1, "Green Earth Alliance", "Environmental non-profit focused on reforestation and urban greenery.", "contact@greenearth.org | +1-555-0101");

                long orgUser2 = insertUser(conn, "Metro Community Food Bank", "info@metrofoodbank.org", defaultPasswordHash, "ORGANIZATION", "ACTIVE");
                long orgId2 = insertOrganization(conn, orgUser2, "Metro Community Food Bank", "Serving healthy meals and food supplies to underserved local communities.", "info@metrofoodbank.org | +1-555-0202");

                // 3. Insert Volunteers
                long vol1 = insertUser(conn, "Alice Johnson", "alice.johnson@example.com", defaultPasswordHash, "VOLUNTEER", "ACTIVE");
                long vol2 = insertUser(conn, "Bob Smith", "bob.smith@example.com", defaultPasswordHash, "VOLUNTEER", "ACTIVE");
                long vol3 = insertUser(conn, "Clara Davis", "clara.davis@example.com", defaultPasswordHash, "VOLUNTEER", "ACTIVE");

                // 4. Insert Opportunities
                long opp1 = insertOpportunity(conn, orgId1, "Community Tree Planting Drive",
                    "Join us to plant over 300 native saplings in Riverside Park to combat local urban heat.",
                    "Environment", "2026-10-25", "09:00:00", "13:00:00", "Riverside Park, Sector 4", 25, "APPROVED", null);

                long opp2 = insertOpportunity(conn, orgId2, "Weekend Grocery Packaging & Distribution",
                    "Sort, pack, and distribute weekly emergency food kits to elderly residents and families.",
                    "Community Relief", "2026-10-26", "10:00:00", "14:00:00", "Metro Warehouse, 42 Elm Street", 15, "APPROVED", null);

                long opp3 = insertOpportunity(conn, orgId1, "Coastal Beach & Estuary Cleanup",
                    "Collection and sorting of marine plastic waste along North Shore coastline.",
                    "Conservation", "2026-11-05", "08:30:00", "12:00:00", "North Shore Beach Pavilion", 30, "PENDING", null);

                long opp4 = insertOpportunity(conn, orgId2, "Winter Warmth Clothing Drive",
                    "Inspect and sort donated winter coats, gloves, and blankets for shelters.",
                    "Humanitarian", "2026-11-12", "11:00:00", "15:00:00", "Downtown Youth Center", 20, "APPROVED", null);

                // 5. Insert Sample Signups
                insertSignup(conn, vol1, opp1, "REGISTERED");
                insertSignup(conn, vol2, opp1, "REGISTERED");
                insertSignup(conn, vol1, opp2, "COMPLETED");
                insertSignup(conn, vol3, opp2, "REGISTERED");

                // 6. Insert Sample Volunteer Hours
                insertHours(conn, vol1, opp2, 4.0, "2026-10-26", "Packed 45 emergency boxes and assisted at the loading bay.", "APPROVED", orgId2);
                insertHours(conn, vol2, opp1, 3.5, "2026-10-25", "Helped prepare ground and planted 15 saplings.", "PENDING", null);

                // 7. Insert Sample Messages
                insertMessage(conn, vol1, orgUser2, opp2, "Hello! Will parking be available near the loading bay?", "2026-10-24 14:30:00");
                insertMessage(conn, orgUser2, vol1, opp2, "Hi Alice, yes! Designated volunteer parking is available in Lot B.", "2026-10-24 15:10:00");

                // 8. Insert System Settings
                insertSetting(conn, "platform_name", "Online Volunteer Management Platform", adminId);
                insertSetting(conn, "max_volunteer_capacity", "100", adminId);
                insertSetting(conn, "org_registration_approval_required", "false", adminId);
                insertSetting(conn, "allow_hour_self_submission", "true", adminId);

                conn.commit();
                System.out.println("[DB-INIT] Initial seed data successfully committed!");
            } catch (Exception ex) {
                conn.rollback();
                System.err.println("[DB-INIT] Error during seed insertion. Transaction rolled back: " + ex.getMessage());
                throw new SQLException(ex);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private static long insertUser(Connection conn, String name, String email, String hash, String role, String status) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, password_hash, role, account_status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, hash);
            ps.setString(4, role);
            ps.setString(5, status);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1;
    }

    private static long insertOrganization(Connection conn, long userId, String name, String desc, String contact) throws SQLException {
        String sql = "INSERT INTO organizations (user_id, organization_name, description, contact_information) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, name);
            ps.setString(3, desc);
            ps.setString(4, contact);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1;
    }

    private static long insertOpportunity(Connection conn, long orgId, String title, String desc, String cat,
                                          String date, String start, String end, String loc, int cap, String status, String reason) throws SQLException {
        String sql = "INSERT INTO opportunities (organization_id, title, description, category, event_date, start_time, end_time, location, capacity, approval_status, rejection_reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, orgId);
            ps.setString(2, title);
            ps.setString(3, desc);
            ps.setString(4, cat);
            ps.setDate(5, java.sql.Date.valueOf(date));
            ps.setTime(6, java.sql.Time.valueOf(start));
            ps.setTime(7, java.sql.Time.valueOf(end));
            ps.setString(8, loc);
            ps.setInt(9, cap);
            ps.setString(10, status);
            ps.setString(11, reason);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1;
    }

    private static void insertSignup(Connection conn, long volId, long oppId, String status) throws SQLException {
        String sql = "INSERT INTO signups (volunteer_id, opportunity_id, registration_status) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, volId);
            ps.setLong(2, oppId);
            ps.setString(3, status);
            ps.executeUpdate();
        }
    }

    private static void insertHours(Connection conn, long volId, long oppId, double hours, String date, String desc, String status, Long verifiedBy) throws SQLException {
        String sql = "INSERT INTO volunteer_hours (volunteer_id, opportunity_id, hours_worked, work_date, description, verification_status, verified_by) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, volId);
            ps.setLong(2, oppId);
            ps.setDouble(3, hours);
            ps.setDate(4, java.sql.Date.valueOf(date));
            ps.setString(5, desc);
            ps.setString(6, status);
            if (verifiedBy != null) {
                ps.setLong(7, verifiedBy);
            } else {
                ps.setNull(7, java.sql.Types.BIGINT);
            }
            ps.executeUpdate();
        }
    }

    private static void insertMessage(Connection conn, long senderId, long recipientId, Long oppId, String text, String sentAt) throws SQLException {
        String sql = "INSERT INTO messages (sender_id, recipient_id, opportunity_id, message_text, sent_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, senderId);
            ps.setLong(2, recipientId);
            if (oppId != null) {
                ps.setLong(3, oppId);
            } else {
                ps.setNull(3, java.sql.Types.BIGINT);
            }
            ps.setString(4, text);
            ps.setTimestamp(5, java.sql.Timestamp.valueOf(sentAt));
            ps.executeUpdate();
        }
    }

    private static void insertSetting(Connection conn, String key, String value, long updatedBy) throws SQLException {
        String sql = "INSERT INTO system_settings (setting_key, setting_value, updated_by) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setLong(3, updatedBy);
            ps.executeUpdate();
        }
    }

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  ONLINE VOLUNTEER PLATFORM - DATABASE INITIALIZER");
        System.out.println("==========================================================");
        try {
            initializeSchema();
            seedInitialData();
            System.out.println("==========================================================");
            System.out.println("  Database Initialization Completed Successfully!");
            System.out.println("==========================================================");
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
            } catch (Throwable ignored) {
            }
        }
    }
}
