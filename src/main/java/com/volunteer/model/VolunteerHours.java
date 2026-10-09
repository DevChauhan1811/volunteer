package com.volunteer.model;

import com.volunteer.model.enums.VerificationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Domain entity representing logged volunteer service hours.
 */
public class VolunteerHours {

    private Long id;
    private Long volunteerId;
    private String volunteerName;
    private Long opportunityId;
    private String opportunityTitle;
    private double hoursWorked;
    private LocalDate workDate;
    private String description;
    private VerificationStatus verificationStatus;
    private Long verifiedBy; // Organization ID that verified
    private String verifierOrgName;
    private LocalDateTime createdAt;

    public VolunteerHours() {
        this.verificationStatus = VerificationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public VolunteerHours(Long id, Long volunteerId, Long opportunityId, double hoursWorked,
                          LocalDate workDate, String description, VerificationStatus verificationStatus) {
        this.id = id;
        this.volunteerId = volunteerId;
        this.opportunityId = opportunityId;
        this.hoursWorked = hoursWorked;
        this.workDate = workDate;
        this.description = description;
        this.verificationStatus = verificationStatus != null ? verificationStatus : VerificationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // ====================================================================
    // Business Logic Helpers
    // ====================================================================

    public boolean isApproved() {
        return this.verificationStatus == VerificationStatus.APPROVED;
    }

    public boolean isPending() {
        return this.verificationStatus == VerificationStatus.PENDING;
    }

    public boolean isRejected() {
        return this.verificationStatus == VerificationStatus.REJECTED;
    }

    public String getFormattedWorkDate() {
        return workDate != null ? workDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "";
    }

    // ====================================================================
    // Getters and Setters
    // ====================================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(Long volunteerId) {
        this.volunteerId = volunteerId;
    }

    public String getVolunteerName() {
        return volunteerName;
    }

    public void setVolunteerName(String volunteerName) {
        this.volunteerName = volunteerName;
    }

    public Long getOpportunityId() {
        return opportunityId;
    }

    public void setOpportunityId(Long opportunityId) {
        this.opportunityId = opportunityId;
    }

    public String getOpportunityTitle() {
        return opportunityTitle;
    }

    public void setOpportunityTitle(String opportunityTitle) {
        this.opportunityTitle = opportunityTitle;
    }

    public double getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(double hoursWorked) {
        this.hoursWorked = hoursWorked;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Long getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(Long verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getVerifierOrgName() {
        return verifierOrgName;
    }

    public void setVerifierOrgName(String verifierOrgName) {
        this.verifierOrgName = verifierOrgName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VolunteerHours that = (VolunteerHours) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "VolunteerHours [id=" + id + ", volunteer=" + volunteerId + ", hours=" + hoursWorked + ", status=" + verificationStatus + "]";
    }
}
