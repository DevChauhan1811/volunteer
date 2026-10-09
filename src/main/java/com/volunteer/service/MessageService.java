package com.volunteer.service;

import com.volunteer.exception.DatabaseException;
import com.volunteer.exception.UnauthorizedException;
import com.volunteer.exception.ValidationException;
import com.volunteer.model.Message;
import com.volunteer.model.User;

import java.util.List;

/**
 * Service contract for secure communication between authorized volunteers and organizations.
 */
public interface MessageService {

    /**
     * Sends a message from the initiator to a specified recipient regarding an opportunity.
     */
    Message sendMessage(User initiator, Long recipientId, Long opportunityId, String text)
        throws UnauthorizedException, ValidationException, DatabaseException;

    /**
     * Retrieves conversation messages between the initiator and another user for an opportunity.
     */
    List<Message> getConversation(User initiator, Long otherUserId, Long opportunityId)
        throws UnauthorizedException, DatabaseException;

    /**
     * Marks messages sent to the initiator as read.
     */
    void markAsRead(User initiator, Long messageId) throws DatabaseException;
}
