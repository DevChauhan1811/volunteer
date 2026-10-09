package com.volunteer.view.admin;

import com.volunteer.model.Admin;
import com.volunteer.service.SystemSettingsService;
import com.volunteer.service.impl.SystemSettingsServiceImpl;
import com.volunteer.view.common.ToastNotification;
import com.volunteer.view.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * System Settings configuration panel for Administrators.
 * Persists application-wide parameters in the MySQL system_settings table.
 */
public class SystemSettingsPanel extends JPanel {

    private final Admin adminUser;
    private final SystemSettingsService settingsService;

    private JTextField platformNameField;
    private JTextField maxCapacityField;
    private JCheckBox orgApprovalCheck;
    private JCheckBox allowHourSubmissionCheck;

    private JButton saveButton;
    private JButton refreshButton;
    private JLabel statusLabel;

    public SystemSettingsPanel(Admin admin) {
        this.adminUser = admin;
        this.settingsService = new SystemSettingsServiceImpl();
        initializeUI();
        loadSettingsAsync();
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

        JLabel title = new JLabel("System Settings & Global Configuration");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel sub = new JLabel("Manage global thresholds and persistence rules. Saved directly in MySQL.");
        sub.setFont(UITheme.FONT_REGULAR);
        sub.setForeground(UITheme.TEXT_MUTED);

        titleGrp.add(title);
        titleGrp.add(Box.createVerticalStrut(4));
        titleGrp.add(sub);
        top.add(titleGrp, BorderLayout.WEST);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);
        top.add(statusLabel, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        // Center Form
        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 16));
        center.setOpaque(false);

        JPanel card = UITheme.createCardPanel();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(640, 420));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        int row = 0;
        gbc.gridy = row++;
        card.add(UITheme.createFieldLabel("Platform Application Name"), gbc);
        platformNameField = UITheme.createTextField(30);
        gbc.gridy = row++;
        card.add(platformNameField, gbc);

        gbc.gridy = row++;
        card.add(UITheme.createFieldLabel("Maximum Volunteer Capacity Per Opportunity"), gbc);
        maxCapacityField = UITheme.createTextField(30);
        gbc.gridy = row++;
        card.add(maxCapacityField, gbc);

        gbc.gridy = row++;
        orgApprovalCheck = new JCheckBox("Require Admin Approval for New Organization Registrations");
        orgApprovalCheck.setFont(UITheme.FONT_REGULAR);
        orgApprovalCheck.setOpaque(false);
        card.add(orgApprovalCheck, gbc);

        gbc.gridy = row++;
        allowHourSubmissionCheck = new JCheckBox("Allow Volunteers to Self-Log Hours for Registered Opportunities");
        allowHourSubmissionCheck.setFont(UITheme.FONT_REGULAR);
        allowHourSubmissionCheck.setOpaque(false);
        card.add(allowHourSubmissionCheck, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(20, 8, 8, 8);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        refreshButton = UITheme.createSecondaryButton("Reload");
        refreshButton.addActionListener(e -> loadSettingsAsync());

        saveButton = UITheme.createPrimaryButton("Save System Settings");
        saveButton.addActionListener(e -> handleSaveSettings());

        actions.add(refreshButton);
        actions.add(saveButton);
        card.add(actions, gbc);

        center.add(card);
        add(new JScrollPane(center), BorderLayout.CENTER);
    }

    public void loadSettingsAsync() {
        statusLabel.setText("Loading settings...");
        saveButton.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            String platformName;
            int maxCap;
            boolean orgApproval;
            boolean allowHours;

            @Override
            protected Void doInBackground() throws Exception {
                platformName = settingsService.getSetting("platform_name", "Online Volunteer Management Platform");
                maxCap = settingsService.getIntSetting("max_volunteer_capacity", 100);
                orgApproval = settingsService.getBooleanSetting("org_registration_approval_required", false);
                allowHours = settingsService.getBooleanSetting("allow_hour_self_submission", true);
                return null;
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                try {
                    get();
                    platformNameField.setText(platformName);
                    maxCapacityField.setText(String.valueOf(maxCap));
                    orgApprovalCheck.setSelected(orgApproval);
                    allowHourSubmissionCheck.setSelected(allowHours);
                    statusLabel.setText("Settings up to date");
                } catch (Exception ex) {
                    statusLabel.setText("Failed loading settings");
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(SystemSettingsPanel.this, "Error: " + cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void handleSaveSettings() {
        String name = platformNameField.getText().trim();
        String capStr = maxCapacityField.getText().trim();
        boolean orgApproval = orgApprovalCheck.isSelected();
        boolean allowHours = allowHourSubmissionCheck.isSelected();

        saveButton.setEnabled(false);
        saveButton.setText("Saving...");
        statusLabel.setText("Updating MySQL records...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                settingsService.updateSetting(adminUser, "platform_name", name);
                settingsService.updateSetting(adminUser, "max_volunteer_capacity", capStr);
                settingsService.updateSetting(adminUser, "org_registration_approval_required", String.valueOf(orgApproval));
                settingsService.updateSetting(adminUser, "allow_hour_self_submission", String.valueOf(allowHours));
                return null;
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                saveButton.setText("Save System Settings");
                try {
                    get();
                    statusLabel.setText("Saved successfully");
                    ToastNotification.showSuccess(SystemSettingsPanel.this, "System settings updated successfully!");
                } catch (Exception ex) {
                    statusLabel.setText("Save failed");
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(SystemSettingsPanel.this, cause.getMessage());
                }
            }
        };
        worker.execute();
    }
}
