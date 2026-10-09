package com.volunteer.exception;

/**
 * Exception thrown when user input fails business validation rules
 * (e.g., malformed email, past dates, invalid time ranges, negative capacity).
 */
public class ValidationException extends PlatformException {

    private final String fieldName;

    public ValidationException(String message) {
        super(message);
        this.fieldName = null;
    }

    public ValidationException(String fieldName, String message) {
        super(message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
