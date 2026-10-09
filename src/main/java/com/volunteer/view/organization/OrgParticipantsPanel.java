package com.volunteer.view.organization;

import com.volunteer.model.Opportunity;
import com.volunteer.model.Organization;
import com.volunteer.model.Signup;
import com.volunteer.model.enums.RegistrationStatus;
import com.volunteer.service.OpportunityService;
import com.volunteer.service.SignupService;
import com.volunteer.service.impl.OpportunityServiceImpl;
import com.volunteer.service.impl.SignupServiceImpl;
import com.volunteer.view.common.ModernTable;
import com.volunteer.view.common.ToastNotification;
import com.volunteer.view.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Participant Roster tracking panel allowing organizations to view registered volunteers,
 * monitor participation metrics, and mark attendance status (ATTENDED, NO_SHOW, COMPLETED).
 */
public class OrgParticipantsPanel extends JPanel {

    private final Organization orgUser;
    private final SignupService signupService;
    private final OpportunityService oppService;

    private JComboBox<String> oppFilterCombo;
    private ModernTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JLabel metricsLabel;

    private final List<Opportunity> myOpps = new ArrayList<>();
    private final List<Signup> currentSignups = new ArrayList<>();

    public OrgParticipantsPanel(Organization org) {
        this.orgUser = org;
        this.signupService = new SignupServiceImpl();
        this.oppService = new OpportunityServiceImpl();
        initializeUI();
        loadOpportunitiesFilterAsync();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header & Filter Bar
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel titleGrp = new JPanel();
        titleGrp.setLayout(new BoxLayout(titleGrp, BoxLayout.Y_AXIS));
        titleGrp.setOpaque(false);

        JLabel title = new JLabel("Volunteer Participant Roster");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel sub = new JLabel("Track registrations, manage attendee check-ins, and record completion.");
        sub.setFont(UITheme.FONT_REGULAR);
        sub.setForeground(UITheme.TEXT_MUTED);

        titleGrp.add(title);
        titleGrp.add(Box.createVerticalStrut(4));
        titleGrp.add(sub);
        top.add(titleGrp, BorderLayout.WEST);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterRow.setOpaque(false);
        filterRow.add(new JLabel("Filter Event:"));

        oppFilterCombo = new JComboBox<>(new String[]{"All My Opportunities"});
        oppFilterCombo.setFont(UITheme.FONT_REGULAR);
        oppFilterCombo.addActionListener(e -> loadSignupsAsync());
        filterRow.add(oppFilterCombo);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> {
            loadOpportunitiesFilterAsync();
        });
        filterRow.add(refreshBtn);

