package com.volunteer.view.admin;

import com.volunteer.model.Admin;
import com.volunteer.service.SystemSettingsService;
import com.volunteer.service.impl.SystemSettingsServiceImpl;
import com.volunteer.view.common.StatCard;
import com.volunteer.view.common.ToastNotification;
import com.volunteer.view.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Map;

/**
 * Administrative Overview Panel displaying real-time platform statistics
 * loaded asynchronously from the MySQL database.
 */
public class AdminOverviewPanel extends JPanel {

    private final Admin adminUser;
    private final SystemSettingsService settingsService;

    private StatCard totalUsersCard;
    private StatCard totalOrgsCard;
    private StatCard totalVolsCard;
    private StatCard totalOppsCard;
    private StatCard pendingOppsCard;
    private StatCard activeSignupsCard;
    private StatCard verifiedHoursCard;

    private JButton refreshButton;
    private JLabel statusLabel;

    public AdminOverviewPanel(Admin admin) {
        this.adminUser = admin;
        this.settingsService = new SystemSettingsServiceImpl();
        initializeUI();
        loadStatisticsAsync();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);

        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(UITheme.BG_MAIN);
        mainContent.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header with Refresh Action
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleGrp = new JPanel();
        titleGrp.setLayout(new BoxLayout(titleGrp, BoxLayout.Y_AXIS));
        titleGrp.setOpaque(false);

        JLabel title = new JLabel("Platform Participation Overview");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Real-time aggregated metrics from MySQL database queries.");
        subtitle.setFont(UITheme.FONT_REGULAR);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        titleGrp.add(title);
        titleGrp.add(Box.createVerticalStrut(4));
        titleGrp.add(subtitle);

        JPanel actionGrp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionGrp.setOpaque(false);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);

        refreshButton = UITheme.createPrimaryButton("Refresh Metrics");
        refreshButton.addActionListener(e -> loadStatisticsAsync());

        actionGrp.add(statusLabel);
        actionGrp.add(refreshButton);

        header.add(titleGrp, BorderLayout.WEST);
        header.add(actionGrp, BorderLayout.EAST);
        mainContent.add(header);
        mainContent.add(Box.createVerticalStrut(20));

        // Metrics Grid (Top Row)
        JPanel grid1 = new JPanel(new GridLayout(1, 4, 16, 16));
        grid1.setOpaque(false);

        totalUsersCard = new StatCard("TOTAL USERS", "...", "Registered accounts", UITheme.PRIMARY);
        totalOrgsCard = new StatCard("ORGANIZATIONS", "...", "Active community hosts", UITheme.PRIMARY_HOVER);
        totalVolsCard = new StatCard("VOLUNTEERS", "...", "Registered service seekers", UITheme.SUCCESS);
        totalOppsCard = new StatCard("OPPORTUNITIES", "...", "All submitted listings", new Color(139, 92, 246));

        grid1.add(totalUsersCard);
        grid1.add(totalOrgsCard);
        grid1.add(totalVolsCard);
        grid1.add(totalOppsCard);
        mainContent.add(grid1);
        mainContent.add(Box.createVerticalStrut(16));

        // Metrics Grid (Bottom Row)
        JPanel grid2 = new JPanel(new GridLayout(1, 3, 16, 16));
        grid2.setOpaque(false);

        pendingOppsCard = new StatCard("PENDING MODERATION", "...", "Awaiting admin approval", UITheme.WARNING);
        activeSignupsCard = new StatCard("ACTIVE SIGN-UPS", "...", "Current event participants", UITheme.SUCCESS);
        verifiedHoursCard = new StatCard("VERIFIED SERVICE HOURS", "...", "Total approved hours", UITheme.PRIMARY);

        grid2.add(pendingOppsCard);
        grid2.add(activeSignupsCard);
        grid2.add(verifiedHoursCard);
        mainContent.add(grid2);
        mainContent.add(Box.createVerticalStrut(24));

        // System Status Card
        JPanel statusCard = UITheme.createCardPanel();
        statusCard.setLayout(new BoxLayout(statusCard, BoxLayout.Y_AXIS));

        JLabel infoTitle = new JLabel("System Health & Security Audit");
        infoTitle.setFont(UITheme.FONT_SUBHEADER);
        infoTitle.setForeground(UITheme.TEXT_MAIN);

        JLabel infoDesc1 = new JLabel("• Database Concurrency: Optimistic & pessimistic row-level locks active on registrations.");
        infoDesc1.setFont(UITheme.FONT_REGULAR);
        infoDesc1.setForeground(UITheme.TEXT_MUTED);

        JLabel infoDesc2 = new JLabel("• Security Architecture: Salted BCrypt password verification with work factor 12.");
        infoDesc2.setFont(UITheme.FONT_REGULAR);
        infoDesc2.setForeground(UITheme.TEXT_MUTED);

        JLabel infoDesc3 = new JLabel("• Administrator Safety: Protected from accidental deletion of last active admin account.");
        infoDesc3.setFont(UITheme.FONT_REGULAR);
        infoDesc3.setForeground(UITheme.TEXT_MUTED);

        statusCard.add(infoTitle);
        statusCard.add(Box.createVerticalStrut(10));
        statusCard.add(infoDesc1);
        statusCard.add(Box.createVerticalStrut(6));
        statusCard.add(infoDesc2);
        statusCard.add(Box.createVerticalStrut(6));
        statusCard.add(infoDesc3);

        mainContent.add(statusCard);

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void loadStatisticsAsync() {
        refreshButton.setEnabled(false);
        statusLabel.setText("Querying MySQL database...");

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                return settingsService.getPlatformOverviewStats(adminUser);
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);
                statusLabel.setText("Updated just now");
                try {
                    Map<String, Object> stats = get();
                    totalUsersCard.setValue(String.valueOf(stats.getOrDefault("totalUsers", 0)));
                    totalOrgsCard.setValue(String.valueOf(stats.getOrDefault("totalOrganizations", 0)));
                    totalVolsCard.setValue(String.valueOf(stats.getOrDefault("totalVolunteers", 0)));
                    totalOppsCard.setValue(String.valueOf(stats.getOrDefault("totalOpportunities", 0)));

                    long pending = ((Number) stats.getOrDefault("pendingOpportunities", 0)).longValue();
                    pendingOppsCard.setValue(String.valueOf(pending));
                    if (pending > 0) {
                        pendingOppsCard.setSubtitle("Requires review in Moderation tab");
                    } else {
                        pendingOppsCard.setSubtitle("All opportunities reviewed");
                    }

                    activeSignupsCard.setValue(String.valueOf(stats.getOrDefault("totalSignups", 0)));

                    double hours = ((Number) stats.getOrDefault("totalVerifiedHours", 0.0)).doubleValue();
                    verifiedHoursCard.setValue(String.format("%.1f hrs", hours));
                } catch (Exception ex) {
                    statusLabel.setText("Failed loading stats");
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(AdminOverviewPanel.this, "Error fetching statistics: " + cause.getMessage());
                }
            }
        };
        worker.execute();
    }
}
