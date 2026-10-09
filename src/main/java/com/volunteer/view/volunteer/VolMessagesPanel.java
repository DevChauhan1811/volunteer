package com.volunteer.view.volunteer;

import com.volunteer.model.Message;
import com.volunteer.model.Opportunity;
import com.volunteer.model.Signup;
import com.volunteer.model.Volunteer;
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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * Volunteer messaging panel for communicating with organizations regarding specific opportunities.
 * Allows selecting a sign-up, composing a message, and reading the conversation thread.
 */
public class VolMessagesPanel extends JPanel {

    private final Volunteer volunteerUser;
    private final MessageService messageService;
    private final SignupService signupService;

    private JComboBox<String> signupCombo;
    private DefaultListModel<String> conversationModel;
    private JList<String> conversationList;
    private JTextArea composeArea;
    private JButton sendBtn;
    private JLabel statusLabel;

    private final List<Signup> mySignups = new ArrayList<>();
    private final List<Message> conversationMessages = new ArrayList<>();

    public VolMessagesPanel(Volunteer volunteer) {
        this.volunteerUser = volunteer;
        this.messageService = new MessageServiceImpl();
        this.signupService = new SignupServiceImpl();
        initializeUI();
        loadSignupsAsync();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ---- Top Bar ----
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JPanel titleGrp = new JPanel();
        titleGrp.setLayout(new BoxLayout(titleGrp, BoxLayout.Y_AXIS));
        titleGrp.setOpaque(false);
        JLabel title = new JLabel("Messages & Inquiries");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TEXT_MAIN);
        JLabel sub = new JLabel("Contact host organizations about your registered opportunities.");
        sub.setFont(UITheme.FONT_REGULAR);
        sub.setForeground(UITheme.TEXT_MUTED);
        titleGrp.add(title);
        titleGrp.add(Box.createVerticalStrut(4));
        titleGrp.add(sub);

        statusLabel = new JLabel("");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topRight.setOpaque(false);
        topRight.add(statusLabel);

        top.add(titleGrp, BorderLayout.WEST);
        top.add(topRight, BorderLayout.EAST);

        // ---- Opportunity Selector ----
        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        selectorPanel.setOpaque(false);
        selectorPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JLabel oppLabel = UITheme.createFieldLabel("Select Opportunity:");
        signupCombo = new JComboBox<>();
        signupCombo.setFont(UITheme.FONT_REGULAR);
        signupCombo.setPreferredSize(new Dimension(360, 34));
        signupCombo.addActionListener(e -> loadConversationAsync());

        JButton refreshBtn = UITheme.createSecondaryButton("🔄  Refresh");
        refreshBtn.addActionListener(e -> loadSignupsAsync());

        selectorPanel.add(oppLabel);
        selectorPanel.add(signupCombo);
        selectorPanel.add(refreshBtn);

