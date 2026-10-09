package com.volunteer.exception;

/**
 * Exception thrown when a database or JDBC operation fails.
 * Protects technical database credentials and raw SQL errors from being exposed to end users.
 */
public class DatabaseException extends PlatformException {

    private final String sqlState;
    private final int errorCode;

    public DatabaseException(String message) {
        super(message);
        this.sqlState = null;
        this.errorCode = 0;
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
        if (cause instanceof java.sql.SQLException sqlEx) {
            this.sqlState = sqlEx.getSQLState();
            this.errorCode = sqlEx.getErrorCode();
        } else {
            this.sqlState = null;
            this.errorCode = 0;
        }
    }

    public String getSqlState() {
        return sqlState;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
