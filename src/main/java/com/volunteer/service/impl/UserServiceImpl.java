package com.volunteer.service.impl;

import com.volunteer.dao.OrganizationDAO;
import com.volunteer.dao.SystemSettingsDAO;
import com.volunteer.dao.UserDAO;
import com.volunteer.exception.AuthenticationException;
import com.volunteer.exception.DatabaseException;
import com.volunteer.exception.DuplicateResourceException;
import com.volunteer.exception.UnauthorizedException;
import com.volunteer.exception.ValidationException;
import com.volunteer.model.Admin;
import com.volunteer.model.Organization;
import com.volunteer.model.User;
import com.volunteer.model.Volunteer;
import com.volunteer.model.enums.AccountStatus;
import com.volunteer.model.enums.Role;
import com.volunteer.service.UserService;
import com.volunteer.util.PasswordHasher;
import com.volunteer.util.ValidationUtil;

import java.util.List;
import java.util.Optional;

/**
 * Service implementation for user authentication, registration, and administrative account management.
 */
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final OrganizationDAO organizationDAO;
    private final SystemSettingsDAO systemSettingsDAO;

    public UserServiceImpl() {
        this.userDAO = new UserDAO();
        this.organizationDAO = new OrganizationDAO();
        this.systemSettingsDAO = new SystemSettingsDAO();
    }

    public UserServiceImpl(UserDAO userDAO, OrganizationDAO organizationDAO, SystemSettingsDAO systemSettingsDAO) {
        this.userDAO = userDAO;
        this.organizationDAO = organizationDAO;
        this.systemSettingsDAO = systemSettingsDAO;
    }

    @Override
    public User authenticate(String email, String plainPassword)
            throws AuthenticationException, DatabaseException, ValidationException {

        ValidationUtil.validateEmail(email);
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new ValidationException("password", "Password cannot be empty.");
        }

        Optional<User> userOpt = userDAO.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Invalid email or password.");
        }

        User user = userOpt.get();

        if (!PasswordHasher.verify(plainPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password.");
        }

        if (user.getAccountStatus() == AccountStatus.INACTIVE) {
            throw new AuthenticationException("Your account is currently inactive. Please contact an administrator.");
        }

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new AuthenticationException("Your account has been suspended due to policy violations.");
        }

        return user;
    }

    @Override
    public User registerVolunteer(String fullName, String email, String plainPassword)
            throws ValidationException, DuplicateResourceException, DatabaseException {

        ValidationUtil.validateNotEmpty(fullName, "fullName", "Full name");
        ValidationUtil.validateEmail(email);
        ValidationUtil.validatePassword(plainPassword);

        if (userDAO.findByEmail(email.trim().toLowerCase()).isPresent()) {
            throw new DuplicateResourceException("An account with email '" + email + "' already exists.");
        }

        String passwordHash = PasswordHasher.hash(plainPassword);
        Volunteer volunteer = new Volunteer(null, fullName.trim(), email.trim().toLowerCase(), passwordHash, AccountStatus.ACTIVE);

        return userDAO.create(volunteer);
    }

    @Override
    public Organization registerOrganization(String fullName, String email, String plainPassword,
                                             String orgName, String description, String contactInfo)
            throws ValidationException, DuplicateResourceException, DatabaseException {

        ValidationUtil.validateNotEmpty(fullName, "fullName", "Representative name");
        ValidationUtil.validateNotEmpty(orgName, "orgName", "Organization name");
        ValidationUtil.validateEmail(email);
        ValidationUtil.validatePassword(plainPassword);

        if (userDAO.findByEmail(email.trim().toLowerCase()).isPresent()) {
            throw new DuplicateResourceException("An account with email '" + email + "' already exists.");
        }

        // Check if platform requires manual approval for organization signups
        boolean requiresApproval = false;
        try {
            Optional<String> approvalSetting = systemSettingsDAO.getSetting("org_registration_approval_required");
            if (approvalSetting.isPresent()) {
                requiresApproval = Boolean.parseBoolean(approvalSetting.get());
            }
        } catch (DatabaseException ignored) {
        }

        AccountStatus initialStatus = requiresApproval ? AccountStatus.INACTIVE : AccountStatus.ACTIVE;
        String passwordHash = PasswordHasher.hash(plainPassword);

        Organization org = new Organization();
        org.setFullName(fullName.trim());
        org.setEmail(email.trim().toLowerCase());
        org.setPasswordHash(passwordHash);
        org.setRole(Role.ORGANIZATION);
        org.setAccountStatus(initialStatus);
        org.setOrganizationName(orgName.trim());
        org.setDescription(description != null ? description.trim() : "");
        org.setContactInformation(contactInfo != null ? contactInfo.trim() : email.trim());

        User createdUser = userDAO.create(org);
        org.setId(createdUser.getId());

        organizationDAO.create(org);
        return org;
    }

    @Override
    public User createUser(User initiator, String fullName, String email, String plainPassword, Role role)
            throws UnauthorizedException, ValidationException, DuplicateResourceException, DatabaseException {

        if (initiator == null || !initiator.isAdmin()) {
            throw new UnauthorizedException("Only system administrators are authorized to create user accounts directly.");
        }

        ValidationUtil.validateNotEmpty(fullName, "fullName", "Full name");
        ValidationUtil.validateEmail(email);
        ValidationUtil.validatePassword(plainPassword);
        if (role == null) {
            throw new ValidationException("role", "Role must be specified.");
        }

        if (userDAO.findByEmail(email.trim().toLowerCase()).isPresent()) {
            throw new DuplicateResourceException("An account with email '" + email + "' already exists.");
        }

        String hash = PasswordHasher.hash(plainPassword);

        User newUser;
        switch (role) {
            case ADMIN -> newUser = new Admin(null, fullName.trim(), email.trim().toLowerCase(), hash, AccountStatus.ACTIVE);
            case ORGANIZATION -> newUser = new Organization(null, fullName.trim(), email.trim().toLowerCase(), hash, AccountStatus.ACTIVE,
                                                            null, fullName.trim(), "", email.trim());
            case VOLUNTEER -> newUser = new Volunteer(null, fullName.trim(), email.trim().toLowerCase(), hash, AccountStatus.ACTIVE);
            default -> throw new ValidationException("role", "Unsupported role type: " + role);
        }

        User created = userDAO.create(newUser);
        if (created instanceof Organization org) {
            org.setId(created.getId());
            organizationDAO.create(org);
        }
        return created;
    }

    @Override
    public Optional<User> getUserById(Long id) throws DatabaseException {
        if (id == null) return Optional.empty();
        return userDAO.findById(id);
    }

    @Override
    public Optional<User> getUserByEmail(String email) throws DatabaseException {
        if (email == null) return Optional.empty();
        return userDAO.findByEmail(email.trim().toLowerCase());
    }

    @Override
    public List<User> getAllUsers(User initiator) throws UnauthorizedException, DatabaseException {
        if (initiator == null || !initiator.isAdmin()) {
            throw new UnauthorizedException("Only administrators can view the complete user roster.");
        }
        return userDAO.findAll();
    }

    @Override
    public List<User> searchUsers(User initiator, String keyword, Role roleFilter, AccountStatus statusFilter)
            throws UnauthorizedException, DatabaseException {

        if (initiator == null || !initiator.isAdmin()) {
            throw new UnauthorizedException("Only administrators can search and filter the full user directory.");
        }
        return userDAO.search(keyword, roleFilter, statusFilter);
    }

    @Override
    public void updateUser(User initiator, User updatedUser)
            throws UnauthorizedException, ValidationException, DatabaseException {

        if (initiator == null || updatedUser == null) {
            throw new ValidationException("user", "User details cannot be null.");
        }

        // Only Admin or the user themselves can update their details
        boolean isSelf = initiator.getId() != null && initiator.getId().equals(updatedUser.getId());
        if (!initiator.isAdmin() && !isSelf) {
            throw new UnauthorizedException("You are not authorized to update another user's profile.");
        }

        ValidationUtil.validateNotEmpty(updatedUser.getFullName(), "fullName", "Full name");
        ValidationUtil.validateEmail(updatedUser.getEmail());

        userDAO.update(updatedUser);

        if (updatedUser instanceof Organization org && org.getOrganizationId() != null) {
            organizationDAO.update(org);
        }
    }

    @Override
    public void setUserAccountStatus(User initiator, Long targetUserId, AccountStatus newStatus)
            throws UnauthorizedException, ValidationException, DatabaseException {

        if (initiator == null || !initiator.isAdmin()) {
            throw new UnauthorizedException("Only administrators can modify user account statuses.");
        }
        if (targetUserId == null || newStatus == null) {
            throw new ValidationException("status", "Target user ID and new status must be provided.");
        }

        Optional<User> targetOpt = userDAO.findById(targetUserId);
        if (targetOpt.isEmpty()) {
            throw new ValidationException("targetUser", "Target user #" + targetUserId + " not found.");
        }

        User targetUser = targetOpt.get();

        // Protect the last active administrator from accidental deletion or deactivation
        if (targetUser.isAdmin() && newStatus != AccountStatus.ACTIVE) {
            int activeAdmins = userDAO.countActiveAdmins();
            if (activeAdmins <= 1) {
                throw new ValidationException("accountStatus", "Safety Guard: Cannot deactivate or suspend the last active administrator account.");
            }
        }

        userDAO.updateAccountStatus(targetUserId, newStatus);
    }
}
