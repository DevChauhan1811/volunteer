package com.volunteer.view.organization;

import com.volunteer.model.Organization;
import com.volunteer.model.VolunteerHours;
import com.volunteer.model.enums.VerificationStatus;
import com.volunteer.service.VolunteerHoursService;
import com.volunteer.service.impl.VolunteerHoursServiceImpl;
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
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for organizations to review, verify, or reject volunteer service hours submitted
 * for their hosted opportunities.
 */
public class OrgHoursVerificationPanel extends JPanel {

    private final Organization orgUser;
    private final VolunteerHoursService hoursService;

    private JComboBox<String> filterCombo;
    private ModernTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;

    private JLabel detailVolunteerLbl;
    private JLabel detailOppLbl;
    private JLabel detailHoursLbl;
    private JTextArea detailDescArea;

    private final List<VolunteerHours> currentList = new ArrayList<>();

    public OrgHoursVerificationPanel(Organization org) {
        this.orgUser = org;
        this.hoursService = new VolunteerHoursServiceImpl();
        initializeUI();
        loadHoursAsync();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header & Filter
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel titleGrp = new JPanel();
        titleGrp.setLayout(new BoxLayout(titleGrp, BoxLayout.Y_AXIS));
        titleGrp.setOpaque(false);

        JLabel title = new JLabel("Volunteer Hours Verification");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel sub = new JLabel("Review submitted volunteer work logs and officially approve service hours.");
        sub.setFont(UITheme.FONT_REGULAR);
        sub.setForeground(UITheme.TEXT_MUTED);

        titleGrp.add(title);
        titleGrp.add(Box.createVerticalStrut(4));
        titleGrp.add(sub);
        top.add(titleGrp, BorderLayout.WEST);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterRow.setOpaque(false);
        filterRow.add(new JLabel("Status:"));

        filterCombo = new JComboBox<>(new String[]{"PENDING", "All Hours"});
        filterCombo.setFont(UITheme.FONT_REGULAR);
        filterCombo.addActionListener(e -> loadHoursAsync());
        filterRow.add(filterCombo);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadHoursAsync());
        filterRow.add(refreshBtn);

