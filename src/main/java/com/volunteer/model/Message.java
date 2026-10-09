package com.volunteer.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Domain entity representing a direct communication message
 * between a volunteer and an organization regarding an opportunity.
 */
public class Message {

    private Long id;
    private Long senderId;
    private String senderName;
    private Long recipientId;
    private String recipientName;
    private Long opportunityId;
    private String opportunityTitle;
    private String messageText;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;

    public Message() {
        this.sentAt = LocalDateTime.now();
    }

    public Message(Long id, Long senderId, Long recipientId, Long opportunityId, String messageText) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.opportunityId = opportunityId;
        this.messageText = messageText;
        this.sentAt = LocalDateTime.now();
    }

    public boolean isRead() {
        return readAt != null;
    }

    public String getFormattedSentAt() {
        return sentAt != null ? sentAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) : "";
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

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
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

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Message [id=" + id + ", from=" + senderId + ", to=" + recipientId + ", text='" + messageText + "']";
    }
}
