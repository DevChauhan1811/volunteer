package com.volunteer.view.volunteer;

import com.volunteer.model.Signup;
import com.volunteer.model.Volunteer;
import com.volunteer.model.VolunteerHours;
import com.volunteer.model.enums.RegistrationStatus;
import com.volunteer.model.enums.VerificationStatus;
import com.volunteer.service.SignupService;
import com.volunteer.service.VolunteerHoursService;
import com.volunteer.service.impl.SignupServiceImpl;
import com.volunteer.service.impl.VolunteerHoursServiceImpl;
import com.volunteer.view.common.ModernTable;
import com.volunteer.view.common.ToastNotification;
import com.volunteer.view.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Volunteer panel for submitting service hours and reviewing verification status.
 * Displays total verified hours summary and allows submitting hours for completed sign-ups.
 */
public class VolHoursPanel extends JPanel {

    private final Volunteer volunteerUser;
    private final VolunteerHoursService hoursService;
    private final SignupService signupService;

    private ModernTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JLabel totalHoursLbl;

    // Submit form
    private JComboBox<String> opportunityCombo;
    private JSpinner hoursSpinner;
    private JSpinner dateSpinner;
    private JTextArea notesArea;
    private JButton submitBtn;

    private final List<VolunteerHours> currentHours = new ArrayList<>();
    private final List<Signup> completedSignups = new ArrayList<>();

