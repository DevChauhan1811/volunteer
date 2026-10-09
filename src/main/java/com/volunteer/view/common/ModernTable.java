package com.volunteer.view.common;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.function.Function;

/**
 * Custom modern JTable component with clean typography, zebra striping,
 * row padding, and styled table headers.
 */
public class ModernTable extends JTable {

    public ModernTable(DefaultTableModel model) {
        super(model);
        initializeStyle();
    }

    private void initializeStyle() {
        setRowHeight(36);
        setFont(UITheme.FONT_REGULAR);
        setForeground(UITheme.TEXT_MAIN);
        setGridColor(UITheme.BORDER);
        setShowHorizontalLines(true);
        setShowVerticalLines(false);
        setFillsViewportHeight(true);

        // Header Styling
        JTableHeader header = getTableHeader();
        header.setFont(UITheme.FONT_BOLD);
        header.setBackground(new Color(241, 245, 249)); // Slate 100
        header.setForeground(UITheme.TEXT_MAIN);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER));
        header.setReorderingAllowed(false);

        // Center / Padding renderer
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

                if (!isSelected) {
                    // Subtle alternating zebra striping
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(UITheme.TEXT_MAIN);
                } else {
                    c.setBackground(UITheme.PRIMARY_LIGHT);
                    c.setForeground(UITheme.PRIMARY_HOVER);
                }
                return c;
            }
        });
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false; // Non-editable by default
    }

    public JScrollPane wrapInScrollPane() {
        JScrollPane scrollPane = new JScrollPane(this);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    /**
     * Hides a column by collapsing its min/max/preferred widths to 0.
     */
    public void setColumnHidden(int columnIndex) {
        TableColumn col = getColumnModel().getColumn(columnIndex);
        col.setMinWidth(0);
        col.setMaxWidth(0);
        col.setPreferredWidth(0);
        col.setWidth(0);
    }

    /**
     * Applies a color-coded cell renderer to a specific column.
     * The colorProvider maps a cell value (String) to the desired foreground Color.
     */
    public void setStatusColumnRenderer(int columnIndex, Function<Object, Color> colorProvider) {
        getColumnModel().getColumn(columnIndex).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(colorProvider.apply(value));
                } else {
                    c.setBackground(UITheme.PRIMARY_LIGHT);
                    c.setForeground(UITheme.PRIMARY_HOVER);
                }
                setFont(UITheme.FONT_BOLD);
                return c;
            }
        });
    }
}
