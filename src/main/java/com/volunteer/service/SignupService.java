package com.volunteer.service;

import com.volunteer.exception.CapacityExceededException;
import com.volunteer.exception.DatabaseException;
import com.volunteer.exception.DuplicateResourceException;
import com.volunteer.exception.UnauthorizedException;
import com.volunteer.exception.ValidationException;
import com.volunteer.model.Signup;
import com.volunteer.model.User;
import com.volunteer.model.enums.RegistrationStatus;

import java.util.List;

/**
 * Service contract for volunteer registration, capacity locking, and attendance management.
 */
public interface SignupService {

    /**
     * Atomically registers a volunteer for an opportunity with capacity and duplicate validation.
     */
    Signup registerForOpportunity(User initiator, Long opportunityId)
        throws UnauthorizedException, ValidationException, DuplicateResourceException,
               CapacityExceededException, DatabaseException;

    /**
     * Cancels an existing active registration owned by the volunteer.
     */
    void cancelRegistration(User initiator, Long signupId)
        throws UnauthorizedException, ValidationException, DatabaseException;

    /**
     * Retrieves all registrations for a specific volunteer (Volunteer "My Sign-Ups").
     */
    List<Signup> getSignupsByVolunteer(User initiator, Long volunteerId)
        throws UnauthorizedException, DatabaseException;

    /**
     * Retrieves all participants registered for an organization's opportunity.
     */
    List<Signup> getSignupsByOpportunity(User initiator, Long opportunityId)
        throws UnauthorizedException, DatabaseException;

    /**
     * Updates participant attendance status (e.g. ATTENDED, NO_SHOW, COMPLETED).
     */
    void updateAttendanceStatus(User initiator, Long signupId, RegistrationStatus status)
        throws UnauthorizedException, ValidationException, DatabaseException;
}
