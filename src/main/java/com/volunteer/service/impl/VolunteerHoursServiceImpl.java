package com.volunteer.service.impl;

import com.volunteer.dao.OpportunityDAO;
import com.volunteer.dao.OrganizationDAO;
import com.volunteer.dao.SignupDAO;
import com.volunteer.dao.VolunteerHoursDAO;
import com.volunteer.exception.DatabaseException;
import com.volunteer.exception.UnauthorizedException;
import com.volunteer.exception.ValidationException;
import com.volunteer.model.Opportunity;
import com.volunteer.model.Organization;
import com.volunteer.model.Signup;
import com.volunteer.model.User;
import com.volunteer.model.VolunteerHours;
import com.volunteer.model.enums.VerificationStatus;
import com.volunteer.service.VolunteerHoursService;
import com.volunteer.util.ValidationUtil;

import java.util.List;
import java.util.Optional;

/**
 * Service implementation for volunteer hours submission, verification, and hours aggregation.
 */
public class VolunteerHoursServiceImpl implements VolunteerHoursService {

    private final VolunteerHoursDAO hoursDAO;
    private final OpportunityDAO opportunityDAO;
    private final SignupDAO signupDAO;
    private final OrganizationDAO organizationDAO;

    public VolunteerHoursServiceImpl() {
        this.hoursDAO = new VolunteerHoursDAO();
        this.opportunityDAO = new OpportunityDAO();
        this.signupDAO = new SignupDAO();
        this.organizationDAO = new OrganizationDAO();
    }

    public VolunteerHoursServiceImpl(VolunteerHoursDAO hoursDAO, OpportunityDAO opportunityDAO,
                                    SignupDAO signupDAO, OrganizationDAO organizationDAO) {
        this.hoursDAO = hoursDAO;
        this.opportunityDAO = opportunityDAO;
        this.signupDAO = signupDAO;
        this.organizationDAO = organizationDAO;
    }

    @Override
    public VolunteerHours submitHours(User initiator, VolunteerHours record)
            throws UnauthorizedException, ValidationException, DatabaseException {

        if (initiator == null || !initiator.isVolunteer()) {
            throw new UnauthorizedException("Only registered volunteers can submit volunteer hours.");
        }
        if (record == null) {
            throw new ValidationException("record", "Hour record cannot be null.");
        }

        // Enforce that volunteer logs hours for themselves
        record.setVolunteerId(initiator.getId());

        ValidationUtil.validateVolunteerHours(record.getHoursWorked());
        ValidationUtil.validateNotEmpty(record.getDescription(), "description", "Work description");

        if (record.getWorkDate() == null) {
            throw new ValidationException("workDate", "Work date is required.");
        }

        if (record.getOpportunityId() == null) {
            throw new ValidationException("opportunityId", "Associated opportunity is required.");
        }

        Optional<Opportunity> oppOpt = opportunityDAO.findById(record.getOpportunityId());
        if (oppOpt.isEmpty()) {
            throw new ValidationException("opportunityId", "Opportunity #" + record.getOpportunityId() + " does not exist.");
        }

        // Verify volunteer is actually associated with this opportunity
        List<Signup> signups = signupDAO.findByVolunteer(initiator.getId());
        boolean registeredForOpp = signups.stream().anyMatch(s -> s.getOpportunityId().equals(record.getOpportunityId()));
        if (!registeredForOpp) {
            throw new ValidationException("opportunityId", "You can only submit hours for opportunities you have registered for.");
        }

        record.setVerificationStatus(VerificationStatus.PENDING);
        record.setVerifiedBy(null);

        return hoursDAO.create(record);
    }

    @Override
    public void verifyHours(User initiator, Long recordId, VerificationStatus status)
            throws UnauthorizedException, ValidationException, DatabaseException {

        if (initiator == null) {
            throw new UnauthorizedException("Authentication required.");
        }
        if (recordId == null || status == null) {
            throw new ValidationException("recordId", "Record ID and verification status are required.");
        }

        Optional<VolunteerHours> hourOpt = hoursDAO.findById(recordId);
        if (hourOpt.isEmpty()) {
            throw new ValidationException("recordId", "Hour record #" + recordId + " not found.");
        }
        VolunteerHours record = hourOpt.get();

        Optional<Opportunity> oppOpt = opportunityDAO.findById(record.getOpportunityId());
        if (oppOpt.isEmpty()) {
            throw new ValidationException("opportunity", "Associated opportunity not found.");
        }
        Opportunity opp = oppOpt.get();

        // Enforce: Volunteers cannot verify their own hours!
        if (initiator.isVolunteer()) {
            throw new UnauthorizedException("Volunteers cannot verify volunteer hours.");
        }

        // Determine if initiator is the hosting organization
        Long verifierOrgId = null;
        if (initiator instanceof Organization org && org.getOrganizationId() != null) {
            verifierOrgId = org.getOrganizationId();
        } else if (initiator.isOrganization()) {
            Optional<Organization> orgOpt = organizationDAO.findByUserId(initiator.getId());
            if (orgOpt.isPresent()) {
                verifierOrgId = orgOpt.get().getOrganizationId();
            }
        }

        if (!initiator.isAdmin() && (verifierOrgId == null || !verifierOrgId.equals(opp.getOrganizationId()))) {
            throw new UnauthorizedException("You can only verify hours for opportunities hosted by your organization.");
        }

        hoursDAO.updateVerificationStatus(recordId, status, verifierOrgId);
    }

    @Override
    public List<VolunteerHours> getHoursByVolunteer(User initiator, Long volunteerId)
            throws UnauthorizedException, DatabaseException {

        if (initiator == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        boolean isSelf = initiator.getId() != null && initiator.getId().equals(volunteerId);
        if (!initiator.isAdmin() && !isSelf) {
            throw new UnauthorizedException("You are not authorized to view another volunteer's logged hours.");
        }

        return hoursDAO.findByVolunteer(volunteerId);
    }

    @Override
    public List<VolunteerHours> getPendingHoursByOrganization(User initiator, Long organizationId)
            throws UnauthorizedException, DatabaseException {

        if (initiator == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        if (!initiator.isAdmin()) {
            Optional<Organization> orgOpt = organizationDAO.findByUserId(initiator.getId());
            if (orgOpt.isEmpty() || !orgOpt.get().getOrganizationId().equals(organizationId)) {
                throw new UnauthorizedException("You can only access hours submitted to your own organization.");
            }
        }

        return hoursDAO.findPendingByOrganization(organizationId);
    }

    @Override
    public double getTotalVerifiedHours(Long volunteerId) throws DatabaseException {
        if (volunteerId == null) return 0.0;
        return hoursDAO.getTotalVerifiedHoursForVolunteer(volunteerId);
    }
}
