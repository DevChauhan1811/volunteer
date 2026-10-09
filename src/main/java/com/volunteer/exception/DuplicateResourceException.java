package com.volunteer.exception;

/**
 * Exception thrown when attempting to create an entity that conflicts with existing unique data
 * (e.g. duplicate email registration, or duplicate active signup for an opportunity).
 */
public class DuplicateResourceException extends PlatformException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
