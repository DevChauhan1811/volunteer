package com.volunteer.view.admin;

import com.volunteer.config.UserSession;
import com.volunteer.model.Admin;
import com.volunteer.view.auth.LoginFrame;
import com.volunteer.view.common.ToastNotification;
import com.volunteer.view.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Complete Admin Dashboard containing:
 * 1. Overview & Real-Time Database Statistics
 * 2. User Directory & Status Moderation
 * 3. Opportunity Review & Moderation
 * 4. System Settings Management
 */
public class AdminDashboard extends JFrame {

    private final Admin adminUser;

    private JPanel cardsPanel;
    private CardLayout cardLayout;
    private final List<JButton> navButtons = new ArrayList<>();

    private AdminOverviewPanel overviewPanel;
    private UserManagementPanel userPanel;
    private OpportunityModerationPanel moderationPanel;
    private SystemSettingsPanel settingsPanel;

    public AdminDashboard(Admin admin) {
        this.adminUser = admin;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Volunteer Management Platform — Administrator Workspace");
        setSize(1240, 820);
        setMinimumSize(new Dimension(1080, 720));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_MAIN);

        // Top Navigation Header
        root.add(createHeaderPanel(), BorderLayout.NORTH);

        // Center split: Sidebar Navigation + Cards Content Area
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);

        // Sidebar
        center.add(createSidebarPanel(), BorderLayout.WEST);

        // Content Area with CardLayout
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.setOpaque(false);

        overviewPanel = new AdminOverviewPanel(adminUser);
        userPanel = new UserManagementPanel(adminUser);
        moderationPanel = new OpportunityModerationPanel(adminUser);
        settingsPanel = new SystemSettingsPanel(adminUser);

        cardsPanel.add(overviewPanel, "OVERVIEW");
        cardsPanel.add(userPanel, "USERS");
        cardsPanel.add(moderationPanel, "MODERATION");
        cardsPanel.add(settingsPanel, "SETTINGS");

        center.add(cardsPanel, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        setContentPane(root);
        selectNavTab(0, "OVERVIEW");
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
            BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));

        // Brand
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.setOpaque(false);

        JLabel brand = new JLabel("Online Volunteer Platform");
        brand.setFont(UITheme.FONT_HEADER);
        brand.setForeground(UITheme.PRIMARY);

        JLabel badge = UITheme.createBadge("ADMINISTRATOR", UITheme.DANGER_LIGHT, UITheme.DANGER);

        left.add(brand);
        left.add(Box.createHorizontalStrut(12));
        left.add(badge);

        // User profile & logout
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.setOpaque(false);

        JLabel userLbl = new JLabel(adminUser.getFullName() + " (" + adminUser.getEmail() + ")");
        userLbl.setFont(UITheme.FONT_BOLD);
        userLbl.setForeground(UITheme.TEXT_MAIN);

        JButton logoutBtn = UITheme.createSecondaryButton("Sign Out");
        logoutBtn.addActionListener(e -> handleLogout());

        right.add(userLbl);
        right.add(Box.createHorizontalStrut(16));
        right.add(logoutBtn);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UITheme.BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 12, 20, 12));

        JLabel menuTitle = new JLabel("  NAVIGATION");
        menuTitle.setFont(UITheme.FONT_SMALL);
        menuTitle.setForeground(new Color(148, 163, 184)); // Slate 400
        sidebar.add(menuTitle);
        sidebar.add(Box.createVerticalStrut(12));

        addNavButton(sidebar, "📊  Participation Overview", 0, "OVERVIEW");
        addNavButton(sidebar, "👥  User Management", 1, "USERS");
        addNavButton(sidebar, "🛡️  Opportunity Moderation", 2, "MODERATION");
        addNavButton(sidebar, "⚙️  System Settings", 3, "SETTINGS");

        sidebar.add(Box.createVerticalGlue());

        // Footer in sidebar
        JLabel footerInfo = new JLabel("<html><center>Session Active<br><small>Logged in as Admin</small></center></html>");
        footerInfo.setFont(UITheme.FONT_SMALL);
        footerInfo.setForeground(new Color(148, 163, 184));
        footerInfo.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        sidebar.add(footerInfo);

        return sidebar;
    }

    private void addNavButton(JPanel sidebar, String text, int index, String cardName) {
        JButton btn = new JButton(text);
        btn.setFont(UITheme.FONT_REGULAR);
        btn.setForeground(new Color(226, 232, 240));
        btn.setBackground(UITheme.BG_SIDEBAR);
        btn.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setHorizontalAlignment(JButton.LEFT);

        btn.addActionListener(e -> {
            selectNavTab(index, cardName);
            // Refresh content upon switching
            if ("OVERVIEW".equals(cardName)) overviewPanel.loadStatisticsAsync();
            if ("USERS".equals(cardName)) userPanel.loadUsersAsync();
            if ("MODERATION".equals(cardName)) moderationPanel.loadOpportunitiesAsync();
            if ("SETTINGS".equals(cardName)) settingsPanel.loadSettingsAsync();
        });

        navButtons.add(btn);
        sidebar.add(btn);
        sidebar.add(Box.createVerticalStrut(6));
    }

    private void selectNavTab(int selectedIndex, String cardName) {
        for (int i = 0; i < navButtons.size(); i++) {
            JButton btn = navButtons.get(i);
            if (i == selectedIndex) {
                btn.setBackground(UITheme.PRIMARY);
                btn.setForeground(Color.WHITE);
                btn.setFont(UITheme.FONT_BOLD);
            } else {
                btn.setBackground(UITheme.BG_SIDEBAR);
                btn.setForeground(new Color(203, 213, 225));
                btn.setFont(UITheme.FONT_REGULAR);
            }
        }
        cardLayout.show(cardsPanel, cardName);
    }

    private void handleLogout() {
        if (ToastNotification.confirm(this, "Are you sure you want to sign out of the Administrator Console?", "Confirm Sign Out")) {
            UserSession.logout();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}
