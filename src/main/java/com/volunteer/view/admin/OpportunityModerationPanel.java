package com.volunteer.view.admin;

import com.volunteer.model.Admin;
import com.volunteer.model.Opportunity;
import com.volunteer.model.enums.ApprovalStatus;
import com.volunteer.service.OpportunityService;
import com.volunteer.service.impl.OpportunityServiceImpl;
import com.volunteer.view.common.ModernTable;
import com.volunteer.view.common.ToastNotification;
import com.volunteer.view.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Moderation panel allowing administrators to review, approve, or reject
 * volunteer opportunities with mandatory rejection feedback.
 */
public class OpportunityModerationPanel extends JPanel {

    private final Admin adminUser;
    private final OpportunityService oppService;

    private JComboBox<String> statusFilterCombo;
    private ModernTable oppTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;

    // Detailed Inspector Components
    private JLabel detailTitleLbl;
    private JLabel detailOrgLbl;
    private JLabel detailScheduleLbl;
    private JLabel detailCapacityLbl;
    private JTextArea detailDescArea;
    private JLabel detailRejectionLbl;

    private final List<Opportunity> currentList = new ArrayList<>();

    public OpportunityModerationPanel(Admin admin) {
        this.adminUser = admin;
        this.oppService = new OpportunityServiceImpl();
        initializeUI();
        loadOpportunitiesAsync();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header and Filter
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel titleGrp = new JPanel();
        titleGrp.setLayout(new BoxLayout(titleGrp, BoxLayout.Y_AXIS));
        titleGrp.setOpaque(false);

        JLabel title = new JLabel("Volunteer Opportunity Moderation");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel sub = new JLabel("Review submissions, verify community safety, and moderate listings.");
        sub.setFont(UITheme.FONT_REGULAR);
        sub.setForeground(UITheme.TEXT_MUTED);

        titleGrp.add(title);
        titleGrp.add(Box.createVerticalStrut(4));
        titleGrp.add(sub);
        top.add(titleGrp, BorderLayout.WEST);

        // Filter Controls
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterRow.setOpaque(false);
        filterRow.add(new JLabel("Status Filter:"));

        statusFilterCombo = new JComboBox<>(new String[]{"PENDING", "APPROVED", "REJECTED", "All Statuses"});
        statusFilterCombo.setFont(UITheme.FONT_REGULAR);
        statusFilterCombo.addActionListener(e -> loadOpportunitiesAsync());
        filterRow.add(statusFilterCombo);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadOpportunitiesAsync());
        filterRow.add(refreshBtn);

