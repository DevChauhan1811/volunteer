package com.volunteer.view.volunteer;

import com.volunteer.model.Signup;
import com.volunteer.model.Volunteer;
import com.volunteer.model.VolunteerHours;
import com.volunteer.model.enums.RegistrationStatus;
import com.volunteer.service.SignupService;
import com.volunteer.service.VolunteerHoursService;
import com.volunteer.service.impl.SignupServiceImpl;
import com.volunteer.service.impl.VolunteerHoursServiceImpl;
import com.volunteer.view.common.StatCard;
import com.volunteer.view.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

/**
 * Volunteer dashboard overview panel showing a welcome message, key statistics,
 * and quick-start guidance. Loads metrics asynchronously from the database.
 */
public class VolOverviewPanel extends JPanel {

    private final Volunteer volunteerUser;
    private final SignupService signupService;
    private final VolunteerHoursService hoursService;

    private StatCard signupsCard;
    private StatCard hoursCard;
    private StatCard pendingCard;
    private StatCard completedCard;

    public VolOverviewPanel(Volunteer volunteer) {
        this.volunteerUser = volunteer;
        this.signupService = new SignupServiceImpl();
        this.hoursService = new VolunteerHoursServiceImpl();
        initializeUI();
        loadStatsAsync();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // ---- Welcome Card ----
        JPanel welcomeCard = UITheme.createCardPanel();
        welcomeCard.setLayout(new BoxLayout(welcomeCard, BoxLayout.Y_AXIS));
        welcomeCard.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel greeting = new JLabel("Welcome back, " + volunteerUser.getFullName() + "! 👋");
        greeting.setFont(UITheme.FONT_HEADER_LARGE);
        greeting.setForeground(UITheme.PRIMARY);

        JLabel subGreeting = new JLabel(volunteerUser.getDashboardWelcomeMessage());
        subGreeting.setFont(UITheme.FONT_REGULAR);
        subGreeting.setForeground(UITheme.TEXT_MUTED);

        JLabel emailLbl = new JLabel("Signed in as: " + volunteerUser.getEmail());
        emailLbl.setFont(UITheme.FONT_SMALL);
        emailLbl.setForeground(UITheme.TEXT_MUTED);

        welcomeCard.add(greeting);
        welcomeCard.add(Box.createVerticalStrut(6));
        welcomeCard.add(subGreeting);
        welcomeCard.add(Box.createVerticalStrut(4));
        welcomeCard.add(emailLbl);

        // ---- Stats Row ----
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        signupsCard  = new StatCard("ACTIVE SIGN-UPS",  "...", "Currently registered", UITheme.PRIMARY);
        hoursCard    = new StatCard("VERIFIED HOURS",   "...", "Total confirmed service hours", UITheme.SUCCESS);
        pendingCard  = new StatCard("PENDING HOURS",    "...", "Awaiting verification", UITheme.WARNING);
        completedCard = new StatCard("COMPLETED EVENTS","...", "Events attended or finished", new java.awt.Color(139, 92, 246));

        statsRow.add(signupsCard);
        statsRow.add(hoursCard);
        statsRow.add(pendingCard);
        statsRow.add(completedCard);

        // ---- Quick Start Tips Card ----
        JPanel tipsCard = UITheme.createCardPanel();
        tipsCard.setLayout(new BoxLayout(tipsCard, BoxLayout.Y_AXIS));
        tipsCard.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel tipsTitle = new JLabel("🚀  Getting Started");
        tipsTitle.setFont(UITheme.FONT_SUBHEADER);
        tipsTitle.setForeground(UITheme.TEXT_MAIN);

        String[] tips = {
            "📋  Browse Opportunities — Discover and sign up for volunteer initiatives in your community.",
            "✅  My Sign-Ups — View your registered events and cancel if your plans change.",
            "⏱️  Service Hours — Log your service hours after completing events and track verification.",
            "💬  Messages — Communicate directly with host organizations about any opportunity."
        };

        tipsCard.add(tipsTitle);
        tipsCard.add(Box.createVerticalStrut(12));
        for (String tip : tips) {
            JLabel tipLbl = new JLabel(tip);
            tipLbl.setFont(UITheme.FONT_REGULAR);
            tipLbl.setForeground(UITheme.TEXT_MUTED);
            tipsCard.add(tipLbl);
            tipsCard.add(Box.createVerticalStrut(8));
        }

        // ---- Assemble ----
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.add(welcomeCard, BorderLayout.NORTH);
        contentPanel.add(statsRow, BorderLayout.CENTER);
        contentPanel.add(tipsCard, BorderLayout.SOUTH);

        add(contentPanel, BorderLayout.CENTER);
    }

    public void loadStatsAsync() {
        new SwingWorker<int[], Void>() {
            double verifiedHours = 0.0;
            double pendingHours = 0.0;
            int activeSignups = 0;
            int completedEvents = 0;

            @Override
            protected int[] doInBackground() {
                try {
                    List<Signup> signups = signupService.getSignupsByVolunteer(volunteerUser, volunteerUser.getId());
                    for (Signup s : signups) {
                        if (s.getRegistrationStatus() == RegistrationStatus.REGISTERED) activeSignups++;
                        if (s.getRegistrationStatus() == RegistrationStatus.ATTENDED
                                || s.getRegistrationStatus() == RegistrationStatus.COMPLETED) completedEvents++;
                    }

                    List<VolunteerHours> hours = hoursService.getHoursByVolunteer(volunteerUser, volunteerUser.getId());
                    for (VolunteerHours h : hours) {
                        if (h.isApproved()) verifiedHours += h.getHoursWorked();
                        if (h.isPending()) pendingHours += h.getHoursWorked();
                    }
                } catch (Exception ignored) {}
                return new int[]{activeSignups, completedEvents};
            }

            @Override
            protected void done() {
                signupsCard.setValue(String.valueOf(activeSignups));
                hoursCard.setValue(String.format("%.1f hrs", verifiedHours));
                pendingCard.setValue(String.format("%.1f hrs", pendingHours));
                completedCard.setValue(String.valueOf(completedEvents));

                signupsCard.setSubtitle(activeSignups == 1 ? "1 active registration" : activeSignups + " active registrations");
                hoursCard.setSubtitle(verifiedHours > 0 ? "Keep it up!" : "Submit hours after events");
                pendingCard.setSubtitle(pendingHours > 0 ? "Waiting on organization" : "Nothing pending");
                completedCard.setSubtitle(completedEvents == 1 ? "1 event completed" : completedEvents + " events completed");
            }
        }.execute();
    }
}