    public VolHoursPanel(Volunteer volunteer) {
        this.volunteerUser = volunteer;
        this.hoursService = new VolunteerHoursServiceImpl();
        this.signupService = new SignupServiceImpl();
        initializeUI();
        loadHoursAsync();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ---- Left: Table of logged hours ----
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        JPanel titleGrp = new JPanel();
        titleGrp.setLayout(new BoxLayout(titleGrp, BoxLayout.Y_AXIS));
        titleGrp.setOpaque(false);
        JLabel title = new JLabel("My Service Hours");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TEXT_MAIN);
        JLabel sub = new JLabel("Submit and track your volunteer service hours.");
        sub.setFont(UITheme.FONT_REGULAR);
        sub.setForeground(UITheme.TEXT_MUTED);
        titleGrp.add(title);
        titleGrp.add(Box.createVerticalStrut(4));
        titleGrp.add(sub);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);
        totalHoursLbl = new JLabel("Total Verified: 0.0 hrs");
        totalHoursLbl.setFont(UITheme.FONT_BOLD);
        totalHoursLbl.setForeground(UITheme.SUCCESS);
        statusLabel = new JLabel("");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);
        JButton refreshBtn = UITheme.createSecondaryButton("🔄");
        refreshBtn.addActionListener(e -> loadHoursAsync());
        rightActions.add(totalHoursLbl);
        rightActions.add(statusLabel);
        rightActions.add(refreshBtn);

        top.add(titleGrp, BorderLayout.WEST);
        top.add(rightActions, BorderLayout.EAST);

        String[] cols = {"ID", "Opportunity", "Date", "Hours", "Status", "Verified By"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new ModernTable(tableModel);
        table.setColumnHidden(0);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(130);
        table.setStatusColumnRenderer(4, val -> {
            String s = String.valueOf(val);
            return switch (s) {
                case "APPROVED" -> UITheme.SUCCESS;
                case "REJECTED" -> UITheme.DANGER;
                default -> UITheme.WARNING;
            };
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        leftPanel.add(top, BorderLayout.NORTH);
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        // ---- Right: Submit Form ----
        JPanel rightPanel = buildSubmitForm();
        rightPanel.setPreferredSize(new Dimension(320, 0));

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(leftPanel, BorderLayout.CENTER);
        center.add(rightPanel, BorderLayout.EAST);

        add(center, BorderLayout.CENTER);
    }

    private JPanel buildSubmitForm() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;

        int row = 0;

        JLabel formTitle = new JLabel("Log Service Hours");
        formTitle.setFont(UITheme.FONT_SUBHEADER);
        formTitle.setForeground(UITheme.TEXT_MAIN);
        gbc.gridx = 0; gbc.gridy = row++;
        card.add(formTitle, gbc);

        JLabel oppLabel = UITheme.createFieldLabel("Opportunity *");
        gbc.gridx = 0; gbc.gridy = row++;
        card.add(oppLabel, gbc);

        opportunityCombo = new JComboBox<>();
        opportunityCombo.setFont(UITheme.FONT_REGULAR);
        gbc.gridx = 0; gbc.gridy = row++;
        card.add(opportunityCombo, gbc);

        JLabel hoursLabel = UITheme.createFieldLabel("Hours Worked *");
        gbc.gridx = 0; gbc.gridy = row++;
        card.add(hoursLabel, gbc);

        hoursSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.5, 24.0, 0.5));
        hoursSpinner.setFont(UITheme.FONT_REGULAR);
        gbc.gridx = 0; gbc.gridy = row++;
        card.add(hoursSpinner, gbc);

        JLabel dateLabel = UITheme.createFieldLabel("Work Date *");
        gbc.gridx = 0; gbc.gridy = row++;
        card.add(dateLabel, gbc);

        dateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "MMM dd, yyyy");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setFont(UITheme.FONT_REGULAR);
        gbc.gridx = 0; gbc.gridy = row++;
        card.add(dateSpinner, gbc);

        JLabel notesLabel = UITheme.createFieldLabel("Description / Notes");
        gbc.gridx = 0; gbc.gridy = row++;
        card.add(notesLabel, gbc);

        notesArea = new JTextArea(4, 20);
        notesArea.setFont(UITheme.FONT_REGULAR);
        notesArea.setWrapStyleWord(true);
        notesArea.setLineWrap(true);
        notesArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setBorder(null);
        gbc.gridx = 0; gbc.gridy = row++;
        card.add(notesScroll, gbc);

        submitBtn = UITheme.createPrimaryButton("Submit Hours");
        submitBtn.setPreferredSize(new Dimension(Integer.MAX_VALUE, 38));
        submitBtn.addActionListener(e -> handleSubmitHours());
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        card.add(submitBtn, gbc);

        JLabel infoLbl = new JLabel("<html><small>Hours will be <b>pending verification</b> by<br>the host organization before being counted.</small></html>");
        infoLbl.setFont(UITheme.FONT_SMALL);
        infoLbl.setForeground(UITheme.TEXT_MUTED);
        gbc.gridx = 0; gbc.gridy = row++;
        card.add(infoLbl, gbc);

        // Glue
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        card.add(Box.createVerticalGlue(), gbc);

        return card;
    }

    private void handleSubmitHours() {
        if (opportunityCombo.getSelectedItem() == null || opportunityCombo.getItemCount() == 0) {
            ToastNotification.showError(this, "Please select an opportunity first.");
            return;
        }
        int selectedIdx = opportunityCombo.getSelectedIndex();
        if (selectedIdx < 0 || selectedIdx >= completedSignups.size()) {
            ToastNotification.showError(this, "Invalid opportunity selection.");
            return;
        }
        Signup signup = completedSignups.get(selectedIdx);

        double hours = (double) hoursSpinner.getValue();
        Date spinnerDate = (Date) dateSpinner.getValue();
        LocalDate workDate = spinnerDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        String notes = notesArea.getText().trim();

        if (workDate.isAfter(LocalDate.now())) {
            ToastNotification.showError(this, "Work date cannot be in the future.");
            return;
        }

        submitBtn.setEnabled(false);
        submitBtn.setText("Submitting...");

        new SwingWorker<Void, Void>() {
            String errMsg = null;

            @Override
            protected Void doInBackground() {
                try {
                    VolunteerHours record = new VolunteerHours();
                    record.setVolunteerId(volunteerUser.getId());
                    record.setOpportunityId(signup.getOpportunityId());
                    record.setHoursWorked(hours);
                    record.setWorkDate(workDate);
                    record.setDescription(notes.isBlank() ? null : notes);
                    record.setVerificationStatus(VerificationStatus.PENDING);
                    hoursService.submitHours(volunteerUser, record);
                } catch (Exception ex) {
                    errMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                submitBtn.setEnabled(true);
                submitBtn.setText("Submit Hours");
                if (errMsg != null) {
                    ToastNotification.showError(VolHoursPanel.this, "Submission failed: " + errMsg);
                } else {
                    ToastNotification.showSuccess(VolHoursPanel.this, "Hours submitted successfully! Awaiting verification.");
                    notesArea.setText("");
                    loadHoursAsync();
                }
            }
        }.execute();
    }

    public void loadHoursAsync() {
        statusLabel.setText("Loading...");
        tableModel.setRowCount(0);
        currentHours.clear();
        opportunityCombo.removeAllItems();
        completedSignups.clear();

        new SwingWorker<Void, Void>() {
            List<VolunteerHours> hours;
            List<Signup> signups;
            double totalVerified = 0.0;
            String errMsg = null;

            @Override
            protected Void doInBackground() {
                try {
                    hours = hoursService.getHoursByVolunteer(volunteerUser, volunteerUser.getId());
                    signups = signupService.getSignupsByVolunteer(volunteerUser, volunteerUser.getId());
                    totalVerified = hoursService.getTotalVerifiedHours(volunteerUser.getId());
                } catch (Exception ex) {
                    errMsg = ex.getMessage();
                    hours = new ArrayList<>();
                    signups = new ArrayList<>();
                }
                return null;
            }

            @Override
            protected void done() {
                if (errMsg != null) {
                    statusLabel.setText("Error loading");
                    ToastNotification.showError(VolHoursPanel.this, "Failed to load hours: " + errMsg);
                    return;
                }
                currentHours.addAll(hours);
                for (VolunteerHours h : hours) {
                    tableModel.addRow(new Object[]{
                            h.getId(),
                            h.getOpportunityTitle() != null ? h.getOpportunityTitle() : "Opportunity #" + h.getOpportunityId(),
                            h.getFormattedWorkDate(),
                            String.format("%.1f", h.getHoursWorked()),
                            h.getVerificationStatus().name(),
                            h.getVerifierOrgName() != null ? h.getVerifierOrgName() : "—"
                    });
                }
                totalHoursLbl.setText(String.format("Total Verified: %.1f hrs", totalVerified));
                statusLabel.setText(hours.size() + " record(s)");

                // Populate combo with attended/completed signups
                for (Signup s : signups) {
                    if (s.getRegistrationStatus() == RegistrationStatus.ATTENDED
                            || s.getRegistrationStatus() == RegistrationStatus.COMPLETED
                            || s.getRegistrationStatus() == RegistrationStatus.REGISTERED) {
                        completedSignups.add(s);
                        String label = (s.getOpportunityTitle() != null ? s.getOpportunityTitle() : "Opportunity #" + s.getOpportunityId())
                                + (s.getEventDate() != null ? "  (" + s.getEventDate() + ")" : "");
                        opportunityCombo.addItem(label);
                    }
                }
                if (opportunityCombo.getItemCount() == 0) {
                    opportunityCombo.addItem("— No eligible sign-ups —");
                    submitBtn.setEnabled(false);
                } else {
                    submitBtn.setEnabled(true);
                }
            }
        }.execute();
    }
}
