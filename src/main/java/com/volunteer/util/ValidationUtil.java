package com.volunteer.util;

import com.volunteer.exception.ValidationException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Pattern;

/**
 * Reusable validation utility class enforcing input integrity across the application.
 * Prevents invalid dates, empty fields, malformed emails, and invalid capacity numbers.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    private ValidationUtil() {
        // Utility class
    }

    public static void validateEmail(String email) throws ValidationException {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("email", "Email address is required.");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("email", "Invalid email address format (e.g. user@example.com).");
        }
    }

    public static void validatePassword(String password) throws ValidationException {
        if (password == null || password.length() < 6) {
            throw new ValidationException("password", "Password must be at least 6 characters long.");
        }
    }

    public static void validateNotEmpty(String value, String fieldName, String displayName) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName, displayName + " cannot be empty.");
        }
    }

    public static void validateCapacity(int capacity, int maxAllowed) throws ValidationException {
        if (capacity <= 0) {
            throw new ValidationException("capacity", "Opportunity capacity must be greater than zero.");
        }
        if (capacity > maxAllowed) {
            throw new ValidationException("capacity", "Opportunity capacity cannot exceed system maximum of " + maxAllowed + ".");
        }
    }

    public static void validateOpportunitySchedule(LocalDate date, LocalTime start, LocalTime end) throws ValidationException {
        if (date == null) {
            throw new ValidationException("eventDate", "Event date is required.");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new ValidationException("eventDate", "Event date cannot be in the past.");
        }
        if (start == null) {
            throw new ValidationException("startTime", "Start time is required.");
        }
        if (end == null) {
            throw new ValidationException("endTime", "End time is required.");
        }
        if (!end.isAfter(start)) {
            throw new ValidationException("endTime", "End time must be later than start time.");
        }
    }

    public static void validateVolunteerHours(double hours) throws ValidationException {
        if (hours <= 0.0) {
            throw new ValidationException("hoursWorked", "Hours worked must be greater than 0.");
        }
        if (hours > 24.0) {
            throw new ValidationException("hoursWorked", "Cannot log more than 24 hours in a single day.");
        }
    }
}