        top.add(filterRow, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // Center Split: Table + Detail Box
        String[] cols = {"Record ID", "Volunteer Name", "Opportunity Title", "Hours Worked", "Work Date", "Status"};
        tableModel = new DefaultTableModel(cols, 0);
        table = new ModernTable(tableModel);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateInspector();
            }
        });

        JScrollPane tableScroll = table.wrapInScrollPane();
        tableScroll.setPreferredSize(new Dimension(800, 280));

        JPanel inspector = createInspectorCard();

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, inspector);
        split.setResizeWeight(0.60);
        split.setBorder(null);
        split.setDividerSize(8);

        JPanel centerPnl = new JPanel(new BorderLayout());
        centerPnl.setOpaque(false);
        centerPnl.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));
        centerPnl.add(split, BorderLayout.CENTER);
        add(centerPnl, BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);

        statusLabel = new JLabel("Loading hours...");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton approveBtn = UITheme.createSuccessButton("✓ Approve & Verify Hours");
        approveBtn.addActionListener(e -> handleDecision(VerificationStatus.APPROVED));

        JButton rejectBtn = UITheme.createDangerButton("✕ Reject Log");
        rejectBtn.addActionListener(e -> handleDecision(VerificationStatus.REJECTED));

        actions.add(approveBtn);
        actions.add(rejectBtn);

        bottom.add(statusLabel, BorderLayout.WEST);
        bottom.add(actions, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    private JPanel createInspectorCard() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(16, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        detailVolunteerLbl = new JLabel("Select an hour record to review volunteer submission");
        detailVolunteerLbl.setFont(UITheme.FONT_SUBHEADER);
        detailVolunteerLbl.setForeground(UITheme.TEXT_MAIN);

        detailHoursLbl = new JLabel("");
        detailHoursLbl.setFont(UITheme.FONT_HEADER);
        detailHoursLbl.setForeground(UITheme.PRIMARY);

        header.add(detailVolunteerLbl, BorderLayout.WEST);
        header.add(detailHoursLbl, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JPanel mid = new JPanel();
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.setOpaque(false);

        detailOppLbl = new JLabel("");
        detailOppLbl.setFont(UITheme.FONT_REGULAR);
        detailOppLbl.setForeground(UITheme.TEXT_MUTED);

        mid.add(detailOppLbl);
        mid.add(Box.createVerticalStrut(8));

        detailDescArea = new JTextArea(3, 30);
        detailDescArea.setFont(UITheme.FONT_REGULAR);
        detailDescArea.setLineWrap(true);
        detailDescArea.setWrapStyleWord(true);
        detailDescArea.setEditable(false);
        detailDescArea.setBackground(new Color(248, 250, 252));
        detailDescArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER, 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        JScrollPane descScroll = new JScrollPane(detailDescArea);
        descScroll.setBorder(null);
        mid.add(descScroll);

        card.add(mid, BorderLayout.CENTER);
        return card;
    }

    private void updateInspector() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentList.size()) {
            detailVolunteerLbl.setText("Select an hour record to review volunteer submission");
            detailHoursLbl.setText("");
            detailOppLbl.setText("");
            detailDescArea.setText("");
            return;
        }

        VolunteerHours vh = currentList.get(row);
        detailVolunteerLbl.setText("Volunteer: " + vh.getVolunteerName());
        detailHoursLbl.setText(String.format("%.1f Hours", vh.getHoursWorked()));
        detailOppLbl.setText("Event: " + vh.getOpportunityTitle() + " • Worked on: " + vh.getFormattedWorkDate());
        detailDescArea.setText(vh.getDescription());
    }

    public void loadHoursAsync() {
        statusLabel.setText("Fetching hour submissions...");

        SwingWorker<List<VolunteerHours>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<VolunteerHours> doInBackground() throws Exception {
                return hoursService.getPendingHoursByOrganization(orgUser, orgUser.getOrganizationId());
            }

            @Override
            protected void done() {
                try {
                    List<VolunteerHours> list = get();
                    currentList.clear();
                    currentList.addAll(list);

                    tableModel.setRowCount(0);
                    for (VolunteerHours vh : list) {
                        tableModel.addRow(new Object[]{
                            vh.getId(),
                            vh.getVolunteerName(),
                            vh.getOpportunityTitle(),
                            String.format("%.1f", vh.getHoursWorked()),
                            vh.getFormattedWorkDate(),
                            vh.getVerificationStatus().name()
                        });
                    }

                    statusLabel.setText("Found " + list.size() + " submitted hour records.");
                    if (!list.isEmpty()) {
                        table.setRowSelectionInterval(0, 0);
                    } else {
                        updateInspector();
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Failed loading hours.");
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(OrgHoursVerificationPanel.this, cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void handleDecision(VerificationStatus status) {
        int row = table.getSelectedRow();
        if (row < 0) {
            ToastNotification.showWarning(this, "Please select an hour record from the table.");
            return;
        }

        VolunteerHours selected = currentList.get(row);
        String actionStr = status == VerificationStatus.APPROVED ? "approve" : "reject";

        if (!ToastNotification.confirm(this,
            "Are you sure you want to " + actionStr + " " + selected.getHoursWorked() +
            " hours logged by " + selected.getVolunteerName() + "?", "Confirm Verification")) {
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                hoursService.verifyHours(orgUser, selected.getId(), status);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ToastNotification.showSuccess(OrgHoursVerificationPanel.this,
                        "Hours successfully " + status.name().toLowerCase() + " for " + selected.getVolunteerName());
                    loadHoursAsync();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(OrgHoursVerificationPanel.this, cause.getMessage());
                }
            }
        };
        worker.execute();
    }
}
