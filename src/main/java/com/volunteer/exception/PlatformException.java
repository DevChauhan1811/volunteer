package com.volunteer.exception;

/**
 * Base custom exception for the Volunteer Management Platform.
 * All domain-specific business exceptions inherit from this class.
 */
public class PlatformException extends Exception {

    public PlatformException(String message) {
        super(message);
    }

    public PlatformException(String message, Throwable cause) {
        super(message, cause);
    }
}
