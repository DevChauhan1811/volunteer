package com.volunteer.view.organization;

import com.volunteer.config.UserSession;
import com.volunteer.model.Organization;
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
 * Complete Organization Workspace containing:
 * 1. Post & Manage Opportunities (with moderation status feedback)
 * 2. Participant Tracking & Attendance Check-In Roster
 * 3. Volunteer Hours Verification (Approve / Reject submitted hours)
 * 4. Participant Communication & Messaging
 * 5. Organization Profile Management
 */
public class OrganizationDashboard extends JFrame {

    private final Organization orgUser;

    private JPanel cardsPanel;
    private CardLayout cardLayout;
    private final List<JButton> navButtons = new ArrayList<>();

    private OrgOpportunitiesPanel opportunitiesPanel;
    private OrgParticipantsPanel participantsPanel;
    private OrgHoursVerificationPanel hoursPanel;
    private OrgMessagesPanel messagesPanel;
    private OrgProfilePanel profilePanel;

    public OrganizationDashboard(Organization org) {
        this.orgUser = org;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Volunteer Management Platform — Organization Workspace");
        setSize(1240, 820);
        setMinimumSize(new Dimension(1080, 720));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_MAIN);

        // Header
        root.add(createHeaderPanel(), BorderLayout.NORTH);

        // Center Split: Sidebar + Cards
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);

        center.add(createSidebarPanel(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.setOpaque(false);

        opportunitiesPanel = new OrgOpportunitiesPanel(orgUser);
        participantsPanel = new OrgParticipantsPanel(orgUser);
        hoursPanel = new OrgHoursVerificationPanel(orgUser);
        messagesPanel = new OrgMessagesPanel(orgUser);
        profilePanel = new OrgProfilePanel(orgUser);

        cardsPanel.add(opportunitiesPanel, "OPPORTUNITIES");
        cardsPanel.add(participantsPanel, "PARTICIPANTS");
        cardsPanel.add(hoursPanel, "HOURS");
        cardsPanel.add(messagesPanel, "MESSAGES");
        cardsPanel.add(profilePanel, "PROFILE");

        center.add(cardsPanel, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        setContentPane(root);
        selectNavTab(0, "OPPORTUNITIES");
    }

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

        JLabel badge = UITheme.createBadge("ORGANIZATION", UITheme.PRIMARY_LIGHT, UITheme.PRIMARY);

        left.add(brand);
        left.add(Box.createHorizontalStrut(12));
        left.add(badge);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.setOpaque(false);

        String display = orgUser.getOrganizationName() != null ? orgUser.getOrganizationName() : orgUser.getFullName();
        JLabel userLbl = new JLabel(display + " (" + orgUser.getEmail() + ")");
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

        JLabel menuTitle = new JLabel("  ORGANIZATION WORKSPACE");
        menuTitle.setFont(UITheme.FONT_SMALL);
        menuTitle.setForeground(new Color(148, 163, 184));
        sidebar.add(menuTitle);
        sidebar.add(Box.createVerticalStrut(12));

        addNavButton(sidebar, "📋  My Opportunities", 0, "OPPORTUNITIES");
        addNavButton(sidebar, "👥  Participant Roster", 1, "PARTICIPANTS");
        addNavButton(sidebar, "⏱️  Verify Hours", 2, "HOURS");
        addNavButton(sidebar, "💬  Messages & Inquiries", 3, "MESSAGES");
        addNavButton(sidebar, "🏢  Profile Settings", 4, "PROFILE");

        sidebar.add(Box.createVerticalGlue());

        JLabel footerInfo = new JLabel("<html><center>Session Active<br><small>Host Organization</small></center></html>");
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
            if ("OPPORTUNITIES".equals(cardName)) opportunitiesPanel.loadOpportunitiesAsync();
            if ("PARTICIPANTS".equals(cardName)) participantsPanel.loadSignupsAsync();
            if ("HOURS".equals(cardName)) hoursPanel.loadHoursAsync();
            if ("MESSAGES".equals(cardName)) messagesPanel.loadParticipantsAsync();
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
        if (ToastNotification.confirm(this, "Are you sure you want to sign out?", "Confirm Sign Out")) {
            UserSession.logout();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}
