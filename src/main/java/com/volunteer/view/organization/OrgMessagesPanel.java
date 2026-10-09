package com.volunteer.view.organization;

import com.volunteer.model.Message;
import com.volunteer.model.Opportunity;
import com.volunteer.model.Organization;
import com.volunteer.model.Signup;
import com.volunteer.service.MessageService;
import com.volunteer.service.OpportunityService;
import com.volunteer.service.SignupService;
import com.volunteer.service.impl.MessageServiceImpl;
import com.volunteer.service.impl.OpportunityServiceImpl;
import com.volunteer.service.impl.SignupServiceImpl;
import com.volunteer.view.common.ToastNotification;
import com.volunteer.view.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Messaging interface allowing organizations to converse with registered volunteers
 * and answer inquiries regarding hosted opportunities.
 */
public class OrgMessagesPanel extends JPanel {

    private final Organization orgUser;
    private final MessageService messageService;
    private final SignupService signupService;
    private final OpportunityService oppService;

    private JList<String> volunteerJList;
    private DefaultListModel<String> listModel;
    private final List<Signup> participantList = new ArrayList<>();

    private JLabel chatHeaderLbl;
    private JPanel messagesContainer;
    private JScrollPane chatScrollPane;
    private JTextField replyField;
    private JButton sendButton;

    private Signup selectedParticipant;

    public OrgMessagesPanel(Organization org) {
        this.orgUser = org;
        this.messageService = new MessageServiceImpl();
        this.signupService = new SignupServiceImpl();
        this.oppService = new OpportunityServiceImpl();
        initializeUI();
        loadParticipantsAsync();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Top Header
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = new JLabel("Volunteer Inquiries & Communications");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel sub = new JLabel("Direct communication with registered participants.");
        sub.setFont(UITheme.FONT_REGULAR);
        sub.setForeground(UITheme.TEXT_MUTED);

        JPanel titleGrp = new JPanel();
        titleGrp.setLayout(new BoxLayout(titleGrp, BoxLayout.Y_AXIS));
        titleGrp.setOpaque(false);
        titleGrp.add(title);
        titleGrp.add(Box.createVerticalStrut(4));
        titleGrp.add(sub);
        top.add(titleGrp, BorderLayout.WEST);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh Inquiries");
        refreshBtn.addActionListener(e -> loadParticipantsAsync());
        top.add(refreshBtn, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        // Center Split: Left Sidebar of Volunteers + Right Chat
        JPanel centerSplit = new JPanel(new BorderLayout(16, 0));
        centerSplit.setOpaque(false);
        centerSplit.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        // Left List
        JPanel leftPanel = UITheme.createCardPanel();
        leftPanel.setLayout(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(300, 0));

        JLabel listHeader = new JLabel("Registered Participants");
        listHeader.setFont(UITheme.FONT_SUBHEADER);
        listHeader.setForeground(UITheme.TEXT_MAIN);
        listHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        leftPanel.add(listHeader, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        volunteerJList = new JList<>(listModel);
        volunteerJList.setFont(UITheme.FONT_REGULAR);
        volunteerJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        volunteerJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int idx = volunteerJList.getSelectedIndex();
                if (idx >= 0 && idx < participantList.size()) {
                    selectedParticipant = participantList.get(idx);
                    loadConversationAsync();
                }
            }
        });

        JScrollPane listScroll = new JScrollPane(volunteerJList);
        listScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        leftPanel.add(listScroll, BorderLayout.CENTER);
        centerSplit.add(leftPanel, BorderLayout.WEST);

        // Right Chat Container
        JPanel rightChat = UITheme.createCardPanel();
        rightChat.setLayout(new BorderLayout());

