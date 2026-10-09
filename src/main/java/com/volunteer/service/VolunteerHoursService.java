package com.volunteer.service;

import com.volunteer.exception.DatabaseException;
import com.volunteer.exception.UnauthorizedException;
import com.volunteer.exception.ValidationException;
import com.volunteer.model.User;
import com.volunteer.model.VolunteerHours;
import com.volunteer.model.enums.VerificationStatus;

import java.util.List;

/**
 * Service contract for submitting, reviewing, verifying, and aggregating volunteer service hours.
 */
public interface VolunteerHoursService {

    /**
     * Submits an hour record for a completed opportunity.
     */
    VolunteerHours submitHours(User initiator, VolunteerHours record)
        throws UnauthorizedException, ValidationException, DatabaseException;

    /**
     * Verifies (approves or rejects) submitted hours (Organization feature).
     */
    void verifyHours(User initiator, Long recordId, VerificationStatus status)
        throws UnauthorizedException, ValidationException, DatabaseException;

    /**
     * Retrieves all hours logged by a specific volunteer.
     */
    List<VolunteerHours> getHoursByVolunteer(User initiator, Long volunteerId)
        throws UnauthorizedException, DatabaseException;

    /**
     * Retrieves all hours pending verification for an organization's opportunities.
     */
    List<VolunteerHours> getPendingHoursByOrganization(User initiator, Long organizationId)
        throws UnauthorizedException, DatabaseException;

    /**
     * Calculates the total verified hours for a volunteer from database records.
     */
    double getTotalVerifiedHours(Long volunteerId) throws DatabaseException;
}
