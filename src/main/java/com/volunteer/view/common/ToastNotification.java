package com.volunteer.view.common;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Clean user notification helper presenting consistent success, error, and confirmation messages.
 */
public final class ToastNotification {

    private ToastNotification() {
    }

    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            "Success",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    public static void showWarning(Component parent, String message) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            "Notice",
            JOptionPane.WARNING_MESSAGE
        );
    }

    public static boolean confirm(Component parent, String message, String title) {
        int result = JOptionPane.showConfirmDialog(
            parent,
            message,
            title,
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }
}
