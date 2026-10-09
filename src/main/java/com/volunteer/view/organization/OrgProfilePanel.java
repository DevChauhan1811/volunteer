package com.volunteer.view.organization;

import com.volunteer.model.Organization;
import com.volunteer.service.UserService;
import com.volunteer.service.impl.UserServiceImpl;
import com.volunteer.view.common.ToastNotification;
import com.volunteer.view.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Organization Profile panel for viewing and updating organization details.
 */
public class OrgProfilePanel extends JPanel {

    private final Organization orgUser;
    private final UserService userService;

    private JTextField orgNameField;
    private JTextField repNameField;
    private JTextField emailField;
    private JTextField contactField;
    private JTextArea descArea;

    private JButton saveButton;

    public OrgProfilePanel(Organization org) {
        this.orgUser = org;
        this.userService = new UserServiceImpl();
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel titleGrp = new JPanel();
        titleGrp.setLayout(new BoxLayout(titleGrp, BoxLayout.Y_AXIS));
        titleGrp.setOpaque(false);

        JLabel title = new JLabel("Organization Profile Settings");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel sub = new JLabel("Manage public organization information displayed to volunteers.");
        sub.setFont(UITheme.FONT_REGULAR);
        sub.setForeground(UITheme.TEXT_MUTED);

        titleGrp.add(title);
        titleGrp.add(Box.createVerticalStrut(4));
        titleGrp.add(sub);
        top.add(titleGrp, BorderLayout.WEST);

        add(top, BorderLayout.NORTH);

        // Center Form Card
        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 16));
        center.setOpaque(false);

        JPanel card = UITheme.createCardPanel();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(640, 520));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        int row = 0;
        gbc.gridy = row++;
        card.add(UITheme.createFieldLabel("Organization Name *"), gbc);
        orgNameField = UITheme.createTextField(30);
        orgNameField.setText(orgUser.getOrganizationName());
        gbc.gridy = row++;
        card.add(orgNameField, gbc);

        gbc.gridy = row++;
        card.add(UITheme.createFieldLabel("Authorized Representative Name *"), gbc);
        repNameField = UITheme.createTextField(30);
        repNameField.setText(orgUser.getFullName());
        gbc.gridy = row++;
        card.add(repNameField, gbc);

        gbc.gridy = row++;
        card.add(UITheme.createFieldLabel("Account Email Address (Login Username) *"), gbc);
        emailField = UITheme.createTextField(30);
        emailField.setText(orgUser.getEmail());
        emailField.setEditable(false); // Email serves as unique identifier
        emailField.setBackground(new java.awt.Color(241, 245, 249));
        gbc.gridy = row++;
        card.add(emailField, gbc);

        gbc.gridy = row++;
        card.add(UITheme.createFieldLabel("Contact Information (Phone / Address)"), gbc);
        contactField = UITheme.createTextField(30);
        contactField.setText(orgUser.getContactInformation());
        gbc.gridy = row++;
        card.add(contactField, gbc);

        gbc.gridy = row++;
        card.add(UITheme.createFieldLabel("Mission Statement & Description"), gbc);
        descArea = new JTextArea(4, 30);
        descArea.setFont(UITheme.FONT_REGULAR);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setText(orgUser.getDescription());
        JScrollPane sp = new JScrollPane(descArea);
        gbc.gridy = row++;
        card.add(sp, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(16, 8, 8, 8);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);

        saveButton = UITheme.createPrimaryButton("Save Profile Details");
        saveButton.addActionListener(e -> handleSave());
        actions.add(saveButton);

        card.add(actions, gbc);

        center.add(card);
        add(new JScrollPane(center), BorderLayout.CENTER);
    }

    private void handleSave() {
        String orgName = orgNameField.getText().trim();
        String repName = repNameField.getText().trim();
        String contact = contactField.getText().trim();
        String desc = descArea.getText().trim();

        if (orgName.isEmpty() || repName.isEmpty()) {
            ToastNotification.showError(this, "Organization name and representative name cannot be empty.");
            return;
        }

        saveButton.setEnabled(false);
        saveButton.setText("Saving...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                orgUser.setOrganizationName(orgName);
                orgUser.setFullName(repName);
                orgUser.setContactInformation(contact);
                orgUser.setDescription(desc);
                userService.updateUser(orgUser, orgUser);
                return null;
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                saveButton.setText("Save Profile Details");
                try {
                    get();
                    ToastNotification.showSuccess(OrgProfilePanel.this, "Profile updated successfully!");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(OrgProfilePanel.this, cause.getMessage());
                }
            }
        };
        worker.execute();
    }
}
