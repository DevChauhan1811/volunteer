package com.volunteer.dao;

import com.volunteer.exception.DatabaseException;
import com.volunteer.model.Message;
import com.volunteer.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Message entities.
 * Handles bidirectional conversations between volunteers and organizations.
 */
public class MessageDAO implements BaseDAO<Message, Long> {

    private static final String BASE_SELECT = """
        SELECT m.id, m.sender_id, u1.full_name AS sender_name,
               m.recipient_id, u2.full_name AS recipient_name,
               m.opportunity_id, o.title AS opportunity_title,
               m.message_text, m.sent_at, m.read_at
        FROM messages m
        JOIN users u1 ON m.sender_id = u1.id
        JOIN users u2 ON m.recipient_id = u2.id
        LEFT JOIN opportunities o ON m.opportunity_id = o.id
    """;

    @Override
    public Message create(Message msg) throws DatabaseException {
        String sql = "INSERT INTO messages (sender_id, recipient_id, opportunity_id, message_text) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, msg.getSenderId());
            ps.setLong(2, msg.getRecipientId());
            if (msg.getOpportunityId() != null) {
                ps.setLong(3, msg.getOpportunityId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            ps.setString(4, msg.getMessageText());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Failed inserting message.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    msg.setId(rs.getLong(1));
                }
            }
            return msg;
        } catch (SQLException e) {
            throw new DatabaseException("Failed creating message: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Message> findById(Long id) throws DatabaseException {
        String sql = BASE_SELECT + " WHERE m.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToMessage(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding message #" + id + ": " + e.getMessage(), e);
        }
    }

    public List<Message> findConversation(Long userId1, Long userId2, Long opportunityId) throws DatabaseException {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("""
            WHERE ((m.sender_id = ? AND m.recipient_id = ?)
               OR  (m.sender_id = ? AND m.recipient_id = ?))
        """);

        if (opportunityId != null) {
            sql.append(" AND m.opportunity_id = ?");
        }

        sql.append(" ORDER BY m.sent_at ASC");

        List<Message> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            ps.setLong(1, userId1);
            ps.setLong(2, userId2);
            ps.setLong(3, userId2);
            ps.setLong(4, userId1);
            if (opportunityId != null) {
                ps.setLong(5, opportunityId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToMessage(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed retrieving conversation: " + e.getMessage(), e);
        }
    }

    public boolean markAsRead(Long messageId) throws DatabaseException {
        String sql = "UPDATE messages SET read_at = CURRENT_TIMESTAMP WHERE id = ? AND read_at IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, messageId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed marking message as read: " + e.getMessage(), e);
        }
    }

    public int countUnreadForUser(Long recipientId) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM messages WHERE recipient_id = ? AND read_at IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, recipientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed counting unread messages: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Message> findAll() throws DatabaseException {
        String sql = BASE_SELECT + " ORDER BY m.sent_at DESC";

        List<Message> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToMessage(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed retrieving all messages: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Message entity) throws DatabaseException {
        String sql = "UPDATE messages SET message_text = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, entity.getMessageText());
            ps.setLong(2, entity.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed updating message: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) throws DatabaseException {
        String sql = "DELETE FROM messages WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed deleting message: " + e.getMessage(), e);
        }
    }

    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        Message msg = new Message();
        msg.setId(rs.getLong("id"));
        msg.setSenderId(rs.getLong("sender_id"));
        msg.setSenderName(rs.getString("sender_name"));
        msg.setRecipientId(rs.getLong("recipient_id"));
        msg.setRecipientName(rs.getString("recipient_name"));

        long oppId = rs.getLong("opportunity_id");
        if (!rs.wasNull()) {
            msg.setOpportunityId(oppId);
        }
        msg.setOpportunityTitle(rs.getString("opportunity_title"));
        msg.setMessageText(rs.getString("message_text"));

        Timestamp s = rs.getTimestamp("sent_at");
        if (s != null) msg.setSentAt(s.toLocalDateTime());

        Timestamp r = rs.getTimestamp("read_at");
        if (r != null) msg.setReadAt(r.toLocalDateTime());

        return msg;
    }
}
