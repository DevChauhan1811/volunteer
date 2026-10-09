package com.volunteer.view.organization;

import com.volunteer.model.Opportunity;
import com.volunteer.model.Organization;
import com.volunteer.model.enums.ApprovalStatus;
import com.volunteer.service.OpportunityService;
import com.volunteer.service.impl.OpportunityServiceImpl;
import com.volunteer.view.common.ToastNotification;
import com.volunteer.view.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Modal dialog for creating or updating a volunteer opportunity listing.
 */
public class OpportunityCreateDialog extends JDialog {

    private final Organization orgUser;
    private final Opportunity existingOpportunity; // null if creating new
    private final OpportunityService oppService;
    private final Runnable onSuccessCallback;

    private JTextField titleField;
    private JComboBox<String> categoryCombo;
    private JTextField dateField;       // YYYY-MM-DD
    private JTextField startTimeField;  // HH:MM
    private JTextField endTimeField;    // HH:MM
    private JTextField locationField;
    private JSpinner capacitySpinner;
    private JTextArea descArea;

    private JButton submitButton;

    public OpportunityCreateDialog(JFrame parent, Organization org, Opportunity existingOpp, Runnable onSuccessCallback) {
        super(parent, existingOpp == null ? "Post New Volunteer Opportunity" : "Edit Volunteer Opportunity", true);
        this.orgUser = org;
        this.existingOpportunity = existingOpp;
        this.oppService = new OpportunityServiceImpl();
        this.onSuccessCallback = onSuccessCallback;
        initializeUI();
    }

    private void initializeUI() {
        setSize(580, 720);
        setMinimumSize(new Dimension(520, 640));
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_MAIN);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel titleLbl = new JLabel(existingOpportunity == null ? "Post Community Opportunity" : "Update Opportunity Details");
        titleLbl.setFont(UITheme.FONT_HEADER);
        titleLbl.setForeground(UITheme.TEXT_MAIN);

        JLabel subLbl = new JLabel("All listings are submitted for administrative review before public display.");
        subLbl.setFont(UITheme.FONT_REGULAR);
        subLbl.setForeground(UITheme.TEXT_MUTED);

        header.add(titleLbl);
        header.add(Box.createVerticalStrut(4));
        header.add(subLbl);
        root.add(header, BorderLayout.NORTH);

        // Center Form
        JPanel form = UITheme.createCardPanel();
        form.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        int row = 0;
        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Opportunity Title *"), gbc);
        titleField = UITheme.createTextField(25);
        gbc.gridy = row++;
        form.add(titleField, gbc);

        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Category *"), gbc);
        categoryCombo = new JComboBox<>(new String[]{
            "Environment", "Community Relief", "Education", "Healthcare", "Animal Welfare", "Arts & Culture", "Conservation", "Humanitarian"
        });
        categoryCombo.setFont(UITheme.FONT_REGULAR);
        categoryCombo.setPreferredSize(new Dimension(categoryCombo.getPreferredSize().width, 36));
        gbc.gridy = row++;
        form.add(categoryCombo, gbc);

        // Schedule Grid Row (Date, Start Time, End Time)
        gbc.gridy = row++;
        JPanel scheduleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        scheduleRow.setOpaque(false);

        JPanel datePnl = new JPanel();
        datePnl.setLayout(new BoxLayout(datePnl, BoxLayout.Y_AXIS));
        datePnl.setOpaque(false);
        datePnl.add(UITheme.createFieldLabel("Date (YYYY-MM-DD) *"));
        dateField = UITheme.createTextField(10);
        dateField.setText(LocalDate.now().plusDays(7).toString());
        datePnl.add(dateField);

        JPanel startPnl = new JPanel();
        startPnl.setLayout(new BoxLayout(startPnl, BoxLayout.Y_AXIS));
        startPnl.setOpaque(false);
        startPnl.add(UITheme.createFieldLabel("Start (HH:MM 24h) *"));
        startTimeField = UITheme.createTextField(8);
        startTimeField.setText("09:00");
        startPnl.add(startTimeField);

        JPanel endPnl = new JPanel();
        endPnl.setLayout(new BoxLayout(endPnl, BoxLayout.Y_AXIS));
        endPnl.setOpaque(false);
        endPnl.add(UITheme.createFieldLabel("End (HH:MM 24h) *"));
        endTimeField = UITheme.createTextField(8);
        endTimeField.setText("13:00");
        endPnl.add(endTimeField);