        top.add(filterRow, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // Center Table
        String[] cols = {"Signup ID", "Volunteer Name", "Email Address", "Opportunity Title", "Event Date", "Status", "Registered At"};
        tableModel = new DefaultTableModel(cols, 0);
        table = new ModernTable(tableModel);

        JScrollPane scroll = table.wrapInScrollPane();
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));
        center.add(scroll, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // Bottom Actions & Summary
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);

        JPanel leftStats = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftStats.setOpaque(false);
        statusLabel = new JLabel("Loading participants...");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);

        metricsLabel = new JLabel("");
        metricsLabel.setFont(UITheme.FONT_BOLD);
        metricsLabel.setForeground(UITheme.PRIMARY);

        leftStats.add(statusLabel);
        leftStats.add(Box.createHorizontalStrut(16));
        leftStats.add(metricsLabel);
        bottom.add(leftStats, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton markAttendedBtn = UITheme.createSuccessButton("✓ Check-in / Attended");
        markAttendedBtn.addActionListener(e -> updateStatus(RegistrationStatus.ATTENDED));

        JButton markCompletedBtn = UITheme.createPrimaryButton("★ Mark Completed");
        markCompletedBtn.addActionListener(e -> updateStatus(RegistrationStatus.COMPLETED));

        JButton markNoShowBtn = UITheme.createDangerButton("✕ Mark No-Show");
        markNoShowBtn.addActionListener(e -> updateStatus(RegistrationStatus.NO_SHOW));

        actions.add(markAttendedBtn);
        actions.add(markCompletedBtn);
        actions.add(markNoShowBtn);

        bottom.add(actions, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    private void loadOpportunitiesFilterAsync() {
        SwingWorker<List<Opportunity>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Opportunity> doInBackground() throws Exception {
                return oppService.getOpportunitiesByOrganization(orgUser, orgUser.getOrganizationId());
            }

            @Override
            protected void done() {
                try {
                    List<Opportunity> opps = get();
                    myOpps.clear();
                    myOpps.addAll(opps);

                    oppFilterCombo.removeAllItems();
                    oppFilterCombo.addItem("All My Opportunities");
                    for (Opportunity o : opps) {
                        oppFilterCombo.addItem("#" + o.getId() + " " + o.getTitle());
                    }
                    loadSignupsAsync();
                } catch (Exception ex) {
                    ToastNotification.showError(OrgParticipantsPanel.this, "Failed loading filter options.");
                }
            }
        };
        worker.execute();
    }

    public void loadSignupsAsync() {
        statusLabel.setText("Fetching roster records...");
        int selectedIndex = oppFilterCombo.getSelectedIndex();
        Long filterOppId = null;
        if (selectedIndex > 0 && selectedIndex - 1 < myOpps.size()) {
            filterOppId = myOpps.get(selectedIndex - 1).getId();
        }

        final Long targetOppId = filterOppId;
        SwingWorker<List<Signup>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Signup> doInBackground() throws Exception {
                if (targetOppId != null) {
                    return signupService.getSignupsByOpportunity(orgUser, targetOppId);
                } else {
                    List<Signup> all = new ArrayList<>();
                    for (Opportunity o : myOpps) {
                        all.addAll(signupService.getSignupsByOpportunity(orgUser, o.getId()));
                    }
                    return all;
                }
            }

            @Override
            protected void done() {
                try {
                    List<Signup> signups = get();
                    currentSignups.clear();
                    currentSignups.addAll(signups);

                    tableModel.setRowCount(0);
                    int registered = 0;
                    int attended = 0;
                    int completed = 0;
                    int noShow = 0;

                    for (Signup s : signups) {
                        if (s.getRegistrationStatus() == RegistrationStatus.REGISTERED) registered++;
                        else if (s.getRegistrationStatus() == RegistrationStatus.ATTENDED) attended++;
                        else if (s.getRegistrationStatus() == RegistrationStatus.COMPLETED) completed++;
                        else if (s.getRegistrationStatus() == RegistrationStatus.NO_SHOW) noShow++;

                        tableModel.addRow(new Object[]{
                            s.getId(),
                            s.getVolunteerName(),
                            s.getVolunteerEmail(),
                            s.getOpportunityTitle(),
                            s.getEventDate() != null ? s.getEventDate().toString() : "",
                            s.getRegistrationStatus().name(),
                            s.getFormattedRegisteredAt()
                        });
                    }

                    statusLabel.setText("Total participants: " + signups.size());
                    metricsLabel.setText(String.format("Registered: %d | Attended: %d | Completed: %d | No-Show: %d",
                        registered, attended, completed, noShow));

                } catch (Exception ex) {
                    statusLabel.setText("Failed loading roster.");
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(OrgParticipantsPanel.this, cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void updateStatus(RegistrationStatus newStatus) {
        int row = table.getSelectedRow();
        if (row < 0) {
            ToastNotification.showWarning(this, "Please select a participant row from the table.");
            return;
        }

        Signup selected = currentSignups.get(row);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                signupService.updateAttendanceStatus(orgUser, selected.getId(), newStatus);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ToastNotification.showSuccess(OrgParticipantsPanel.this,
                        "Updated status to " + newStatus.name() + " for " + selected.getVolunteerName());
                    loadSignupsAsync();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(OrgParticipantsPanel.this, cause.getMessage());
                }
            }
        };
        worker.execute();
    }
}
