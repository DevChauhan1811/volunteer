package com.volunteer.service.impl;

import com.volunteer.dao.OpportunityDAO;
import com.volunteer.dao.OrganizationDAO;
import com.volunteer.dao.SystemSettingsDAO;
import com.volunteer.exception.DatabaseException;
import com.volunteer.exception.UnauthorizedException;
import com.volunteer.exception.ValidationException;
import com.volunteer.model.Opportunity;
import com.volunteer.model.Organization;
import com.volunteer.model.User;
import com.volunteer.model.enums.ApprovalStatus;
import com.volunteer.service.OpportunityService;
import com.volunteer.util.ValidationUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service implementation for volunteer opportunity posting, updating, and moderation.
 */
public class OpportunityServiceImpl implements OpportunityService {

    private final OpportunityDAO opportunityDAO;
    private final OrganizationDAO organizationDAO;
    private final SystemSettingsDAO systemSettingsDAO;

    public OpportunityServiceImpl() {
        this.opportunityDAO = new OpportunityDAO();
        this.organizationDAO = new OrganizationDAO();
        this.systemSettingsDAO = new SystemSettingsDAO();
    }

    public OpportunityServiceImpl(OpportunityDAO opportunityDAO, OrganizationDAO organizationDAO, SystemSettingsDAO systemSettingsDAO) {
        this.opportunityDAO = opportunityDAO;
        this.organizationDAO = organizationDAO;
        this.systemSettingsDAO = systemSettingsDAO;
    }

    @Override
    public Opportunity postOpportunity(User initiator, Opportunity opp)
            throws UnauthorizedException, ValidationException, DatabaseException {

        if (initiator == null || (!initiator.isOrganization() && !initiator.isAdmin())) {
            throw new UnauthorizedException("Only registered organizations can post volunteer opportunities.");
        }

        // Determine organization ID
        Long orgId = opp.getOrganizationId();
        if (orgId == null) {
            if (initiator instanceof Organization org && org.getOrganizationId() != null) {
                orgId = org.getOrganizationId();
            } else {
                Optional<Organization> orgOpt = organizationDAO.findByUserId(initiator.getId());
                if (orgOpt.isEmpty()) {
                    throw new ValidationException("organizationId", "No organization profile associated with this account.");
                }
                orgId = orgOpt.get().getOrganizationId();
            }
            opp.setOrganizationId(orgId);
        }

        // Validate fields
        ValidationUtil.validateNotEmpty(opp.getTitle(), "title", "Opportunity title");
        ValidationUtil.validateNotEmpty(opp.getDescription(), "description", "Description");
        ValidationUtil.validateNotEmpty(opp.getCategory(), "category", "Category");
        ValidationUtil.validateNotEmpty(opp.getLocation(), "location", "Location");
        ValidationUtil.validateOpportunitySchedule(opp.getEventDate(), opp.getStartTime(), opp.getEndTime());

        int maxAllowedCapacity = 100;
        try {
            Optional<String> capSetting = systemSettingsDAO.getSetting("max_volunteer_capacity");
            if (capSetting.isPresent()) {
                maxAllowedCapacity = Integer.parseInt(capSetting.get());
            }
        } catch (Exception ignored) {
        }
        ValidationUtil.validateCapacity(opp.getCapacity(), maxAllowedCapacity);

        // Security rule: Organizations cannot approve their own listings; always initially PENDING
        opp.setApprovalStatus(ApprovalStatus.PENDING);
        opp.setRejectionReason(null);

        return opportunityDAO.create(opp);
    }

