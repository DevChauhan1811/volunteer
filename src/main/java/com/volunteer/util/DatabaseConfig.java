package com.volunteer.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class to load and access database configuration properties
 * from 'application.properties' safely without hardcoding credentials in source code.
 */
public final class DatabaseConfig {

    private static final Properties properties = new Properties();
    private static boolean loaded = false;

    private DatabaseConfig() {
        // Prevent instantiation of utility class
    }

    private static synchronized void loadProperties() {
        if (loaded) {
            return;
        }

        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.err.println("[WARN] 'application.properties' not found in classpath. Falling back to default settings.");
            } else {
                properties.load(input);
            }
            loaded = true;
        } catch (IOException ex) {
            System.err.println("[ERROR] Failed to load application.properties: " + ex.getMessage());
        }
    }

    public static String getHost() {
        loadProperties();
        return properties.getProperty("db.host", "localhost");
    }

    public static int getPort() {
        loadProperties();
        try {
            return Integer.parseInt(properties.getProperty("db.port", "3306"));
        } catch (NumberFormatException e) {
            return 3306;
        }
    }

    public static String getDatabaseName() {
        loadProperties();
        return properties.getProperty("db.name", "volunteer_platform_db");
    }

    public static String getUsername() {
        loadProperties();
        return properties.getProperty("db.username", "root");
    }

    public static String getPassword() {
        loadProperties();
        return properties.getProperty("db.password", "root");
    }

    public static String getJdbcUrl() {
        loadProperties();
        String host = getHost();
        int port = getPort();
        String dbName = getDatabaseName();
        String useSSL = properties.getProperty("db.useSSL", "false");
        String allowPublicKey = properties.getProperty("db.allowPublicKeyRetrieval", "true");
        String timezone = properties.getProperty("db.serverTimezone", "UTC");
        String encoding = properties.getProperty("db.characterEncoding", "UTF-8");

        return String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=%s&allowPublicKeyRetrieval=%s&serverTimezone=%s&characterEncoding=%s",
            host, port, dbName, useSSL, allowPublicKey, timezone, encoding
        );
    }

    public static String getServerOnlyJdbcUrl() {
        loadProperties();
        String host = getHost();
        int port = getPort();
        String useSSL = properties.getProperty("db.useSSL", "false");
        String allowPublicKey = properties.getProperty("db.allowPublicKeyRetrieval", "true");
        String timezone = properties.getProperty("db.serverTimezone", "UTC");

        return String.format(
            "jdbc:mysql://%s:%d/?useSSL=%s&allowPublicKeyRetrieval=%s&serverTimezone=%s",
            host, port, useSSL, allowPublicKey, timezone
        );
    }
}
