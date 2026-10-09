package com.volunteer.view.volunteer;

import com.volunteer.model.Signup;
import com.volunteer.model.Volunteer;
import com.volunteer.model.enums.RegistrationStatus;
import com.volunteer.service.SignupService;
import com.volunteer.service.impl.SignupServiceImpl;
import com.volunteer.view.common.ModernTable;
import com.volunteer.view.common.ToastNotification;
import com.volunteer.view.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
 * Volunteer panel displaying all current registrations, with status badges and cancellation support.
 */
public class VolMySignupsPanel extends JPanel {

    private final Volunteer volunteerUser;
    private final SignupService signupService;

    private ModernTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JButton cancelBtn;

    private final List<Signup> currentList = new ArrayList<>();

    public VolMySignupsPanel(Volunteer volunteer) {
        this.volunteerUser = volunteer;
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
        JLabel title = new JLabel("My Sign-Ups");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TEXT_MAIN);
        JLabel sub = new JLabel("Track your registered opportunities and cancel if needed.");
        sub.setFont(UITheme.FONT_REGULAR);
        sub.setForeground(UITheme.TEXT_MUTED);
        titleGrp.add(title);
        titleGrp.add(Box.createVerticalStrut(4));
        titleGrp.add(sub);

        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsRow.setOpaque(false);

        JButton refreshBtn = UITheme.createSecondaryButton("🔄  Refresh");
        refreshBtn.addActionListener(e -> loadSignupsAsync());

        cancelBtn = UITheme.createDangerButton("❌  Cancel Registration");
        cancelBtn.setEnabled(false);
        cancelBtn.addActionListener(e -> handleCancelRegistration());

        statusLabel = new JLabel("Loading...");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);

        actionsRow.add(statusLabel);
        actionsRow.add(refreshBtn);
        actionsRow.add(cancelBtn);

        top.add(titleGrp, BorderLayout.WEST);
        top.add(actionsRow, BorderLayout.EAST);

        // ---- Table ----
        String[] cols = {"ID", "Opportunity", "Organization", "Event Date", "Location", "Status", "Registered On"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new ModernTable(tableModel);
        table.setColumnHidden(0);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(130);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        table.getColumnModel().getColumn(6).setPreferredWidth(130);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0 && row < currentList.size()) {
                    cancelBtn.setEnabled(currentList.get(row).canCancel());
                } else {
                    cancelBtn.setEnabled(false);
                }
            }
        });

        // Color-code status column renderer
        table.setStatusColumnRenderer(5, val -> {
            String s = String.valueOf(val);
            return switch (s) {
                case "REGISTERED" -> new Color(22, 163, 74);
                case "CANCELLED" -> new Color(220, 38, 38);
                case "ATTENDED", "COMPLETED" -> new Color(37, 99, 235);
                default -> UITheme.TEXT_MUTED;
            };
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        add(top, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // ---- Bottom Summary Card ----
        JPanel summary = buildSummaryPanel();
        add(summary, BorderLayout.SOUTH);
    }

    private JPanel buildSummaryPanel() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 24, 8));
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel infoLbl = new JLabel("<html><b>Tip:</b> You can only cancel registrations with REGISTERED status where the event has not yet passed.</html>");
        infoLbl.setFont(UITheme.FONT_SMALL);
        infoLbl.setForeground(UITheme.TEXT_MUTED);
        card.add(infoLbl);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    private void handleCancelRegistration() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentList.size()) return;
        Signup signup = currentList.get(row);

        if (!signup.canCancel()) {
            ToastNotification.showError(this, "This registration cannot be cancelled.");
            return;
        }
        if (!ToastNotification.confirm(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                "Cancel registration for: " + signup.getOpportunityTitle() + "?",
                "Confirm Cancellation")) {
            return;
        }

        cancelBtn.setEnabled(false);
        new SwingWorker<Void, Void>() {
            String errMsg = null;

            @Override
            protected Void doInBackground() {
                try {
                    signupService.cancelRegistration(volunteerUser, signup.getId());
                } catch (Exception ex) {
                    errMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (errMsg != null) {
                    ToastNotification.showError(VolMySignupsPanel.this, "Cancellation failed: " + errMsg);
                } else {
                    ToastNotification.showSuccess(VolMySignupsPanel.this,
                            "Registration cancelled for: " + signup.getOpportunityTitle());
                    loadSignupsAsync();
                }
            }
        }.execute();
    }

    public void loadSignupsAsync() {
        statusLabel.setText("Loading...");
        tableModel.setRowCount(0);
        currentList.clear();
        cancelBtn.setEnabled(false);

        new SwingWorker<List<Signup>, Void>() {
            @Override
            protected List<Signup> doInBackground() throws Exception {
                return signupService.getSignupsByVolunteer(volunteerUser, volunteerUser.getId());
            }

            @Override
            protected void done() {
                try {
                    List<Signup> list = get();
                    currentList.addAll(list);
                    for (Signup s : list) {
                        tableModel.addRow(new Object[]{
                                s.getId(),
                                s.getOpportunityTitle() != null ? s.getOpportunityTitle() : "N/A",
                                s.getOrganizationName() != null ? s.getOrganizationName() : "N/A",
                                s.getEventDate() != null ? s.getEventDate().toString() : "N/A",
                                s.getLocation() != null ? s.getLocation() : "N/A",
                                s.getRegistrationStatus().name(),
                                s.getFormattedRegisteredAt()
                        });
                    }
                    long active = list.stream()
                            .filter(s -> s.getRegistrationStatus() == RegistrationStatus.REGISTERED).count();
                    statusLabel.setText(list.size() + " total  |  " + active + " active");
                } catch (Exception ex) {
                    statusLabel.setText("Error loading sign-ups");
                    ToastNotification.showError(VolMySignupsPanel.this,
                            "Failed to load sign-ups: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
