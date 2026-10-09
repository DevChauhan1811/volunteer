package com.volunteer.service.impl;

import com.volunteer.dao.MessageDAO;
import com.volunteer.dao.UserDAO;
import com.volunteer.exception.DatabaseException;
import com.volunteer.exception.UnauthorizedException;
import com.volunteer.exception.ValidationException;
import com.volunteer.model.Message;
import com.volunteer.model.User;
import com.volunteer.service.MessageService;
import com.volunteer.util.ValidationUtil;

import java.util.List;

/**
 * Service implementation for volunteer and organization communication.
 */
public class MessageServiceImpl implements MessageService {

    private final MessageDAO messageDAO;
    private final UserDAO userDAO;

    public MessageServiceImpl() {
        this.messageDAO = new MessageDAO();
        this.userDAO = new UserDAO();
    }

    public MessageServiceImpl(MessageDAO messageDAO, UserDAO userDAO) {
        this.messageDAO = messageDAO;
        this.userDAO = userDAO;
    }

    @Override
    public Message sendMessage(User initiator, Long recipientId, Long opportunityId, String text)
            throws UnauthorizedException, ValidationException, DatabaseException {

        if (initiator == null) {
            throw new UnauthorizedException("Authentication required to send messages.");
        }
        if (recipientId == null) {
            throw new ValidationException("recipientId", "Recipient must be specified.");
        }
        if (initiator.getId().equals(recipientId)) {
            throw new ValidationException("recipientId", "Cannot send a message to yourself.");
        }
        ValidationUtil.validateNotEmpty(text, "messageText", "Message text");

        if (userDAO.findById(recipientId).isEmpty()) {
            throw new ValidationException("recipientId", "Recipient user #" + recipientId + " not found.");
        }

        Message msg = new Message();
        msg.setSenderId(initiator.getId());
        msg.setRecipientId(recipientId);
        msg.setOpportunityId(opportunityId);
        msg.setMessageText(text.trim());

        return messageDAO.create(msg);
    }

    @Override
    public List<Message> getConversation(User initiator, Long otherUserId, Long opportunityId)
            throws UnauthorizedException, DatabaseException {

        if (initiator == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        return messageDAO.findConversation(initiator.getId(), otherUserId, opportunityId);
    }

    @Override
    public void markAsRead(User initiator, Long messageId) throws DatabaseException {
        if (initiator == null || messageId == null) return;
        messageDAO.markAsRead(messageId);
    }
}
