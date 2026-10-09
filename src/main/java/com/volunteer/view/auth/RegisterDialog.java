package com.volunteer.view.auth;

import com.volunteer.model.Organization;
import com.volunteer.model.User;
import com.volunteer.service.UserService;
import com.volunteer.service.impl.UserServiceImpl;
import com.volunteer.view.common.ToastNotification;
import com.volunteer.view.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;

/**
 * Registration modal dialog allowing self-registration as a Volunteer or Organization.
 * Strictly prevents self-registration of Admin accounts (privilege escalation guard).
 */
public class RegisterDialog extends JDialog {

    private final UserService userService;
    private final Consumer<String> onSuccessCallback;

    private JRadioButton volunteerRadio;
    private JRadioButton orgRadio;
    private JPanel cardsPanel;
    private CardLayout cardLayout;

    // Volunteer Form Fields
    private JTextField volNameField;
    private JTextField volEmailField;
    private JPasswordField volPasswordField;
    private JPasswordField volConfirmPasswordField;

    // Organization Form Fields
    private JTextField orgRepNameField;
    private JTextField orgNameField;
    private JTextField orgEmailField;
    private JPasswordField orgPasswordField;
    private JPasswordField orgConfirmPasswordField;
    private JTextArea orgDescArea;
    private JTextField orgContactField;

    private JButton submitButton;

    public RegisterDialog(JFrame parent, Consumer<String> onSuccessCallback) {
        super(parent, "Create Account — Online Volunteer Platform", true);
        this.userService = new UserServiceImpl();
        this.onSuccessCallback = onSuccessCallback;
        initializeUI();
    }

    private void initializeUI() {
        setSize(540, 680);
        setMinimumSize(new Dimension(500, 600));
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_MAIN);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel titleLbl = new JLabel("Join Volunteer Platform");
        titleLbl.setFont(UITheme.FONT_HEADER);
        titleLbl.setForeground(UITheme.TEXT_MAIN);

        JLabel subLbl = new JLabel("Select your role and enter your details to create an account.");
        subLbl.setFont(UITheme.FONT_REGULAR);
        subLbl.setForeground(UITheme.TEXT_MUTED);

        headerPanel.add(titleLbl);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subLbl);
        headerPanel.add(Box.createVerticalStrut(16));

        // Role Selector Toggle
        JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        rolePanel.setOpaque(false);

        volunteerRadio = new JRadioButton("Volunteer", true);
        volunteerRadio.setFont(UITheme.FONT_BOLD);
        orgRadio = new JRadioButton("Organization", false);
        orgRadio.setFont(UITheme.FONT_BOLD);

        ButtonGroup group = new ButtonGroup();
        group.add(volunteerRadio);
        group.add(orgRadio);

        rolePanel.add(new JLabel("Register As: "));
        rolePanel.add(volunteerRadio);
        rolePanel.add(orgRadio);

        headerPanel.add(rolePanel);
        headerPanel.add(Box.createVerticalStrut(12));
        root.add(headerPanel, BorderLayout.NORTH);

        // Forms Card Panel
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.setOpaque(false);

        cardsPanel.add(createVolunteerForm(), "VOLUNTEER");
        cardsPanel.add(createOrganizationForm(), "ORGANIZATION");

        volunteerRadio.addActionListener(e -> cardLayout.show(cardsPanel, "VOLUNTEER"));
        orgRadio.addActionListener(e -> cardLayout.show(cardsPanel, "ORGANIZATION"));

        root.add(cardsPanel, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setOpaque(false);

        JButton cancelBtn = UITheme.createSecondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        submitButton = UITheme.createPrimaryButton("Create Account");
        submitButton.addActionListener(e -> handleRegister());

        footer.add(cancelBtn);
        footer.add(submitButton);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel createVolunteerForm() {
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
        volNameField = UITheme.createTextField(20);
        gbc.gridy = row++;
        form.add(volNameField, gbc);

        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Email Address *"), gbc);
        volEmailField = UITheme.createTextField(20);
        gbc.gridy = row++;
        form.add(volEmailField, gbc);

        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Password * (Min 6 chars)"), gbc);
        volPasswordField = UITheme.createPasswordField(20);
        gbc.gridy = row++;
        form.add(volPasswordField, gbc);

        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Confirm Password *"), gbc);
        volConfirmPasswordField = UITheme.createPasswordField(20);
        gbc.gridy = row++;
        form.add(volConfirmPasswordField, gbc);

        return form;
    }

    private JPanel createOrganizationForm() {
        JPanel form = UITheme.createCardPanel();
        form.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        int row = 0;
        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Organization Name *"), gbc);
        orgNameField = UITheme.createTextField(20);
        gbc.gridy = row++;
        form.add(orgNameField, gbc);

        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Representative Full Name *"), gbc);
        orgRepNameField = UITheme.createTextField(20);
        gbc.gridy = row++;
        form.add(orgRepNameField, gbc);

        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Official Email *"), gbc);
        orgEmailField = UITheme.createTextField(20);
        gbc.gridy = row++;
        form.add(orgEmailField, gbc);

        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Password *"), gbc);
        orgPasswordField = UITheme.createPasswordField(20);
        gbc.gridy = row++;
        form.add(orgPasswordField, gbc);

        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Confirm Password *"), gbc);
        orgConfirmPasswordField = UITheme.createPasswordField(20);
        gbc.gridy = row++;
        form.add(orgConfirmPasswordField, gbc);

        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Contact Information (Phone / Office)"), gbc);
        orgContactField = UITheme.createTextField(20);
        gbc.gridy = row++;
        form.add(orgContactField, gbc);

        gbc.gridy = row++;
        form.add(UITheme.createFieldLabel("Mission / Description"), gbc);
        orgDescArea = new JTextArea(2, 20);
        orgDescArea.setFont(UITheme.FONT_REGULAR);
        orgDescArea.setLineWrap(true);
        orgDescArea.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(orgDescArea);
        gbc.gridy = row++;
        form.add(sp, gbc);

        return form;
    }

    private void handleRegister() {
        boolean isVolunteer = volunteerRadio.isSelected();

        String email = isVolunteer ? volEmailField.getText().trim() : orgEmailField.getText().trim();
        String pass = new String(isVolunteer ? volPasswordField.getPassword() : orgPasswordField.getPassword());
        String confirm = new String(isVolunteer ? volConfirmPasswordField.getPassword() : orgConfirmPasswordField.getPassword());

        if (!pass.equals(confirm)) {
            ToastNotification.showError(this, "Passwords do not match.");
            return;
        }

        submitButton.setEnabled(false);
        submitButton.setText("Creating Account...");

        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() throws Exception {
                if (isVolunteer) {
                    String name = volNameField.getText().trim();
                    return userService.registerVolunteer(name, email, pass);
                } else {
                    String repName = orgRepNameField.getText().trim();
                    String orgName = orgNameField.getText().trim();
                    String desc = orgDescArea.getText().trim();
                    String contact = orgContactField.getText().trim();
                    return userService.registerOrganization(repName, email, pass, orgName, desc, contact);
                }
            }

            @Override
            protected void done() {
                submitButton.setEnabled(true);
                submitButton.setText("Create Account");
                try {
                    User created = get();
                    ToastNotification.showSuccess(RegisterDialog.this,
                        "Account created successfully for " + created.getEmail() + "!\nYou may now sign in.");
                    if (onSuccessCallback != null) {
                        onSuccessCallback.accept(created.getEmail());
                    }
                    dispose();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastNotification.showError(RegisterDialog.this, cause.getMessage());
                }
            }
        };
        worker.execute();
    }
}
