package com.volunteer.view.admin;

import com.volunteer.model.Admin;
import com.volunteer.model.User;
import com.volunteer.model.enums.AccountStatus;
import com.volunteer.model.enums.Role;
import com.volunteer.service.UserService;
import com.volunteer.service.impl.UserServiceImpl;
import com.volunteer.view.common.ModernTable;
import com.volunteer.view.common.ToastNotification;
import com.volunteer.view.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * User Management panel for viewing, searching, filtering, creating,
 * and moderating user accounts with last-admin safety protection.
 */
public class UserManagementPanel extends JPanel {

    private final Admin adminUser;
    private final UserService userService;

    private JTextField searchField;
    private JComboBox<String> roleFilterCombo;
    private JComboBox<String> statusFilterCombo;
    private ModernTable userTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;

    private final List<User> currentUsersList = new ArrayList<>();

    public UserManagementPanel(Admin admin) {
        this.adminUser = admin;
        this.userService = new UserServiceImpl();
        initializeUI();
        loadUsersAsync();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Top Filter & Search Bar
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // Header Title
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel titleLbl = new JLabel("Registered User Directory");
        titleLbl.setFont(UITheme.FONT_HEADER);
        titleLbl.setForeground(UITheme.TEXT_MAIN);

        JLabel subLbl = new JLabel("Manage platform credentials, roles, and account statuses.");
        subLbl.setFont(UITheme.FONT_REGULAR);
        subLbl.setForeground(UITheme.TEXT_MUTED);

        JPanel titleGrp = new JPanel();
        titleGrp.setLayout(new BoxLayout(titleGrp, BoxLayout.Y_AXIS));
        titleGrp.setOpaque(false);
        titleGrp.add(titleLbl);
        titleGrp.add(Box.createVerticalStrut(4));
        titleGrp.add(subLbl);

        JButton createBtn = UITheme.createPrimaryButton("+ Create User");
        createBtn.addActionListener(e -> openCreateUserDialog());

        titleRow.add(titleGrp, BorderLayout.WEST);
        titleRow.add(createBtn, BorderLayout.EAST);
        topContainer.add(titleRow);
        topContainer.add(Box.createVerticalStrut(16));

        // Filter Controls Card
        JPanel filterCard = UITheme.createCardPanel();
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 4));

        filterCard.add(new JLabel("Search:"));
        searchField = UITheme.createTextField(14);
        searchField.putClientProperty("JTextField.placeholderText", "Name or email...");
        filterCard.add(searchField);

        filterCard.add(new JLabel("Role:"));
        roleFilterCombo = new JComboBox<>(new String[]{"All Roles", "ADMIN", "ORGANIZATION", "VOLUNTEER"});
        roleFilterCombo.setFont(UITheme.FONT_REGULAR);
        filterCard.add(roleFilterCombo);

        filterCard.add(new JLabel("Status:"));
        statusFilterCombo = new JComboBox<>(new String[]{"All Statuses", "ACTIVE", "INACTIVE", "SUSPENDED"});
        statusFilterCombo.setFont(UITheme.FONT_REGULAR);
        filterCard.add(statusFilterCombo);

        JButton searchBtn = UITheme.createSecondaryButton("Filter");
        searchBtn.addActionListener(e -> loadUsersAsync());
        filterCard.add(searchBtn);

        JButton resetBtn = UITheme.createSecondaryButton("Reset");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            roleFilterCombo.setSelectedIndex(0);
            statusFilterCombo.setSelectedIndex(0);
            loadUsersAsync();
        });
        filterCard.add(resetBtn);

        topContainer.add(filterCard);
        topContainer.add(Box.createVerticalStrut(12));
        add(topContainer, BorderLayout.NORTH);

        // Center Table
        String[] columns = {"ID", "Full Name", "Email Address", "Role", "Account Status", "Created Date"};
        tableModel = new DefaultTableModel(columns, 0);
        userTable = new ModernTable(tableModel);

        JScrollPane scrollPane = userTable.wrapInScrollPane();
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Actions Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        bottomBar.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        statusLabel = new JLabel("Loading users...");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setOpaque(false);

        JButton toggleStatusBtn = UITheme.createSecondaryButton("Toggle Active / Inactive");
        toggleStatusBtn.addActionListener(e -> handleToggleStatus());

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadUsersAsync());

        btnBar.add(toggleStatusBtn);
        btnBar.add(refreshBtn);

        bottomBar.add(statusLabel, BorderLayout.WEST);
        bottomBar.add(btnBar, BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);
    }

    public void loadUsersAsync() {
        statusLabel.setText("Fetching users from MySQL...");
        String keyword = searchField.getText().trim();
        String roleStr = (String) roleFilterCombo.getSelectedItem();
        String statusStr = (String) statusFilterCombo.getSelectedItem();

        Role roleFilter = "All Roles".equals(roleStr) ? null : Role.valueOf(roleStr);
        AccountStatus statusFilter = "All Statuses".equals(statusStr) ? null : AccountStatus.valueOf(statusStr);

        SwingWorker<List<User>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                return userService.searchUsers(adminUser, keyword, roleFilter, statusFilter);
            }

            @Override
            protected void done() {
                try {
                    List<User> users = get();
                    currentUsersList.clear();
                    currentUsersList.addAll(users);

                    tableModel.setRowCount(0);
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy");

                    for (User u : users) {
                        String createdDate = u.getCreatedAt() != null ? u.getCreatedAt().format(dtf) : "";
                        tableModel.addRow(new Object[]{
                            u.getId(),
                            u.getFullName(),
                            u.getEmail(),
                            u.getRole().name(),
                            u.getAccountStatus().name(),
                            createdDate
                        });
                    }
                    statusLabel.setText("Showing " + users.size() + " user accounts.");
                } catch (Exception ex) {
                    statusLabel.setText("Failed loading users.");
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(UserManagementPanel.this, "Error fetching users: " + cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void handleToggleStatus() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            ToastNotification.showWarning(this, "Please select a user from the table to modify.");
            return;
        }

        User selectedUser = currentUsersList.get(selectedRow);
        AccountStatus newStatus = selectedUser.getAccountStatus() == AccountStatus.ACTIVE ?
            AccountStatus.INACTIVE : AccountStatus.ACTIVE;

        String actionText = newStatus == AccountStatus.ACTIVE ? "activate" : "deactivate";
        if (!ToastNotification.confirm(this,
            "Are you sure you want to " + actionText + " account for " + selectedUser.getEmail() + "?",
            "Confirm Account Status Change")) {
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                userService.setUserAccountStatus(adminUser, selectedUser.getId(), newStatus);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ToastNotification.showSuccess(UserManagementPanel.this,
                        "Account status updated to " + newStatus.name() + " for " + selectedUser.getEmail());
                    loadUsersAsync();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(UserManagementPanel.this, cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void openCreateUserDialog() {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        UserCreateDialog dialog = new UserCreateDialog(parent, adminUser, this::loadUsersAsync);
        dialog.setVisible(true);
    }
}
