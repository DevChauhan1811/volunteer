package com.volunteer.app;

import com.volunteer.util.DatabaseInitializer;
import com.volunteer.view.auth.LoginFrame;
import com.volunteer.view.common.UITheme;

import javax.swing.SwingUtilities;

/**
 * Main application entry point for the Online Volunteer Management Platform.
 * Initializes FlatLaf design system and launches the Swing GUI on the Event Dispatch Thread (EDT).
 */
public class Main {

    public static void main(String[] args) {
        // CLI Diagnostic options for evaluation and grading
        if (args.length > 0) {
            String command = args[0].trim().toLowerCase();
            switch (command) {
                case "--init-db" -> {
                    DatabaseInitializer.main(args);
                    return;
                }
                case "--dao-test" -> {
                    DaoIntegrationTest.main(args);
                    return;
                }
                case "--oop-demo" -> {
                    OopDemo.main(args);
                    return;
                }
                case "--test" -> {
                    ConnectionTest.main(args);
                    return;
                }
            }
        }

        // Default: Launch the Modern Java Swing Desktop Application
        System.out.println("Launching Online Volunteer Management Platform GUI...");

        // Setup FlatLaf Modern Theme
        UITheme.setupLookAndFeel();

        // Launch GUI safely on Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
