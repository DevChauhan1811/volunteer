package com.volunteer.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Thread-safe Database Connection Utility implementing safe JDBC management.
 * Provides connections to MySQL and methods for connection validation.
 */
public final class DatabaseConnection {

    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    static {
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found in classpath. Ensure mysql-connector-j is in pom.xml.", e);
        }
    }

    private DatabaseConnection() {
        // Utility class: private constructor prevents instantiation
    }

    /**
     * Obtains a new JDBC connection to the configured database.
     * Callers must close the connection using try-with-resources.
     *
     * @return an open {@link Connection}
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            DatabaseConfig.getJdbcUrl(),
            DatabaseConfig.getUsername(),
            DatabaseConfig.getPassword()
        );
    }

    /**
     * Obtains a connection to the MySQL server directly without specifying a database.
     * Useful for database creation / health checking scripts.
     *
     * @return an open {@link Connection}
     * @throws SQLException if a database access error occurs
     */
    public static Connection getServerConnection() throws SQLException {
        return DriverManager.getConnection(
            DatabaseConfig.getServerOnlyJdbcUrl(),
            DatabaseConfig.getUsername(),
            DatabaseConfig.getPassword()
        );
    }

    /**
     * Tests if the database connection can be established successfully.
     *
     * @return true if connection succeeded and is valid; false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed() && conn.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }
}
