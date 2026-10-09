package com.volunteer.app;

import com.volunteer.util.DatabaseConfig;
import com.volunteer.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Diagnostic application to test and verify MySQL JDBC connectivity.
 * Run this during Phase 1 to ensure that credentials and database access are correctly established.
 */
public class ConnectionTest {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  ONLINE VOLUNTEER PLATFORM - JDBC CONNECTIVITY TEST");
        System.out.println("==========================================================");
        System.out.println("Testing configuration:");
        System.out.println("  Host:     " + DatabaseConfig.getHost());
        System.out.println("  Port:     " + DatabaseConfig.getPort());
        System.out.println("  Database: " + DatabaseConfig.getDatabaseName());
        System.out.println("  User:     " + DatabaseConfig.getUsername());
        System.out.println("----------------------------------------------------------");

        // 1. Test Server Connectivity (without specifying DB name)
        System.out.print("[1/3] Connecting to MySQL Server... ");
        try (Connection serverConn = DatabaseConnection.getServerConnection()) {
            DatabaseMetaData meta = serverConn.getMetaData();
            System.out.println("SUCCESS!");
            System.out.println("      MySQL Product: " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
            System.out.println("      JDBC Driver:   " + meta.getDriverName() + " " + meta.getDriverVersion());
        } catch (SQLException ex) {
            System.out.println("FAILED!");
            System.err.println("      Error: " + ex.getMessage());
            System.err.println("      ErrorCode: " + ex.getErrorCode() + ", SQLState: " + ex.getSQLState());
            System.err.println("      [TIP] Please check your username and password in src/main/resources/application.properties");
            return;
        }

        // 2. Check if the database exists on the server
        System.out.print("[2/3] Checking if database '" + DatabaseConfig.getDatabaseName() + "' exists... ");
        boolean dbExists = false;
        try (Connection serverConn = DatabaseConnection.getServerConnection();
             Statement stmt = serverConn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW DATABASES LIKE '" + DatabaseConfig.getDatabaseName() + "'")) {
            if (rs.next()) {
                dbExists = true;
                System.out.println("YES, database found.");
            } else {
                System.out.println("NO, database not created yet.");
                System.out.println("      (This will be created in Phase 2 using sql/schema.sql)");
            }
        } catch (SQLException ex) {
            System.out.println("ERROR: " + ex.getMessage());
        }

        // 3. Test Full Connection to the Target Database (if it exists)
        if (dbExists) {
            System.out.print("[3/3] Connecting to database '" + DatabaseConfig.getDatabaseName() + "'... ");
            try (Connection dbConn = DatabaseConnection.getConnection()) {
                System.out.println("SUCCESS! Database is ready for queries.");
            } catch (SQLException ex) {
                System.out.println("FAILED: " + ex.getMessage());
            }
        } else {
            System.out.println("[3/3] Target database test skipped until schema creation in Phase 2.");
        }

        System.out.println("==========================================================");
        System.out.println("  Phase 1 Connectivity Diagnostics Complete.");
        System.out.println("==========================================================");

        // Ensure MySQL driver cleanup thread shuts down cleanly
        try {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Throwable ignored) {
        }
    }
}