    @Override
    public void updateOpportunity(User initiator, Opportunity opp)
            throws UnauthorizedException, ValidationException, DatabaseException {

        if (initiator == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        Optional<Opportunity> existingOpt = opportunityDAO.findById(opp.getId());
        if (existingOpt.isEmpty()) {
            throw new ValidationException("opportunity", "Opportunity #" + opp.getId() + " does not exist.");
        }
        Opportunity existing = existingOpt.get();

        // Enforce ownership: only the owning org or an Admin can edit
        boolean isOwner = false;
        if (initiator instanceof Organization org && org.getOrganizationId() != null) {
            isOwner = org.getOrganizationId().equals(existing.getOrganizationId());
        } else if (initiator.isOrganization()) {
            Optional<Organization> orgOpt = organizationDAO.findByUserId(initiator.getId());
            isOwner = orgOpt.isPresent() && orgOpt.get().getOrganizationId().equals(existing.getOrganizationId());
        }

        if (!initiator.isAdmin() && !isOwner) {
            throw new UnauthorizedException("You do not have permission to modify opportunities posted by another organization.");
        }

        // Validate modified inputs
        ValidationUtil.validateNotEmpty(opp.getTitle(), "title", "Opportunity title");
        ValidationUtil.validateNotEmpty(opp.getDescription(), "description", "Description");
        ValidationUtil.validateNotEmpty(opp.getCategory(), "category", "Category");
        ValidationUtil.validateNotEmpty(opp.getLocation(), "location", "Location");
        ValidationUtil.validateOpportunitySchedule(opp.getEventDate(), opp.getStartTime(), opp.getEndTime());
        ValidationUtil.validateCapacity(opp.getCapacity(), 1000);

        // Rule: If an approved listing undergoes significant schedule or location change, reset to PENDING for moderation
        boolean majorChanges = !existing.getEventDate().equals(opp.getEventDate())
                            || !existing.getStartTime().equals(opp.getStartTime())
                            || !existing.getEndTime().equals(opp.getEndTime())
                            || !existing.getLocation().equalsIgnoreCase(opp.getLocation())
                            || existing.getCapacity() != opp.getCapacity();

        if (existing.getApprovalStatus() == ApprovalStatus.APPROVED && majorChanges && !initiator.isAdmin()) {
            opp.setApprovalStatus(ApprovalStatus.PENDING);
            opp.setRejectionReason("Re-submitted for moderation following schedule or capacity modification.");
        } else {
            opp.setApprovalStatus(existing.getApprovalStatus());
            opp.setRejectionReason(existing.getRejectionReason());
        }

        opportunityDAO.update(opp);
    }

    @Override
    public Optional<Opportunity> getOpportunityById(Long id) throws DatabaseException {
        if (id == null) return Optional.empty();
        return opportunityDAO.findById(id);
    }

    @Override
    public List<Opportunity> getAvailableOpportunities(String keyword, String category, String location, LocalDate date)
            throws DatabaseException {
        return opportunityDAO.findApproved(keyword, category, location, date);
    }

    @Override
    public List<Opportunity> getOpportunitiesByOrganization(User initiator, Long orgId)
            throws UnauthorizedException, DatabaseException {

        if (initiator == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        // Verify that the organization is requesting its own listings, or requester is Admin
        if (!initiator.isAdmin()) {
            Optional<Organization> orgOpt = organizationDAO.findByUserId(initiator.getId());
            if (orgOpt.isEmpty() || !orgOpt.get().getOrganizationId().equals(orgId)) {
                throw new UnauthorizedException("You cannot view internal listings for another organization.");
            }
        }

        return opportunityDAO.findByOrganization(orgId);
    }

    @Override
    public List<Opportunity> getAllOpportunitiesForAdmin(User initiator, ApprovalStatus statusFilter)
            throws UnauthorizedException, DatabaseException {

        if (initiator == null || !initiator.isAdmin()) {
            throw new UnauthorizedException("Only administrators can access the complete moderation list.");
        }
        return opportunityDAO.findByApprovalStatus(statusFilter);
    }

    @Override
    public void moderateOpportunity(User initiator, Long opportunityId, ApprovalStatus status, String rejectionReason)
            throws UnauthorizedException, ValidationException, DatabaseException {

        if (initiator == null || !initiator.isAdmin()) {
            throw new UnauthorizedException("Only system administrators can moderate volunteer opportunities.");
        }
        if (opportunityId == null || status == null) {
            throw new ValidationException("moderation", "Opportunity ID and decision status are required.");
        }
        if (status == ApprovalStatus.REJECTED && (rejectionReason == null || rejectionReason.trim().isEmpty())) {
            throw new ValidationException("rejectionReason", "A rejection reason must be provided when rejecting an opportunity.");
        }

        opportunityDAO.updateApprovalStatus(opportunityId, status, rejectionReason != null ? rejectionReason.trim() : null);
    }

    @Override
    public void cancelOpportunity(User initiator, Long opportunityId)
            throws UnauthorizedException, DatabaseException {

        if (initiator == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        Optional<Opportunity> oppOpt = opportunityDAO.findById(opportunityId);
        if (oppOpt.isEmpty()) return;

        Opportunity opp = oppOpt.get();
        boolean isOwner = false;
        if (initiator instanceof Organization org && org.getOrganizationId() != null) {
            isOwner = org.getOrganizationId().equals(opp.getOrganizationId());
        } else if (initiator.isOrganization()) {
            Optional<Organization> orgOpt = organizationDAO.findByUserId(initiator.getId());
            isOwner = orgOpt.isPresent() && orgOpt.get().getOrganizationId().equals(opp.getOrganizationId());
        }

        if (!initiator.isAdmin() && !isOwner) {
            throw new UnauthorizedException("You cannot cancel an opportunity posted by another organization.");
        }

        opportunityDAO.updateApprovalStatus(opportunityId, ApprovalStatus.REJECTED, "Cancelled by organization.");
    }
}
