package com.volunteer.model;

import com.volunteer.model.enums.RegistrationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Domain entity representing a volunteer registration for an opportunity.
 */
public class Signup {

    private Long id;
    private Long volunteerId;
    private String volunteerName;
    private String volunteerEmail;
    private Long opportunityId;
    private String opportunityTitle;
    private String organizationName;
    private LocalDate eventDate;
    private String location;
    private RegistrationStatus registrationStatus;
    private LocalDateTime registeredAt;
    private LocalDateTime updatedAt;

    public Signup() {
        this.registrationStatus = RegistrationStatus.REGISTERED;
        this.registeredAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Signup(Long id, Long volunteerId, Long opportunityId, RegistrationStatus registrationStatus) {
        this.id = id;
        this.volunteerId = volunteerId;
        this.opportunityId = opportunityId;
        this.registrationStatus = registrationStatus != null ? registrationStatus : RegistrationStatus.REGISTERED;
        this.registeredAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ====================================================================
    // Business Logic Helpers
    // ====================================================================

    public boolean isRegistered() {
        return this.registrationStatus == RegistrationStatus.REGISTERED;
    }

    public boolean isCancelled() {
        return this.registrationStatus == RegistrationStatus.CANCELLED;
    }

    public boolean isCompleted() {
        return this.registrationStatus == RegistrationStatus.COMPLETED || this.registrationStatus == RegistrationStatus.ATTENDED;
    }

    /**
     * Determines whether the volunteer can cancel this registration.
     * Cancellation is only allowed if status is REGISTERED and the event is today or in the future.
     */
    public boolean canCancel() {
        if (registrationStatus != RegistrationStatus.REGISTERED) {
            return false;
        }
        if (eventDate != null) {
            return !eventDate.isBefore(LocalDate.now());
        }
        return true;
    }

    public String getFormattedRegisteredAt() {
        return registeredAt != null ? registeredAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) : "";
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

    public String getVolunteerEmail() {
        return volunteerEmail;
    }

    public void setVolunteerEmail(String volunteerEmail) {
        this.volunteerEmail = volunteerEmail;
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

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public RegistrationStatus getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(RegistrationStatus registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Signup signup = (Signup) o;
        return Objects.equals(id, signup.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Signup [id=" + id + ", volunteer=" + volunteerId + ", opp=" + opportunityId + ", status=" + registrationStatus + "]";
    }
}
