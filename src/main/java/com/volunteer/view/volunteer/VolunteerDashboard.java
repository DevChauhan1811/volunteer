package com.volunteer.view.volunteer;

import com.volunteer.config.UserSession;
import com.volunteer.model.Volunteer;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Main window for Volunteers. Contains a dark sidebar navigation and a card panel
 * with the following sections:
 * <ul>
 *   <li>Overview — Welcome dashboard with live statistics</li>
 *   <li>Browse Opportunities — Search and register for approved events</li>
 *   <li>My Sign-Ups — View and cancel personal registrations</li>
 *   <li>Service Hours — Submit hours and track verification status</li>
 *   <li>Messages — Communicate with host organizations</li>
 * </ul>
 */
public class VolunteerDashboard extends JFrame {

    private final Volunteer volunteerUser;

    private JPanel cardsPanel;
    private CardLayout cardLayout;
    private final List<JButton> navButtons = new ArrayList<>();

    private VolOverviewPanel overviewPanel;
    private VolBrowseOpportunitiesPanel browsePanel;
    private VolMySignupsPanel signupsPanel;
    private VolHoursPanel hoursPanel;
    private VolMessagesPanel messagesPanel;

    public VolunteerDashboard(Volunteer volunteer) {
        this.volunteerUser = volunteer;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Volunteer Management Platform — Volunteer Hub");
        setSize(1240, 820);
        setMinimumSize(new Dimension(1080, 720));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_MAIN);

        root.add(createHeaderPanel(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(createSidebarPanel(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.setOpaque(false);

        overviewPanel  = new VolOverviewPanel(volunteerUser);
        browsePanel    = new VolBrowseOpportunitiesPanel(volunteerUser);
        signupsPanel   = new VolMySignupsPanel(volunteerUser);
        hoursPanel     = new VolHoursPanel(volunteerUser);
        messagesPanel  = new VolMessagesPanel(volunteerUser);

        cardsPanel.add(overviewPanel,  "OVERVIEW");
        cardsPanel.add(browsePanel,    "BROWSE");
        cardsPanel.add(signupsPanel,   "SIGNUPS");
        cardsPanel.add(hoursPanel,     "HOURS");
        cardsPanel.add(messagesPanel,  "MESSAGES");

        center.add(cardsPanel, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        setContentPane(root);
        selectNavTab(0, "OVERVIEW");
    }

    // =========================================================================
    // Header
    // =========================================================================

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
            BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.setOpaque(false);

        JLabel brand = new JLabel("Online Volunteer Platform");
        brand.setFont(UITheme.FONT_HEADER);
        brand.setForeground(UITheme.PRIMARY);

        JLabel badge = UITheme.createBadge("VOLUNTEER", UITheme.SUCCESS_LIGHT, UITheme.SUCCESS);

        left.add(brand);
        left.add(Box.createHorizontalStrut(12));
        left.add(badge);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.setOpaque(false);

        JLabel userLbl = new JLabel(volunteerUser.getFullName() + " (" + volunteerUser.getEmail() + ")");
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

    // =========================================================================
    // Sidebar
    // =========================================================================

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UITheme.BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 12, 20, 12));

        JLabel menuTitle = new JLabel("  VOLUNTEER HUB");
        menuTitle.setFont(UITheme.FONT_SMALL);
        menuTitle.setForeground(new Color(148, 163, 184));
        sidebar.add(menuTitle);
        sidebar.add(Box.createVerticalStrut(12));

        addNavButton(sidebar, "🏠  Overview",              0, "OVERVIEW");
        addNavButton(sidebar, "📋  Browse Opportunities",  1, "BROWSE");
        addNavButton(sidebar, "✅  My Sign-Ups",            2, "SIGNUPS");
        addNavButton(sidebar, "⏱️  Service Hours",          3, "HOURS");
        addNavButton(sidebar, "💬  Messages",               4, "MESSAGES");

        sidebar.add(Box.createVerticalGlue());

        JLabel footerInfo = new JLabel("<html><center>Session Active<br><small>Volunteer Account</small></center></html>");
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
            switch (cardName) {
                case "OVERVIEW"  -> overviewPanel.loadStatsAsync();
                case "BROWSE"    -> browsePanel.loadOpportunitiesAsync(null);
                case "SIGNUPS"   -> signupsPanel.loadSignupsAsync();
                case "HOURS"     -> hoursPanel.loadHoursAsync();
            }
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

    // =========================================================================
    // Logout
    // =========================================================================

    private void handleLogout() {
        if (ToastNotification.confirm(this, "Are you sure you want to sign out?", "Confirm Logout")) {
            UserSession.logout();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}
