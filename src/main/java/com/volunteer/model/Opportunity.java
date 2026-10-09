package com.volunteer.model;

import com.volunteer.model.enums.ApprovalStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Domain entity representing a volunteer opportunity posted by an organization.
 */
public class Opportunity {

    private Long id;
    private Long organizationId;
    private String organizationName; // Joined from organizations table for UI display
    private String title;
    private String description;
    private String category;
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private int capacity;
    private int currentSignups; // Populated from DB aggregation
    private ApprovalStatus approvalStatus;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Opportunity() {
        this.approvalStatus = ApprovalStatus.PENDING;
        this.currentSignups = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Opportunity(Long id, Long organizationId, String title, String description, String category,
                       LocalDate eventDate, LocalTime startTime, LocalTime endTime, String location,
                       int capacity, ApprovalStatus approvalStatus) {
        this.id = id;
        this.organizationId = organizationId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.capacity = capacity;
        this.approvalStatus = approvalStatus != null ? approvalStatus : ApprovalStatus.PENDING;
        this.currentSignups = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ====================================================================
    // Business Logic Helpers
    // ====================================================================

    public int getAvailableSlots() {
        return Math.max(0, capacity - currentSignups);
    }

    public boolean isFull() {
        return currentSignups >= capacity;
    }

    public boolean isApproved() {
        return this.approvalStatus == ApprovalStatus.APPROVED;
    }

    public boolean isPending() {
        return this.approvalStatus == ApprovalStatus.PENDING;
    }

    public boolean isRejected() {
        return this.approvalStatus == ApprovalStatus.REJECTED;
    }

    public String getFormattedDate() {
        return eventDate != null ? eventDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "";
    }

    public String getFormattedTimeRange() {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");
        if (startTime != null && endTime != null) {
            return startTime.format(timeFmt) + " - " + endTime.format(timeFmt);
        } else if (startTime != null) {
            return startTime.format(timeFmt);
        }
        return "";
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

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getCurrentSignups() {
        return currentSignups;
    }

    public void setCurrentSignups(int currentSignups) {
        this.currentSignups = currentSignups;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
        Opportunity that = (Opportunity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Opportunity [id=" + id + ", title='" + title + "', date=" + eventDate + ", status=" + approvalStatus + "]";
    }
}