        // ---- Conversation List ----
        conversationModel = new DefaultListModel<>();
        conversationList = new JList<>(conversationModel);
        conversationList.setFont(UITheme.FONT_REGULAR);
        conversationList.setBackground(UITheme.BG_CARD);
        conversationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conversationList.setFixedCellHeight(56);
        conversationList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JPanel cell = new JPanel(new BorderLayout());
            cell.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER));
            boolean isMine = index < conversationMessages.size()
                    && conversationMessages.get(index).getSenderId() != null
                    && conversationMessages.get(index).getSenderId().equals(volunteerUser.getId());
            cell.setBackground(isSelected ? UITheme.PRIMARY_LIGHT : (isMine ? new Color(240, 253, 244) : UITheme.BG_CARD));

            JLabel msgText = new JLabel("<html><body style='width:300px'>" +
                    (isMine ? "You: " : "Org: ") + value + "</body></html>");
            msgText.setFont(UITheme.FONT_REGULAR);
            msgText.setForeground(UITheme.TEXT_MAIN);
            msgText.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            cell.add(msgText, BorderLayout.CENTER);

            if (index < conversationMessages.size()) {
                JLabel timeLabel = new JLabel(conversationMessages.get(index).getFormattedSentAt());
                timeLabel.setFont(UITheme.FONT_SMALL);
                timeLabel.setForeground(UITheme.TEXT_MUTED);
                timeLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 12));
                cell.add(timeLabel, BorderLayout.EAST);
            }
            return cell;
        });

        JScrollPane convScroll = new JScrollPane(conversationList);
        convScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));

        // ---- Compose Area ----
        JPanel composePanel = UITheme.createCardPanel();
        composePanel.setLayout(new BorderLayout(8, 8));
        composePanel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel composeLabel = UITheme.createFieldLabel("New Message:");
        composeArea = new JTextArea(3, 40);
        composeArea.setFont(UITheme.FONT_REGULAR);
        composeArea.setWrapStyleWord(true);
        composeArea.setLineWrap(true);
        composeArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        sendBtn = UITheme.createPrimaryButton("✉️  Send Message");
        sendBtn.setEnabled(false);
        sendBtn.addActionListener(e -> handleSendMessage());

        JPanel sendRow = new JPanel(new BorderLayout(8, 0));
        sendRow.setOpaque(false);
        sendRow.add(new JScrollPane(composeArea), BorderLayout.CENTER);
        sendRow.add(sendBtn, BorderLayout.EAST);

        composePanel.add(composeLabel, BorderLayout.NORTH);
        composePanel.add(sendRow, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, convScroll, composePanel);
        splitPane.setResizeWeight(0.70);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(selectorPanel, BorderLayout.NORTH);
        centerPanel.add(splitPane, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void handleSendMessage() {
        int idx = signupCombo.getSelectedIndex();
        if (idx < 0 || idx >= mySignups.size()) {
            ToastNotification.showError(this, "Please select an opportunity first.");
            return;
        }
        String text = composeArea.getText().trim();
        if (text.isBlank()) {
            ToastNotification.showError(this, "Message cannot be empty.");
            return;
        }

        Signup signup = mySignups.get(idx);
        sendBtn.setEnabled(false);
        sendBtn.setText("Sending...");

        new SwingWorker<Void, Void>() {
            String errMsg = null;

            @Override
            protected Void doInBackground() {
                try {
                    // Recipient is the organization's user ID (signup doesn't store org user id directly,
                    // so we use opportunity's organizationId as a proxy - service layer resolves)
                    messageService.sendMessage(volunteerUser, signup.getOpportunityId(), signup.getOpportunityId(), text);
                } catch (Exception ex) {
                    errMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                sendBtn.setText("✉️  Send Message");
                sendBtn.setEnabled(true);
                if (errMsg != null) {
                    ToastNotification.showError(VolMessagesPanel.this, "Send failed: " + errMsg);
                } else {
                    composeArea.setText("");
                    ToastNotification.showSuccess(VolMessagesPanel.this, "Message sent!");
                    loadConversationAsync();
                }
            }
        }.execute();
    }

    private void loadSignupsAsync() {
        statusLabel.setText("Loading sign-ups...");
        signupCombo.removeAllItems();
        mySignups.clear();
        conversationModel.clear();
        sendBtn.setEnabled(false);

        new SwingWorker<List<Signup>, Void>() {
            @Override
            protected List<Signup> doInBackground() throws Exception {
                return signupService.getSignupsByVolunteer(volunteerUser, volunteerUser.getId());
            }

            @Override
            protected void done() {
                try {
                    List<Signup> list = get();
                    mySignups.addAll(list);
                    for (Signup s : list) {
                        signupCombo.addItem((s.getOpportunityTitle() != null ? s.getOpportunityTitle() : "Opportunity #" + s.getOpportunityId())
                                + " — " + (s.getOrganizationName() != null ? s.getOrganizationName() : "Org"));
                    }
                    if (list.isEmpty()) {
                        signupCombo.addItem("— No sign-ups yet —");
                        statusLabel.setText("Sign up for opportunities to message organizations.");
                    } else {
                        statusLabel.setText(list.size() + " sign-up(s) found");
                        sendBtn.setEnabled(true);
                        loadConversationAsync();
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Error loading");
                    ToastNotification.showError(VolMessagesPanel.this, "Failed: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void loadConversationAsync() {
        int idx = signupCombo.getSelectedIndex();
        if (idx < 0 || idx >= mySignups.size()) {
            conversationModel.clear();
            return;
        }
        Signup signup = mySignups.get(idx);
        conversationModel.clear();
        conversationMessages.clear();

        new SwingWorker<List<Message>, Void>() {
            @Override
            protected List<Message> doInBackground() throws Exception {
                // Fetch conversation for this volunteer and the opportunity's organization
                return messageService.getConversation(volunteerUser, signup.getOpportunityId(), signup.getOpportunityId());
            }

            @Override
            protected void done() {
                try {
                    List<Message> msgs = get();
                    conversationMessages.addAll(msgs);
                    for (Message m : msgs) {
                        conversationModel.addElement(m.getMessageText());
                    }
                    if (msgs.isEmpty()) {
                        conversationModel.addElement("(No messages yet. Start the conversation below.)");
                    }
                    // Auto-scroll to bottom
                    conversationList.ensureIndexIsVisible(conversationModel.getSize() - 1);
                } catch (Exception ex) {
                    conversationModel.addElement("(Could not load messages: " + ex.getMessage() + ")");
                }
            }
        }.execute();
    }
}
