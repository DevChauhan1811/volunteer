package com.volunteer.service;

import com.volunteer.exception.AuthenticationException;
import com.volunteer.exception.DatabaseException;
import com.volunteer.exception.DuplicateResourceException;
import com.volunteer.exception.UnauthorizedException;
import com.volunteer.exception.ValidationException;
import com.volunteer.model.Organization;
import com.volunteer.model.User;
import com.volunteer.model.enums.AccountStatus;
import com.volunteer.model.enums.Role;

import java.util.List;
import java.util.Optional;

/**
 * Service contract for user authentication, registration, and administrative user management.
 */
public interface UserService {

    /**
     * Authenticates a user using email and plaintext password against BCrypt hashes.
     */
    User authenticate(String email, String plainPassword)
        throws AuthenticationException, DatabaseException, ValidationException;

    /**
     * Registers a new volunteer account.
     */
    User registerVolunteer(String fullName, String email, String plainPassword)
        throws ValidationException, DuplicateResourceException, DatabaseException;

    /**
     * Registers a new organization account and associated organization profile.
     */
    Organization registerOrganization(String fullName, String email, String plainPassword,
                                      String orgName, String description, String contactInfo)
        throws ValidationException, DuplicateResourceException, DatabaseException;

    /**
     * Creates a user account directly (Admin feature).
     */
    User createUser(User initiator, String fullName, String email, String plainPassword, Role role)
        throws UnauthorizedException, ValidationException, DuplicateResourceException, DatabaseException;

    /**
     * Finds a user by identifier.
     */
    Optional<User> getUserById(Long id) throws DatabaseException;

    /**
     * Finds a user by email.
     */
    Optional<User> getUserByEmail(String email) throws DatabaseException;

    /**
     * Retrieves all users (for Admin table).
     */
    List<User> getAllUsers(User initiator) throws UnauthorizedException, DatabaseException;

    /**
     * Searches and filters users by keyword, role, and account status.
     */
    List<User> searchUsers(User initiator, String keyword, Role roleFilter, AccountStatus statusFilter)
        throws UnauthorizedException, DatabaseException;

    /**
     * Updates permitted user profile details.
     */
    void updateUser(User initiator, User updatedUser)
        throws UnauthorizedException, ValidationException, DatabaseException;

    /**
     * Activates or deactivates a user account, protecting the last active administrator.
     */
    void setUserAccountStatus(User initiator, Long targetUserId, AccountStatus newStatus)
        throws UnauthorizedException, ValidationException, DatabaseException;
}
