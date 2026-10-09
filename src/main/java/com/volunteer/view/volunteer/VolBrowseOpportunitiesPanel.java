package com.volunteer.view.volunteer;

import com.volunteer.exception.CapacityExceededException;
import com.volunteer.exception.DuplicateResourceException;
import com.volunteer.model.Opportunity;
import com.volunteer.model.Volunteer;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
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
 * Volunteer panel for browsing all APPROVED opportunities and signing up for them.
 * Includes keyword/category/location search filters and a detail view pane.
 */
public class VolBrowseOpportunitiesPanel extends JPanel {

    private final Volunteer volunteerUser;
    private final OpportunityService oppService;
    private final SignupService signupService;

    private ModernTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JTextField keywordField;

    // Detail pane
    private JLabel detailTitleLbl;
    private JLabel detailOrgLbl;
    private JLabel detailScheduleLbl;
    private JLabel detailLocationLbl;
    private JLabel detailCapacityLbl;
    private JLabel detailCategoryLbl;
    private JTextArea detailDescArea;
    private JButton signUpBtn;

    private final List<Opportunity> currentList = new ArrayList<>();

    public VolBrowseOpportunitiesPanel(Volunteer volunteer) {
        this.volunteerUser = volunteer;
        this.oppService = new OpportunityServiceImpl();
        this.signupService = new SignupServiceImpl();
        initializeUI();
        loadOpportunitiesAsync(null);
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
        JLabel title = new JLabel("Browse Opportunities");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TEXT_MAIN);
        JLabel sub = new JLabel("Discover volunteer opportunities and sign up for causes that matter.");
        sub.setFont(UITheme.FONT_REGULAR);
        sub.setForeground(UITheme.TEXT_MUTED);
        titleGrp.add(title);
        titleGrp.add(Box.createVerticalStrut(4));
        titleGrp.add(sub);

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchRow.setOpaque(false);
        keywordField = UITheme.createTextField(20);
        keywordField.putClientProperty("JTextField.placeholderText", "Search keyword...");
        JButton searchBtn = UITheme.createPrimaryButton("🔍  Search");
        searchBtn.addActionListener(e -> loadOpportunitiesAsync(keywordField.getText().trim()));
        JButton clearBtn = UITheme.createSecondaryButton("Clear");
        clearBtn.addActionListener(e -> {
            keywordField.setText("");
            loadOpportunitiesAsync(null);
        });