        chatHeaderLbl = new JLabel("Select a participant from the left to view messages.");
        chatHeaderLbl.setFont(UITheme.FONT_SUBHEADER);
        chatHeaderLbl.setForeground(UITheme.TEXT_MAIN);
        chatHeaderLbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
            BorderFactory.createEmptyBorder(0, 0, 10, 0)
        ));
        rightChat.add(chatHeaderLbl, BorderLayout.NORTH);

        messagesContainer = new JPanel();
        messagesContainer.setLayout(new BoxLayout(messagesContainer, BoxLayout.Y_AXIS));
        messagesContainer.setBackground(new Color(248, 250, 252));
        messagesContainer.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        chatScrollPane = new JScrollPane(messagesContainer);
        chatScrollPane.setBorder(null);
        rightChat.add(chatScrollPane, BorderLayout.CENTER);

        // Bottom Reply Input
        JPanel replyBar = new JPanel(new BorderLayout(8, 0));
        replyBar.setOpaque(false);
        replyBar.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        replyField = UITheme.createTextField(30);
        replyField.putClientProperty("JTextField.placeholderText", "Type reply message...");
        replyField.addActionListener(e -> handleSendReply());

        sendButton = UITheme.createPrimaryButton("Send Reply");
        sendButton.addActionListener(e -> handleSendReply());

        replyBar.add(replyField, BorderLayout.CENTER);
        replyBar.add(sendButton, BorderLayout.EAST);
        rightChat.add(replyBar, BorderLayout.SOUTH);

        centerSplit.add(rightChat, BorderLayout.CENTER);
        add(centerSplit, BorderLayout.CENTER);
    }

    public void loadParticipantsAsync() {
        listModel.clear();
        participantList.clear();

        SwingWorker<List<Signup>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Signup> doInBackground() throws Exception {
                List<Opportunity> opps = oppService.getOpportunitiesByOrganization(orgUser, orgUser.getOrganizationId());
                List<Signup> signups = new ArrayList<>();
                for (Opportunity opp : opps) {
                    signups.addAll(signupService.getSignupsByOpportunity(orgUser, opp.getId()));
                }
                return signups;
            }

            @Override
            protected void done() {
                try {
                    List<Signup> list = get();
                    participantList.addAll(list);
                    for (Signup s : list) {
                        listModel.addElement(s.getVolunteerName() + " (" + s.getOpportunityTitle() + ")");
                    }
                    if (!list.isEmpty()) {
                        volunteerJList.setSelectedIndex(0);
                    }
                } catch (Exception ex) {
                    ToastNotification.showError(OrgMessagesPanel.this, "Failed loading participants.");
                }
            }
        };
        worker.execute();
    }

    private void loadConversationAsync() {
        if (selectedParticipant == null) return;

        chatHeaderLbl.setText("Conversation with " + selectedParticipant.getVolunteerName() +
                              " • " + selectedParticipant.getOpportunityTitle());
        messagesContainer.removeAll();

        SwingWorker<List<Message>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Message> doInBackground() throws Exception {
                return messageService.getConversation(orgUser, selectedParticipant.getVolunteerId(), selectedParticipant.getOpportunityId());
            }

            @Override
            protected void done() {
                try {
                    List<Message> msgs = get();
                    if (msgs.isEmpty()) {
                        JLabel emptyLbl = new JLabel("No messages in this conversation yet. Send a message below to start.");
                        emptyLbl.setFont(UITheme.FONT_REGULAR);
                        emptyLbl.setForeground(UITheme.TEXT_MUTED);
                        messagesContainer.add(emptyLbl);
                    } else {
                        for (Message m : msgs) {
                            messagesContainer.add(createMessageBubble(m));
                            messagesContainer.add(Box.createVerticalStrut(10));
                        }
                    }
                    messagesContainer.revalidate();
                    messagesContainer.repaint();

                    // Scroll to bottom
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        chatScrollPane.getVerticalScrollBar().setValue(chatScrollPane.getVerticalScrollBar().getMaximum());
                    });
                } catch (Exception ex) {
                    ToastNotification.showError(OrgMessagesPanel.this, "Failed loading conversation.");
                }
            }
        };
        worker.execute();
    }

    private JPanel createMessageBubble(Message m) {
        boolean isMe = m.getSenderId().equals(orgUser.getId());

        JPanel row = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);

        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBackground(isMe ? UITheme.PRIMARY_LIGHT : Color.WHITE);
        bubble.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(isMe ? UITheme.PRIMARY : UITheme.BORDER, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel senderLbl = new JLabel(isMe ? "You" : m.getSenderName());
        senderLbl.setFont(UITheme.FONT_BOLD);
        senderLbl.setForeground(isMe ? UITheme.PRIMARY_HOVER : UITheme.TEXT_MAIN);

        JLabel textLbl = new JLabel("<html>" + m.getMessageText().replace("\n", "<br>") + "</html>");
        textLbl.setFont(UITheme.FONT_REGULAR);
        textLbl.setForeground(UITheme.TEXT_MAIN);

        JLabel timeLbl = new JLabel(m.getFormattedSentAt());
        timeLbl.setFont(UITheme.FONT_SMALL);
        timeLbl.setForeground(UITheme.TEXT_MUTED);

        bubble.add(senderLbl);
        bubble.add(Box.createVerticalStrut(4));
        bubble.add(textLbl);
        bubble.add(Box.createVerticalStrut(4));
        bubble.add(timeLbl);

        row.add(bubble);
        return row;
    }

    private void handleSendReply() {
        if (selectedParticipant == null) {
            ToastNotification.showWarning(this, "Please select a participant to message.");
            return;
        }

        String text = replyField.getText().trim();
        if (text.isEmpty()) return;

        replyField.setText("");
        sendButton.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                messageService.sendMessage(orgUser, selectedParticipant.getVolunteerId(), selectedParticipant.getOpportunityId(), text);
                return null;
            }

            @Override
            protected void done() {
                sendButton.setEnabled(true);
                try {
                    get();
                    loadConversationAsync();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(OrgMessagesPanel.this, cause.getMessage());
                }
            }
        };
        worker.execute();
    }
}
