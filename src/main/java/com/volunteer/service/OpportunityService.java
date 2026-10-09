package com.volunteer.service;

import com.volunteer.exception.DatabaseException;
import com.volunteer.exception.UnauthorizedException;
import com.volunteer.exception.ValidationException;
import com.volunteer.model.Opportunity;
import com.volunteer.model.User;
import com.volunteer.model.enums.ApprovalStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service contract for opportunity creation, moderation, editing, and volunteer searching.
 */
public interface OpportunityService {

    /**
     * Creates a new volunteer opportunity. Initially set to PENDING approval status.
     */
    Opportunity postOpportunity(User initiator, Opportunity opportunity)
        throws UnauthorizedException, ValidationException, DatabaseException;

    /**
     * Updates an opportunity. If major details change on an approved listing, reverts to PENDING.
     */
    void updateOpportunity(User initiator, Opportunity opportunity)
        throws UnauthorizedException, ValidationException, DatabaseException;

    /**
     * Finds an opportunity by ID.
     */
    Optional<Opportunity> getOpportunityById(Long id) throws DatabaseException;

    /**
     * Retrieves all approved opportunities matching optional filters (Volunteer view).
     */
    List<Opportunity> getAvailableOpportunities(String keyword, String category, String location, LocalDate date)
        throws DatabaseException;

    /**
     * Retrieves all opportunities owned by an organization.
     */
    List<Opportunity> getOpportunitiesByOrganization(User initiator, Long orgId)
        throws UnauthorizedException, DatabaseException;

    /**
     * Retrieves all opportunities across the system (Admin moderation view).
     */
    List<Opportunity> getAllOpportunitiesForAdmin(User initiator, ApprovalStatus statusFilter)
        throws UnauthorizedException, DatabaseException;

    /**
     * Approves or rejects an opportunity (Admin feature).
     */
    void moderateOpportunity(User initiator, Long opportunityId, ApprovalStatus status, String rejectionReason)
        throws UnauthorizedException, ValidationException, DatabaseException;

    /**
     * Cancels or closes an opportunity by the organization that owns it.
     */
    void cancelOpportunity(User initiator, Long opportunityId)
        throws UnauthorizedException, DatabaseException;
}