        statusLabel = new JLabel("Loading...");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);

        searchRow.add(new JLabel("Search:"));
        searchRow.add(keywordField);
        searchRow.add(searchBtn);
        searchRow.add(clearBtn);
        searchRow.add(statusLabel);

        top.add(titleGrp, BorderLayout.WEST);
        top.add(searchRow, BorderLayout.EAST);

        // ---- Table ----
        String[] cols = {"ID", "Title", "Organization", "Category", "Date", "Location", "Slots Left"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new ModernTable(tableModel);
        table.setColumnHidden(0); // hide ID column
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(140);
        table.getColumnModel().getColumn(6).setPreferredWidth(80);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetail(table.getSelectedRow());
        });

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());

        // ---- Detail Pane ----
        JPanel detailPanel = buildDetailPanel();

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, detailPanel);
        split.setResizeWeight(0.60);
        split.setDividerSize(6);
        split.setBorder(null);
        split.setBackground(UITheme.BG_MAIN);

        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildDetailPanel() {
        JPanel panel = UITheme.createCardPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        detailTitleLbl = new JLabel("Select an opportunity above to see details.");
        detailTitleLbl.setFont(UITheme.FONT_SUBHEADER);
        detailTitleLbl.setForeground(UITheme.TEXT_MAIN);

        detailOrgLbl = makeMuted("");
        detailScheduleLbl = makeMuted("");
        detailLocationLbl = makeMuted("");
        detailCategoryLbl = makeMuted("");
        detailCapacityLbl = makeMuted("");

        detailDescArea = new JTextArea(3, 40);
        detailDescArea.setEditable(false);
        detailDescArea.setWrapStyleWord(true);
        detailDescArea.setLineWrap(true);
        detailDescArea.setFont(UITheme.FONT_REGULAR);
        detailDescArea.setForeground(UITheme.TEXT_MUTED);
        detailDescArea.setBackground(UITheme.BG_CARD);
        detailDescArea.setBorder(null);

        left.add(detailTitleLbl);
        left.add(Box.createVerticalStrut(6));
        left.add(detailOrgLbl);
        left.add(Box.createVerticalStrut(4));
        left.add(detailScheduleLbl);
        left.add(Box.createVerticalStrut(4));
        left.add(detailLocationLbl);
        left.add(Box.createVerticalStrut(4));
        left.add(detailCategoryLbl);
        left.add(Box.createVerticalStrut(4));
        left.add(detailCapacityLbl);
        left.add(Box.createVerticalStrut(8));
        left.add(detailDescArea);

        signUpBtn = UITheme.createSuccessButton("✅  Sign Up for This Opportunity");
        signUpBtn.setEnabled(false);
        signUpBtn.setPreferredSize(new Dimension(260, 36));
        signUpBtn.addActionListener(e -> handleSignUp());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(signUpBtn);

        panel.add(left, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.EAST);
        return panel;
    }

    private JLabel makeMuted(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_REGULAR);
        lbl.setForeground(UITheme.TEXT_MUTED);
        return lbl;
    }

    private void showDetail(int viewRow) {
        if (viewRow < 0 || viewRow >= currentList.size()) {
            detailTitleLbl.setText("Select an opportunity above to see details.");
            detailOrgLbl.setText("");
            detailScheduleLbl.setText("");
            detailLocationLbl.setText("");
            detailCategoryLbl.setText("");
            detailCapacityLbl.setText("");
            detailDescArea.setText("");
            signUpBtn.setEnabled(false);
            return;
        }
        Opportunity opp = currentList.get(viewRow);
        detailTitleLbl.setText(opp.getTitle());
        detailOrgLbl.setText("🏢  " + (opp.getOrganizationName() != null ? opp.getOrganizationName() : "N/A"));
        detailScheduleLbl.setText("📅  " + opp.getFormattedDate() + "  " + opp.getFormattedTimeRange());
        detailLocationLbl.setText("📍  " + (opp.getLocation() != null ? opp.getLocation() : "N/A"));
        detailCategoryLbl.setText("🏷  " + (opp.getCategory() != null ? opp.getCategory() : "General"));
        int slots = opp.getAvailableSlots();
        detailCapacityLbl.setText("👥  " + opp.getCurrentSignups() + "/" + opp.getCapacity() + " registered  |  " + slots + " slot(s) left");
        detailCapacityLbl.setForeground(slots == 0 ? UITheme.DANGER : UITheme.SUCCESS);
        detailDescArea.setText(opp.getDescription() != null ? opp.getDescription() : "");
        signUpBtn.setEnabled(!opp.isFull());
    }

    private void handleSignUp() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentList.size()) return;
        Opportunity opp = currentList.get(row);

        if (!ToastNotification.confirm(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                "Sign up for: " + opp.getTitle() + "?",
                "Confirm Sign-Up")) {
            return;
        }

        signUpBtn.setEnabled(false);
        signUpBtn.setText("Signing up...");

        new SwingWorker<Void, Void>() {
            String errorMsg = null;

            @Override
            protected Void doInBackground() {
                try {
                    signupService.registerForOpportunity(volunteerUser, opp.getId());
                } catch (DuplicateResourceException ex) {
                    errorMsg = "You are already registered for this opportunity.";
                } catch (CapacityExceededException ex) {
                    errorMsg = "Sorry, this opportunity is now full.";
                } catch (Exception ex) {
                    errorMsg = "Sign-up failed: " + ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                signUpBtn.setText("✅  Sign Up for This Opportunity");
                if (errorMsg != null) {
                    ToastNotification.showError(VolBrowseOpportunitiesPanel.this, errorMsg);
                    signUpBtn.setEnabled(true);
                } else {
                    ToastNotification.showSuccess(VolBrowseOpportunitiesPanel.this,
                            "Successfully signed up for: " + opp.getTitle());
                    loadOpportunitiesAsync(null);
                }
            }
        }.execute();
    }

    public void loadOpportunitiesAsync(String keyword) {
        statusLabel.setText("Loading...");
        tableModel.setRowCount(0);
        currentList.clear();

        new SwingWorker<List<Opportunity>, Void>() {
            @Override
            protected List<Opportunity> doInBackground() throws Exception {
                return oppService.getAvailableOpportunities(
                        keyword != null && !keyword.isBlank() ? keyword : null,
                        null, null, null);
            }

            @Override
            protected void done() {
                try {
                    List<Opportunity> list = get();
                    currentList.addAll(list);
                    for (Opportunity o : list) {
                        tableModel.addRow(new Object[]{
                                o.getId(),
                                o.getTitle(),
                                o.getOrganizationName() != null ? o.getOrganizationName() : "N/A",
                                o.getCategory() != null ? o.getCategory() : "General",
                                o.getFormattedDate(),
                                o.getLocation() != null ? o.getLocation() : "N/A",
                                o.getAvailableSlots() == 0 ? "FULL" : String.valueOf(o.getAvailableSlots())
                        });
                    }
                    statusLabel.setText(list.size() + " opportunity(ies) available");
                } catch (Exception ex) {
                    statusLabel.setText("Error loading opportunities");
                    ToastNotification.showError(VolBrowseOpportunitiesPanel.this,
                            "Failed to load opportunities: " + ex.getMessage());
                }
                showDetail(-1);
            }
        }.execute();
    }
}
