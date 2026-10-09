package com.volunteer.exception;

/**
 * Exception thrown when an authenticated user attempts to perform an operation
 * not permitted by their role or ownership (privilege escalation prevention).
 */
public class UnauthorizedException extends PlatformException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
