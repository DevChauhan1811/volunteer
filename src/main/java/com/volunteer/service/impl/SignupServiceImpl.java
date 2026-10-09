package com.volunteer.service.impl;

import com.volunteer.dao.OpportunityDAO;
import com.volunteer.dao.OrganizationDAO;
import com.volunteer.dao.SignupDAO;
import com.volunteer.exception.CapacityExceededException;
import com.volunteer.exception.DatabaseException;
import com.volunteer.exception.DuplicateResourceException;
import com.volunteer.exception.UnauthorizedException;
import com.volunteer.exception.ValidationException;
import com.volunteer.model.Opportunity;
import com.volunteer.model.Organization;
import com.volunteer.model.Signup;
import com.volunteer.model.User;
import com.volunteer.model.enums.RegistrationStatus;
import com.volunteer.service.SignupService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service implementation for volunteer registrations, atomic concurrency checks,
 * cancellation rules, and attendance marking.
 */
public class SignupServiceImpl implements SignupService {

    private final SignupDAO signupDAO;
    private final OpportunityDAO opportunityDAO;
    private final OrganizationDAO organizationDAO;

    public SignupServiceImpl() {
        this.signupDAO = new SignupDAO();
        this.opportunityDAO = new OpportunityDAO();
        this.organizationDAO = new OrganizationDAO();
    }

    public SignupServiceImpl(SignupDAO signupDAO, OpportunityDAO opportunityDAO, OrganizationDAO organizationDAO) {
        this.signupDAO = signupDAO;
        this.opportunityDAO = opportunityDAO;
        this.organizationDAO = organizationDAO;
    }

    @Override
    public Signup registerForOpportunity(User initiator, Long opportunityId)
            throws UnauthorizedException, ValidationException, DuplicateResourceException,
                   CapacityExceededException, DatabaseException {

        if (initiator == null) {
            throw new UnauthorizedException("Authentication required to sign up for opportunities.");
        }

        if (!initiator.isVolunteer()) {
            throw new UnauthorizedException("Only volunteer accounts can register for opportunities.");
        }

        if (!initiator.isActive()) {
            throw new UnauthorizedException("Your account is not active. Please contact support.");
        }

        if (opportunityId == null) {
            throw new ValidationException("opportunityId", "Opportunity ID is required.");
        }

        // Delegate to atomic, pessimistic-locking DAO transaction
        return signupDAO.registerWithCapacityLock(initiator.getId(), opportunityId);
    }

    @Override
    public void cancelRegistration(User initiator, Long signupId)
            throws UnauthorizedException, ValidationException, DatabaseException {

        if (initiator == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        Optional<Signup> signupOpt = signupDAO.findById(signupId);
        if (signupOpt.isEmpty()) {
            throw new ValidationException("signup", "Registration record #" + signupId + " not found.");
        }

        Signup signup = signupOpt.get();

        // Enforce ownership: Volunteer can only cancel their own signups
        boolean isOwner = initiator.getId() != null && initiator.getId().equals(signup.getVolunteerId());
        if (!initiator.isAdmin() && !isOwner) {
            throw new UnauthorizedException("You cannot cancel another volunteer's registration.");
        }

        if (signup.getRegistrationStatus() != RegistrationStatus.REGISTERED) {
            throw new ValidationException("status", "Cannot cancel registration with status: " + signup.getRegistrationStatus());
        }

        if (signup.getEventDate() != null && signup.getEventDate().isBefore(LocalDate.now())) {
            throw new ValidationException("date", "Cannot cancel registration for an event that has already occurred.");
        }

        signupDAO.updateStatus(signupId, RegistrationStatus.CANCELLED);
    }

    @Override
    public List<Signup> getSignupsByVolunteer(User initiator, Long volunteerId)
            throws UnauthorizedException, DatabaseException {

        if (initiator == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        boolean isSelf = initiator.getId() != null && initiator.getId().equals(volunteerId);
        if (!initiator.isAdmin() && !isSelf) {
            throw new UnauthorizedException("You are not authorized to view another volunteer's registrations.");
        }

        return signupDAO.findByVolunteer(volunteerId);
    }

    @Override
    public List<Signup> getSignupsByOpportunity(User initiator, Long opportunityId)
            throws UnauthorizedException, DatabaseException {

        if (initiator == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        Optional<Opportunity> oppOpt = opportunityDAO.findById(opportunityId);
        if (oppOpt.isEmpty()) {
            return List.of();
        }

        Opportunity opp = oppOpt.get();

        // Check if initiator is the owning organization or Admin
        boolean isOwner = false;
        if (initiator instanceof Organization org && org.getOrganizationId() != null) {
            isOwner = org.getOrganizationId().equals(opp.getOrganizationId());
        } else if (initiator.isOrganization()) {
            Optional<Organization> orgOpt = organizationDAO.findByUserId(initiator.getId());
            isOwner = orgOpt.isPresent() && orgOpt.get().getOrganizationId().equals(opp.getOrganizationId());
        }

        if (!initiator.isAdmin() && !isOwner) {
            throw new UnauthorizedException("You cannot view participants for another organization's listing.");
        }

        return signupDAO.findByOpportunity(opportunityId);
    }

    @Override
    public void updateAttendanceStatus(User initiator, Long signupId, RegistrationStatus status)
            throws UnauthorizedException, ValidationException, DatabaseException {

        if (initiator == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        Optional<Signup> signupOpt = signupDAO.findById(signupId);
        if (signupOpt.isEmpty()) {
            throw new ValidationException("signup", "Registration record not found.");
        }
        Signup signup = signupOpt.get();

        Optional<Opportunity> oppOpt = opportunityDAO.findById(signup.getOpportunityId());
        if (oppOpt.isEmpty()) {
            throw new ValidationException("opportunity", "Associated opportunity not found.");
        }

        boolean isOwner = false;
        if (initiator instanceof Organization org && org.getOrganizationId() != null) {
            isOwner = org.getOrganizationId().equals(oppOpt.get().getOrganizationId());
        } else if (initiator.isOrganization()) {
            Optional<Organization> orgOpt = organizationDAO.findByUserId(initiator.getId());
            isOwner = orgOpt.isPresent() && orgOpt.get().getOrganizationId().equals(oppOpt.get().getOrganizationId());
        }

        if (!initiator.isAdmin() && !isOwner) {
            throw new UnauthorizedException("Only the hosting organization can mark volunteer attendance.");
        }

        signupDAO.updateStatus(signupId, status);
    }
}
