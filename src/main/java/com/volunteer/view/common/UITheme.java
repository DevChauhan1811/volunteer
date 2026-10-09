package com.volunteer.view.common;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Central design system and styling tokens for the application.
 * Manages modern colors, typography, cards, and reusable UI components.
 */
public final class UITheme {

    // --- Curated Color Palette ---
    public static final Color PRIMARY = new Color(37, 99, 235);       // Royal Blue #2563EB
    public static final Color PRIMARY_HOVER = new Color(29, 78, 216); // Darker Blue #1D4ED8
    public static final Color PRIMARY_LIGHT = new Color(239, 246, 255);// Soft Blue tint
    public static final Color SUCCESS = new Color(22, 163, 74);       // Emerald Green #16A34A
    public static final Color SUCCESS_LIGHT = new Color(240, 253, 244);
    public static final Color DANGER = new Color(220, 38, 38);        // Crimson Red #DC2626
    public static final Color DANGER_LIGHT = new Color(254, 242, 242);
    public static final Color WARNING = new Color(217, 119, 6);       // Amber #D97706
    public static final Color WARNING_LIGHT = new Color(254, 252, 232);

    public static final Color BG_MAIN = new Color(248, 250, 252);     // Slate 50 #F8FAFC
    public static final Color BG_CARD = Color.WHITE;
    public static final Color BG_SIDEBAR = new Color(15, 23, 42);     // Dark Slate #0F172A
    public static final Color SIDEBAR_HOVER = new Color(30, 41, 59);

    public static final Color TEXT_MAIN = new Color(15, 23, 42);      // Slate 900
    public static final Color TEXT_MUTED = new Color(100, 116, 139);  // Slate 500
    public static final Color TEXT_LIGHT = new Color(241, 245, 249);  // Slate 100
    public static final Color BORDER = new Color(226, 232, 240);      // Slate 200

    // --- Modern Typography ---
    private static final String FONT_FAMILY = "Segoe UI";
    public static final Font FONT_HEADER_LARGE = new Font(FONT_FAMILY, Font.BOLD, 22);
    public static final Font FONT_HEADER = new Font(FONT_FAMILY, Font.BOLD, 18);
    public static final Font FONT_SUBHEADER = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font FONT_REGULAR = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font(FONT_FAMILY, Font.PLAIN, 11);

    private UITheme() {
    }

    /**
     * Installs FlatLaf modern Look & Feel and global UI defaults.
     */
    public static void setupLookAndFeel() {
        try {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.thumbArc", 8);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("Table.selectionBackground", PRIMARY_LIGHT);
            UIManager.put("Table.selectionForeground", TEXT_MAIN);
            UIManager.put("Table.rowHeight", 34);
        } catch (Exception e) {
            System.err.println("Could not initialize FlatLaf look and feel: " + e.getMessage());
        }
    }

    // --- Component Factory Helpers ---

    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 36));
        return btn;
    }

    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(Color.WHITE);
        btn.setForeground(TEXT_MAIN);
        btn.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 36));
        return btn;
    }

    public static JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(DANGER);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 36));
        return btn;
    }

    public static JButton createSuccessButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(SUCCESS);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 36));
        return btn;
    }

    public static JTextField createTextField(int columns) {
        JTextField tf = new JTextField(columns);
        tf.setFont(FONT_REGULAR);
        tf.setPreferredSize(new Dimension(tf.getPreferredSize().width, 36));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        return tf;
    }

    public static JPasswordField createPasswordField(int columns) {
        JPasswordField pf = new JPasswordField(columns);
        pf.setFont(FONT_REGULAR);
        pf.setPreferredSize(new Dimension(pf.getPreferredSize().width, 36));
        pf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        return pf;
    }

    /**
     * Creates an elevated modern card panel with subtle rounded corners and clean border.
     */
    public static JPanel createCardPanel() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        return card;
    }

    public static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BOLD);
        label.setForeground(TEXT_MAIN);
        return label;
    }

    public static JLabel createBadge(String text, Color bg, Color fg) {
        JLabel badge = new JLabel(" " + text + " ");
        badge.setFont(FONT_SMALL);
        badge.setOpaque(true);
        badge.setBackground(bg);
        badge.setForeground(fg);
        badge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        return badge;
    }
}
