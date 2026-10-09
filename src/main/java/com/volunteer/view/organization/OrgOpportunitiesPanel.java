package com.volunteer.view.organization;

import com.volunteer.model.Opportunity;
import com.volunteer.model.Organization;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for organizations to manage their own opportunity listings,
 * post new causes, edit existing ones, and view moderation status feedback.
 */
public class OrgOpportunitiesPanel extends JPanel {

    private final Organization orgUser;
    private final OpportunityService oppService;

    private ModernTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;

    private JLabel detailTitleLbl;
    private JLabel detailStatusLbl;
    private JLabel detailScheduleLbl;
    private JLabel detailCapacityLbl;
    private JTextArea detailDescArea;
    private JLabel detailFeedbackLbl;

    private final List<Opportunity> currentList = new ArrayList<>();

    public OrgOpportunitiesPanel(Organization org) {
        this.orgUser = org;
        this.oppService = new OpportunityServiceImpl();
        initializeUI();
        loadOpportunitiesAsync();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Top Row: Title + Action
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel titleGrp = new JPanel();
        titleGrp.setLayout(new BoxLayout(titleGrp, BoxLayout.Y_AXIS));
        titleGrp.setOpaque(false);

        JLabel title = new JLabel("My Volunteer Opportunities");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel sub = new JLabel("Create and manage your organization's volunteer initiatives.");
        sub.setFont(UITheme.FONT_REGULAR);
        sub.setForeground(UITheme.TEXT_MUTED);

        titleGrp.add(title);
        titleGrp.add(Box.createVerticalStrut(4));
        titleGrp.add(sub);
        top.add(titleGrp, BorderLayout.WEST);

        JButton postBtn = UITheme.createPrimaryButton("+ Post Opportunity");
        postBtn.addActionListener(e -> openCreateDialog(null));
        top.add(postBtn, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        // Center Split: Table + Detail Inspector
        String[] cols = {"ID", "Title", "Category", "Event Date", "Time", "Location", "Capacity", "Signed Up", "Approval Status"};
        tableModel = new DefaultTableModel(cols, 0);
        table = new ModernTable(tableModel);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateInspector();
            }
        });

        JScrollPane tableScroll = table.wrapInScrollPane();
        tableScroll.setPreferredSize(new Dimension(800, 300));

        JPanel inspector = createInspectorCard();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, inspector);
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

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);

        JButton editBtn = UITheme.createSecondaryButton("Edit Listing");
        editBtn.addActionListener(e -> handleEdit());

        JButton cancelBtn = UITheme.createDangerButton("Cancel / Close Listing");
        cancelBtn.addActionListener(e -> handleCancelListing());

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadOpportunitiesAsync());

        btnRow.add(editBtn);
        btnRow.add(cancelBtn);
        btnRow.add(refreshBtn);

        bottom.add(statusLabel, BorderLayout.WEST);
        bottom.add(btnRow, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    private JPanel createInspectorCard() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(16, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        detailTitleLbl = new JLabel("Select an opportunity to view full details");
        detailTitleLbl.setFont(UITheme.FONT_SUBHEADER);
        detailTitleLbl.setForeground(UITheme.TEXT_MAIN);

        detailStatusLbl = new JLabel("");
        detailStatusLbl.setFont(UITheme.FONT_BOLD);

        header.add(detailTitleLbl, BorderLayout.WEST);
        header.add(detailStatusLbl, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JPanel mid = new JPanel();
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.setOpaque(false);

        detailScheduleLbl = new JLabel("");
        detailScheduleLbl.setFont(UITheme.FONT_REGULAR);
        detailScheduleLbl.setForeground(UITheme.TEXT_MUTED);

        detailCapacityLbl = new JLabel("");
        detailCapacityLbl.setFont(UITheme.FONT_REGULAR);
        detailCapacityLbl.setForeground(UITheme.TEXT_MUTED);

        detailFeedbackLbl = new JLabel("");
        detailFeedbackLbl.setFont(UITheme.FONT_BOLD);
        detailFeedbackLbl.setForeground(UITheme.DANGER);

        mid.add(detailScheduleLbl);
        mid.add(Box.createVerticalStrut(4));
        mid.add(detailCapacityLbl);
        mid.add(Box.createVerticalStrut(4));
        mid.add(detailFeedbackLbl);
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
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentList.size()) {
            detailTitleLbl.setText("Select an opportunity to view full details");
            detailStatusLbl.setText("");
            detailScheduleLbl.setText("");
            detailCapacityLbl.setText("");
            detailDescArea.setText("");
            detailFeedbackLbl.setText("");
            return;
        }

        Opportunity opp = currentList.get(row);
        detailTitleLbl.setText(opp.getTitle() + " (" + opp.getCategory() + ")");
        detailStatusLbl.setText("Status: " + opp.getApprovalStatus().name());

        if (opp.getApprovalStatus() == ApprovalStatus.APPROVED) {
            detailStatusLbl.setForeground(UITheme.SUCCESS);
        } else if (opp.getApprovalStatus() == ApprovalStatus.PENDING) {
            detailStatusLbl.setForeground(UITheme.WARNING);
        } else {
            detailStatusLbl.setForeground(UITheme.DANGER);
        }

        detailScheduleLbl.setText("📅 " + opp.getFormattedDate() + " • ⏰ " + opp.getFormattedTimeRange() + " • 📍 " + opp.getLocation());
        detailCapacityLbl.setText("Capacity: " + opp.getCapacity() + " • Registered: " + opp.getCurrentSignups() + " • Available Slots: " + opp.getAvailableSlots());
        detailDescArea.setText(opp.getDescription());

        if (opp.getApprovalStatus() == ApprovalStatus.REJECTED && opp.getRejectionReason() != null) {
            detailFeedbackLbl.setText("Admin Feedback: " + opp.getRejectionReason());
        } else {
            detailFeedbackLbl.setText("");
        }
    }

    public void loadOpportunitiesAsync() {
        statusLabel.setText("Querying organization listings...");

        SwingWorker<List<Opportunity>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Opportunity> doInBackground() throws Exception {
                return oppService.getOpportunitiesByOrganization(orgUser, orgUser.getOrganizationId());
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
                            o.getCategory(),
                            o.getFormattedDate(),
                            o.getFormattedTimeRange(),
                            o.getLocation(),
                            o.getCapacity(),
                            o.getCurrentSignups(),
                            o.getApprovalStatus().name()
                        });
                    }

                    statusLabel.setText("Showing " + opps.size() + " opportunity listings.");
                    if (!opps.isEmpty()) {
                        table.setRowSelectionInterval(0, 0);
                    } else {
                        updateInspector();
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Failed loading opportunities.");
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(OrgOpportunitiesPanel.this, cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void handleEdit() {
        int row = table.getSelectedRow();
        if (row < 0) {
            ToastNotification.showWarning(this, "Please select an opportunity to edit.");
            return;
        }

        Opportunity selected = currentList.get(row);
        openCreateDialog(selected);
    }

    private void handleCancelListing() {
        int row = table.getSelectedRow();
        if (row < 0) {
            ToastNotification.showWarning(this, "Please select an opportunity to cancel.");
            return;
        }

        Opportunity selected = currentList.get(row);
        if (!ToastNotification.confirm(this,
            "Are you sure you want to cancel \"" + selected.getTitle() + "\"?", "Confirm Cancellation")) {
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                oppService.cancelOpportunity(orgUser, selected.getId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ToastNotification.showSuccess(OrgOpportunitiesPanel.this, "Listing cancelled successfully.");
                    loadOpportunitiesAsync();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(OrgOpportunitiesPanel.this, cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void openCreateDialog(Opportunity existing) {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        OpportunityCreateDialog dialog = new OpportunityCreateDialog(parent, orgUser, existing, this::loadOpportunitiesAsync);
        dialog.setVisible(true);
    }
}
