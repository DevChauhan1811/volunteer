package com.volunteer.service;

import com.volunteer.exception.AuthenticationException;
import com.volunteer.exception.DuplicateResourceException;
import com.volunteer.exception.UnauthorizedException;
import com.volunteer.exception.ValidationException;
import com.volunteer.model.Admin;
import com.volunteer.model.Opportunity;
import com.volunteer.model.Organization;
import com.volunteer.model.User;
import com.volunteer.model.Volunteer;
import com.volunteer.model.enums.AccountStatus;
import com.volunteer.model.enums.ApprovalStatus;
import com.volunteer.model.enums.Role;
import com.volunteer.service.impl.OpportunityServiceImpl;
import com.volunteer.service.impl.SignupServiceImpl;
import com.volunteer.service.impl.SystemSettingsServiceImpl;
import com.volunteer.service.impl.UserServiceImpl;
import com.volunteer.service.impl.VolunteerHoursServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 Automated Test Suite validating business rules, authentication,
 * authorization guards, capacity enforcement, and validation constraints.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BusinessRulesTest {

    private static UserService userService;
    private static OpportunityService oppService;
    private static SignupService signupService;
    private static VolunteerHoursService hoursService;
    private static SystemSettingsService settingsService;

    @BeforeAll
    public static void setUp() {
        userService = new UserServiceImpl();
        oppService = new OpportunityServiceImpl();
        signupService = new SignupServiceImpl();
        hoursService = new VolunteerHoursServiceImpl();
        settingsService = new SystemSettingsServiceImpl();
    }

    @Test
    @Order(1)
    @DisplayName("Authentication: Valid credentials succeed and return correct User subtype")
    public void testAuthenticationSuccess() throws Exception {
        User admin = userService.authenticate("admin@platform.com", "Password@123");
        assertNotNull(admin);
        assertEquals(Role.ADMIN, admin.getRole());
        assertInstanceOf(Admin.class, admin);

        User volunteer = userService.authenticate("alice.johnson@example.com", "Password@123");
        assertNotNull(volunteer);
        assertEquals(Role.VOLUNTEER, volunteer.getRole());
        assertInstanceOf(Volunteer.class, volunteer);
    }

    @Test
    @Order(2)
    @DisplayName("Authentication: Invalid password throws AuthenticationException")
    public void testAuthenticationInvalidPassword() {
        assertThrows(AuthenticationException.class, () -> {
            userService.authenticate("admin@platform.com", "WrongPassword999!");
        });
    }

    @Test
    @Order(3)
    @DisplayName("Authentication: Nonexistent email throws AuthenticationException")
    public void testAuthenticationNonexistentEmail() {
        assertThrows(AuthenticationException.class, () -> {
            userService.authenticate("nonexistent_user@platform.com", "Password@123");
        });
    }

    @Test
    @Order(4)
    @DisplayName("Registration: Duplicate email registration is prevented")
    public void testDuplicateEmailRegistrationThrows() {
        assertThrows(DuplicateResourceException.class, () -> {
            userService.registerVolunteer("Duplicate User", "admin@platform.com", "Password@123");
        });
    }

    @Test
    @Order(5)
    @DisplayName("Authorization Guard: Volunteer cannot moderate opportunities")
    public void testVolunteerCannotModerateOpportunity() throws Exception {
        User volunteer = userService.getUserByEmail("alice.johnson@example.com").orElseThrow();

        assertThrows(UnauthorizedException.class, () -> {
            oppService.moderateOpportunity(volunteer, 1L, ApprovalStatus.APPROVED, null);
        });
    }

    @Test
    @Order(6)
    @DisplayName("Authorization Guard: Volunteer cannot view full admin user list")
    public void testVolunteerCannotViewAllUsers() throws Exception {
        User volunteer = userService.getUserByEmail("alice.johnson@example.com").orElseThrow();

        assertThrows(UnauthorizedException.class, () -> {
            userService.getAllUsers(volunteer);
        });
    }

    @Test
    @Order(7)
    @DisplayName("Opportunity Moderation: Rejecting an opportunity requires a rejection reason")
    public void testOpportunityRejectionRequiresReason() throws Exception {
        User admin = userService.getUserByEmail("admin@platform.com").orElseThrow();

        assertThrows(ValidationException.class, () -> {
            oppService.moderateOpportunity(admin, 1L, ApprovalStatus.REJECTED, "   ");
        });
    }

    @Test
    @Order(8)
    @DisplayName("Opportunity Creation: New opportunities start in PENDING status")
    public void testNewOpportunityStartsPending() throws Exception {
        User orgUser = userService.getUserByEmail("contact@greenearth.org").orElseThrow();

        Opportunity opp = new Opportunity();
        opp.setTitle("JUnit Automated Test Cleanup Event");
        opp.setDescription("Testing opportunity creation business rules.");
        opp.setCategory("Environment");
        opp.setEventDate(LocalDate.now().plusDays(10));
        opp.setStartTime(LocalTime.of(10, 0));
        opp.setEndTime(LocalTime.of(12, 0));
        opp.setLocation("Central Plaza");
        opp.setCapacity(15);

        Opportunity created = oppService.postOpportunity(orgUser, opp);
        assertNotNull(created.getId());
        assertEquals(ApprovalStatus.PENDING, created.getApprovalStatus(), "New opportunities must default to PENDING");
    }

    @Test
    @Order(9)
    @DisplayName("Safety Guard: Cannot deactivate the last active administrator account")
    public void testProtectLastActiveAdmin() throws Exception {
        User admin = userService.getUserByEmail("admin@platform.com").orElseThrow();

        assertThrows(ValidationException.class, () -> {
            userService.setUserAccountStatus(admin, admin.getId(), AccountStatus.INACTIVE);
        });
    }

    @Test
    @Order(10)
    @DisplayName("Validation: Invalid email formats are rejected")
    public void testInvalidEmailFormatsRejected() {
        assertThrows(ValidationException.class, () -> {
            userService.registerVolunteer("Test Name", "not-a-valid-email", "Password@123");
        });
    }
}
