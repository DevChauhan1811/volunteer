package com.volunteer.exception;

/**
 * Exception thrown when authentication fails due to invalid credentials,
 * deactivated account, or account suspension.
 */
public class AuthenticationException extends PlatformException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
