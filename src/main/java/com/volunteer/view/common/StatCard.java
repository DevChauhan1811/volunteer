package com.volunteer.view.common;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;

/**
 * Reusable modern metric card displaying a statistical indicator,
 * value, and subtitle with accent color styling.
 */
public class StatCard extends JPanel {

    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel subtitleLabel;

    public StatCard(String title, String initialValue, String subtitle, Color accentColor) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)
            )
        ));
        setPreferredSize(new Dimension(240, 105));
        setMinimumSize(new Dimension(200, 105));

        titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_SMALL);
        titleLabel.setForeground(UITheme.TEXT_MUTED);

        valueLabel = new JLabel(initialValue);
        valueLabel.setFont(UITheme.FONT_HEADER_LARGE);
        valueLabel.setForeground(UITheme.TEXT_MAIN);

        subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(UITheme.FONT_SMALL);
        subtitleLabel.setForeground(UITheme.TEXT_MUTED);

        add(titleLabel);
        add(Box.createVerticalStrut(4));
        add(valueLabel);
        add(Box.createVerticalStrut(4));
        add(subtitleLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }
}
