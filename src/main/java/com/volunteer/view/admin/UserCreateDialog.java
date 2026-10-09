package com.volunteer.view.admin;

import com.volunteer.model.Admin;
import com.volunteer.model.User;
import com.volunteer.model.enums.Role;
import com.volunteer.service.UserService;
import com.volunteer.service.impl.UserServiceImpl;
import com.volunteer.view.common.ToastNotification;
import com.volunteer.view.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Modal dialog for administrators to create a user account directly.
 */
public class UserCreateDialog extends JDialog {

    private final Admin adminInitiator;
    private final UserService userService;
    private final Runnable onSuccessCallback;

    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JComboBox<Role> roleCombo;
    private JButton submitButton;

    public UserCreateDialog(JFrame parent, Admin admin, Runnable onSuccessCallback) {
        super(parent, "Create New User Account", true);
        this.adminInitiator = admin;
        this.userService = new UserServiceImpl();
        this.onSuccessCallback = onSuccessCallback;
        initializeUI();
    }

    private void initializeUI() {
        setSize(460, 420);
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_MAIN);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel titleLbl = new JLabel("Create System Account");
        titleLbl.setFont(UITheme.FONT_HEADER);
        titleLbl.setForeground(UITheme.TEXT_MAIN);

        JLabel subLbl = new JLabel("Administratively provision accounts for any role.");
        subLbl.setFont(UITheme.FONT_REGULAR);
        subLbl.setForeground(UITheme.TEXT_MUTED);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(titleLbl);
        header.add(Box.createVerticalStrut(4));
        header.add(subLbl);
        root.add(header, BorderLayout.NORTH);

        JPanel form = UITheme.createCardPanel();
        form.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        int row = 0;
        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Full Name *"), gbc);
        nameField = UITheme.createTextField(20);
        gbc.gridy = row++;
        form.add(nameField, gbc);

        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Email Address *"), gbc);
        emailField = UITheme.createTextField(20);
        gbc.gridy = row++;
        form.add(emailField, gbc);

        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Password * (Min 6 chars)"), gbc);
        passwordField = UITheme.createPasswordField(20);
        gbc.gridy = row++;
        form.add(passwordField, gbc);

        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Assign Role *"), gbc);
        roleCombo = new JComboBox<>(Role.values());
        roleCombo.setFont(UITheme.FONT_REGULAR);
        roleCombo.setPreferredSize(new Dimension(roleCombo.getPreferredSize().width, 36));
        gbc.gridy = row++;
        form.add(roleCombo, gbc);

        root.add(form, BorderLayout.CENTER);

        // Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setOpaque(false);

        JButton cancelBtn = UITheme.createSecondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        submitButton = UITheme.createPrimaryButton("Create Account");
        submitButton.addActionListener(e -> handleSubmit());

        footer.add(cancelBtn);
        footer.add(submitButton);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void handleSubmit() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String pass = new String(passwordField.getPassword());
        Role role = (Role) roleCombo.getSelectedItem();

        submitButton.setEnabled(false);
        submitButton.setText("Creating...");

        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() throws Exception {
                return userService.createUser(adminInitiator, name, email, pass, role);
            }

            @Override
            protected void done() {
                submitButton.setEnabled(true);
                submitButton.setText("Create Account");
                try {
                    User created = get();
                    ToastNotification.showSuccess(UserCreateDialog.this,
                        "User account created successfully for " + created.getEmail());
                    if (onSuccessCallback != null) {
                        onSuccessCallback.run();
                    }
                    dispose();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(UserCreateDialog.this, cause.getMessage());
                }
            }
        };
        worker.execute();
    }
}