        scheduleRow.add(datePnl);
        scheduleRow.add(startPnl);
        scheduleRow.add(endPnl);
        form.add(scheduleRow, gbc);

        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Event Location / Venue *"), gbc);
        locationField = UITheme.createTextField(25);
        gbc.gridy = row++;
        form.add(locationField, gbc);

        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Required Volunteer Capacity *"), gbc);
        capacitySpinner = new JSpinner(new SpinnerNumberModel(20, 1, 500, 1));
        capacitySpinner.setFont(UITheme.FONT_REGULAR);
        capacitySpinner.setPreferredSize(new Dimension(100, 36));
        gbc.gridy = row++;
        form.add(capacitySpinner, gbc);

        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Event Description & Volunteer Instructions *"), gbc);
        descArea = new JTextArea(4, 25);
        descArea.setFont(UITheme.FONT_REGULAR);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(descArea);
        gbc.gridy = row++;
        form.add(sp, gbc);

        root.add(form, BorderLayout.CENTER);

        // Prepopulate if editing
        if (existingOpportunity != null) {
            titleField.setText(existingOpportunity.getTitle());
            categoryCombo.setSelectedItem(existingOpportunity.getCategory());
            dateField.setText(existingOpportunity.getEventDate().toString());
            startTimeField.setText(existingOpportunity.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            endTimeField.setText(existingOpportunity.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            locationField.setText(existingOpportunity.getLocation());
            capacitySpinner.setValue(existingOpportunity.getCapacity());
            descArea.setText(existingOpportunity.getDescription());
        }

        // Actions Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setOpaque(false);

        JButton cancelBtn = UITheme.createSecondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        submitButton = UITheme.createPrimaryButton(existingOpportunity == null ? "Submit Opportunity" : "Save Changes");
        submitButton.addActionListener(e -> handleSubmit());

        footer.add(cancelBtn);
        footer.add(submitButton);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void handleSubmit() {
        String title = titleField.getText().trim();
        String cat = (String) categoryCombo.getSelectedItem();
        String dateStr = dateField.getText().trim();
        String startStr = startTimeField.getText().trim();
        String endStr = endTimeField.getText().trim();
        String loc = locationField.getText().trim();
        int cap = (int) capacitySpinner.getValue();
        String desc = descArea.getText().trim();

        LocalDate date;
        LocalTime start;
        LocalTime end;

        try {
            date = LocalDate.parse(dateStr);
        } catch (DateTimeParseException ex) {
            ToastNotification.showError(this, "Invalid date format. Use YYYY-MM-DD (e.g. 2026-11-15).");
            return;
        }

        try {
            start = LocalTime.parse(startStr);
            end = LocalTime.parse(endStr);
        } catch (DateTimeParseException ex) {
            ToastNotification.showError(this, "Invalid time format. Use HH:MM in 24-hour time (e.g. 09:00 or 14:30).");
            return;
        }

        submitButton.setEnabled(false);
        submitButton.setText("Submitting...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (existingOpportunity == null) {
                    Opportunity opp = new Opportunity();
                    opp.setOrganizationId(orgUser.getOrganizationId());
                    opp.setTitle(title);
                    opp.setCategory(cat);
                    opp.setEventDate(date);
                    opp.setStartTime(start);
                    opp.setEndTime(end);
                    opp.setLocation(loc);
                    opp.setCapacity(cap);
                    opp.setDescription(desc);
                    oppService.postOpportunity(orgUser, opp);
                } else {
                    existingOpportunity.setTitle(title);
                    existingOpportunity.setCategory(cat);
                    existingOpportunity.setEventDate(date);
                    existingOpportunity.setStartTime(start);
                    existingOpportunity.setEndTime(end);
                    existingOpportunity.setLocation(loc);
                    existingOpportunity.setCapacity(cap);
                    existingOpportunity.setDescription(desc);
                    oppService.updateOpportunity(orgUser, existingOpportunity);
                }
                return null;
            }

            @Override
            protected void done() {
                submitButton.setEnabled(true);
                submitButton.setText(existingOpportunity == null ? "Submit Opportunity" : "Save Changes");
                try {
                    get();
                    ToastNotification.showSuccess(OpportunityCreateDialog.this,
                        existingOpportunity == null ?
                            "Opportunity posted successfully! It is now pending administrator moderation." :
                            "Opportunity updated successfully!");
                    if (onSuccessCallback != null) onSuccessCallback.run();
                    dispose();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(OpportunityCreateDialog.this, cause.getMessage());
                }
            }
        };
        worker.execute();
    }
}