        top.add(filterRow, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // Center Split: Table on top, Inspector on bottom
        String[] cols = {"ID", "Opportunity Title", "Organization", "Category", "Date", "Capacity", "Status"};
        tableModel = new DefaultTableModel(cols, 0);
        oppTable = new ModernTable(tableModel);

        oppTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateInspector();
            }
        });

        JScrollPane tableScroll = oppTable.wrapInScrollPane();
        tableScroll.setPreferredSize(new Dimension(800, 280));

        JPanel inspectorCard = createInspectorCard();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, inspectorCard);
        splitPane.setResizeWeight(0.55);
        splitPane.setBorder(null);
        splitPane.setDividerSize(8);

        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setOpaque(false);
        centerContainer.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));
        centerContainer.add(splitPane, BorderLayout.CENTER);

        add(centerContainer, BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);

        statusLabel = new JLabel("Loading listings...");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actionBtns.setOpaque(false);

        JButton approveBtn = UITheme.createSuccessButton("✓ Approve Opportunity");
        approveBtn.addActionListener(e -> handleApprove());

        JButton rejectBtn = UITheme.createDangerButton("✕ Reject Opportunity...");
        rejectBtn.addActionListener(e -> handleReject());

        actionBtns.add(approveBtn);
        actionBtns.add(rejectBtn);

        bottom.add(statusLabel, BorderLayout.WEST);
        bottom.add(actionBtns, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    private JPanel createInspectorCard() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(16, 12));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        detailTitleLbl = new JLabel("Select an opportunity to inspect full details");
        detailTitleLbl.setFont(UITheme.FONT_SUBHEADER);
        detailTitleLbl.setForeground(UITheme.TEXT_MAIN);

        detailOrgLbl = new JLabel("");
        detailOrgLbl.setFont(UITheme.FONT_BOLD);
        detailOrgLbl.setForeground(UITheme.PRIMARY);

        header.add(detailTitleLbl, BorderLayout.WEST);
        header.add(detailOrgLbl, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Middle details
        JPanel mid = new JPanel();
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.setOpaque(false);

        detailScheduleLbl = new JLabel("");
        detailScheduleLbl.setFont(UITheme.FONT_REGULAR);
        detailScheduleLbl.setForeground(UITheme.TEXT_MUTED);

        detailCapacityLbl = new JLabel("");
        detailCapacityLbl.setFont(UITheme.FONT_REGULAR);
        detailCapacityLbl.setForeground(UITheme.TEXT_MUTED);

        detailRejectionLbl = new JLabel("");
        detailRejectionLbl.setFont(UITheme.FONT_BOLD);
        detailRejectionLbl.setForeground(UITheme.DANGER);

        mid.add(detailScheduleLbl);
        mid.add(Box.createVerticalStrut(4));
        mid.add(detailCapacityLbl);
        mid.add(Box.createVerticalStrut(4));
        mid.add(detailRejectionLbl);
        mid.add(Box.createVerticalStrut(8));

        detailDescArea = new JTextArea(4, 30);
        detailDescArea.setFont(UITheme.FONT_REGULAR);
        detailDescArea.setLineWrap(true);
        detailDescArea.setWrapStyleWord(true);
        detailDescArea.setEditable(false);
        detailDescArea.setBackground(new Color(248, 250, 252));
        detailDescArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER, 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JScrollPane descScroll = new JScrollPane(detailDescArea);
        descScroll.setBorder(null);
        mid.add(descScroll);

        card.add(mid, BorderLayout.CENTER);
        return card;
    }

    private void updateInspector() {
        int row = oppTable.getSelectedRow();
        if (row < 0 || row >= currentList.size()) {
            detailTitleLbl.setText("Select an opportunity to inspect full details");
            detailOrgLbl.setText("");
            detailScheduleLbl.setText("");
            detailCapacityLbl.setText("");
            detailDescArea.setText("");
            detailRejectionLbl.setText("");
            return;
        }

        Opportunity opp = currentList.get(row);
        detailTitleLbl.setText(opp.getTitle() + " (" + opp.getCategory() + ")");
        detailOrgLbl.setText("Host: " + opp.getOrganizationName());
        detailScheduleLbl.setText("📅 " + opp.getFormattedDate() + " • ⏰ " + opp.getFormattedTimeRange() + " • 📍 " + opp.getLocation());
        detailCapacityLbl.setText("Capacity: " + opp.getCapacity() + " volunteers (Signed up: " + opp.getCurrentSignups() + ")");
        detailDescArea.setText(opp.getDescription());

        if (opp.getApprovalStatus() == ApprovalStatus.REJECTED && opp.getRejectionReason() != null) {
            detailRejectionLbl.setText("Rejection Feedback: " + opp.getRejectionReason());
        } else {
            detailRejectionLbl.setText("");
        }
    }

    public void loadOpportunitiesAsync() {
        statusLabel.setText("Querying opportunities...");
        String filterStr = (String) statusFilterCombo.getSelectedItem();
        ApprovalStatus statusFilter = "All Statuses".equals(filterStr) ? null : ApprovalStatus.valueOf(filterStr);

        SwingWorker<List<Opportunity>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Opportunity> doInBackground() throws Exception {
                return oppService.getAllOpportunitiesForAdmin(adminUser, statusFilter);
            }

            @Override
            protected void done() {
                try {
                    List<Opportunity> opps = get();
                    currentList.clear();
                    currentList.addAll(opps);

                    tableModel.setRowCount(0);
                    for (Opportunity o : opps) {
                        tableModel.addRow(new Object[]{
                            o.getId(),
                            o.getTitle(),
                            o.getOrganizationName(),
                            o.getCategory(),
                            o.getFormattedDate(),
                            o.getCurrentSignups() + " / " + o.getCapacity(),
                            o.getApprovalStatus().name()
                        });
                    }

                    statusLabel.setText("Loaded " + opps.size() + " listings.");
                    if (!opps.isEmpty()) {
                        oppTable.setRowSelectionInterval(0, 0);
                    } else {
                        updateInspector();
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Failed loading opportunities.");
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(OpportunityModerationPanel.this, "Error fetching listings: " + cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void handleApprove() {
        int row = oppTable.getSelectedRow();
        if (row < 0) {
            ToastNotification.showWarning(this, "Please select an opportunity from the table to approve.");
            return;
        }

        Opportunity selected = currentList.get(row);
        if (selected.getApprovalStatus() == ApprovalStatus.APPROVED) {
            ToastNotification.showWarning(this, "This opportunity is already approved.");
            return;
        }

        if (!ToastNotification.confirm(this,
            "Approve \"" + selected.getTitle() + "\" for volunteer registration?", "Confirm Approval")) {
            return;
        }

        executeModeration(selected.getId(), ApprovalStatus.APPROVED, null, "Opportunity approved successfully!");
    }

    private void handleReject() {
        int row = oppTable.getSelectedRow();
        if (row < 0) {
            ToastNotification.showWarning(this, "Please select an opportunity from the table to reject.");
            return;
        }

        Opportunity selected = currentList.get(row);

        String reason = JOptionPane.showInputDialog(
            this,
            "Enter rejection feedback for the organization (Required):",
            "Reject Opportunity",
            JOptionPane.PLAIN_MESSAGE
        );

        if (reason == null) return; // cancelled
        if (reason.trim().isEmpty()) {
            ToastNotification.showError(this, "A rejection reason is required.");
            return;
        }

        executeModeration(selected.getId(), ApprovalStatus.REJECTED, reason.trim(), "Opportunity rejected and feedback recorded.");
    }

    private void executeModeration(Long oppId, ApprovalStatus newStatus, String reason, String successMsg) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                oppService.moderateOpportunity(adminUser, oppId, newStatus, reason);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ToastNotification.showSuccess(OpportunityModerationPanel.this, successMsg);
                    loadOpportunitiesAsync();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(OpportunityModerationPanel.this, cause.getMessage());
                }
            }
        };
        worker.execute();
    }
}
