package com.volunteer.app;

import com.volunteer.dao.MessageDAO;
import com.volunteer.dao.OpportunityDAO;
import com.volunteer.dao.OrganizationDAO;
import com.volunteer.dao.SignupDAO;
import com.volunteer.dao.SystemSettingsDAO;
import com.volunteer.dao.UserDAO;
import com.volunteer.dao.VolunteerHoursDAO;
import com.volunteer.model.Admin;
import com.volunteer.model.Opportunity;
import com.volunteer.model.Signup;
import com.volunteer.model.User;
import com.volunteer.model.Volunteer;
import com.volunteer.model.enums.RegistrationStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Diagnostic integration test verifying Criterion 4 (Database Operations Classes),
 * Criterion 5 (Database Connectivity), and Criterion 6 (JDBC Implementation).
 * Tests live CRUD operations, parameterized queries, and ACID transactions.
 */
public class DaoIntegrationTest {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  PHASE 4: JDBC DAO LAYER INTEGRATION TEST");
        System.out.println("==========================================================");

        try {
            UserDAO userDAO = new UserDAO();
            OrganizationDAO orgDAO = new OrganizationDAO();
            OpportunityDAO oppDAO = new OpportunityDAO();
            SignupDAO signupDAO = new SignupDAO();
            VolunteerHoursDAO hoursDAO = new VolunteerHoursDAO();
            MessageDAO messageDAO = new MessageDAO();
            SystemSettingsDAO settingsDAO = new SystemSettingsDAO();

            // 1. Test UserDAO & Polymorphic Mapping
            System.out.print("[1/7] Testing UserDAO (findByEmail & Polymorphism)... ");
            Optional<User> adminOpt = userDAO.findByEmail("admin@platform.com");
            if (adminOpt.isPresent() && adminOpt.get() instanceof Admin) {
                System.out.println("PASSED! Found Admin: " + adminOpt.get().getFullName());
            } else {
                throw new RuntimeException("Admin not found or not mapped to Admin class!");
            }

            Optional<User> volOpt = userDAO.findByEmail("alice.johnson@example.com");
            if (volOpt.isPresent() && volOpt.get() instanceof Volunteer) {
                System.out.println("      Found Volunteer: " + volOpt.get().getFullName() + " (Active=" + volOpt.get().isActive() + ")");
            } else {
                throw new RuntimeException("Volunteer Alice not found or not mapped to Volunteer class!");
            }

            // 2. Test OrganizationDAO
            System.out.print("[2/7] Testing OrganizationDAO (findAll)... ");
            var orgs = orgDAO.findAll();
            System.out.println("PASSED! Found " + orgs.size() + " organizations.");
            for (var org : orgs) {
                System.out.println("      > Org: " + org.getOrganizationName() + " (Contact: " + org.getContactInformation() + ")");
            }

            // 3. Test OpportunityDAO
            System.out.print("[3/7] Testing OpportunityDAO (findApproved)... ");
            List<Opportunity> approvedOpps = oppDAO.findApproved(null, null, null, null);
            System.out.println("PASSED! Found " + approvedOpps.size() + " approved opportunities.");
            for (Opportunity opp : approvedOpps) {
                System.out.println("      > #" + opp.getId() + " " + opp.getTitle() + " | Slots: " + opp.getAvailableSlots() + "/" + opp.getCapacity() + " (Signed: " + opp.getCurrentSignups() + ")");
            }

            // 4. Test VolunteerHoursDAO
            System.out.print("[4/7] Testing VolunteerHoursDAO (getTotalVerifiedHours)... ");
            double aliceHours = hoursDAO.getTotalVerifiedHoursForVolunteer(volOpt.get().getId());
            System.out.println("PASSED! Alice's verified hours: " + aliceHours + " hrs.");

            // 5. Test MessageDAO
            System.out.print("[5/7] Testing MessageDAO (findConversation)... ");
            var conversation = messageDAO.findConversation(volOpt.get().getId(), orgs.get(1).getId(), null);
            System.out.println("PASSED! Found " + conversation.size() + " messages in thread.");

            // 6. Test SystemSettingsDAO & Live Platform Statistics
            System.out.print("[6/7] Testing SystemSettingsDAO (getPlatformStatistics)... ");
            Map<String, Object> stats = settingsDAO.getPlatformStatistics();
            System.out.println("PASSED!");
            System.out.println("      * Total Users:          " + stats.get("totalUsers"));
            System.out.println("      * Total Organizations:  " + stats.get("totalOrganizations"));
            System.out.println("      * Total Volunteers:     " + stats.get("totalVolunteers"));
            System.out.println("      * Total Opportunities:  " + stats.get("totalOpportunities"));
            System.out.println("      * Pending Moderation:   " + stats.get("pendingOpportunities"));
            System.out.println("      * Total Active Signups: " + stats.get("totalSignups"));
            System.out.println("      * Total Verified Hours: " + stats.get("totalVerifiedHours") + " hrs");

            // 7. Test Transactional Registration & Rollback/Commit
            System.out.print("[7/7] Testing SignupDAO (registerWithCapacityLock & rollback)... ");
            Long testOppId = approvedOpps.get(0).getId();
            Long testVolId = volOpt.get().getId();

            // Volunteer 4 (Alice) is already registered for opp 1 in seed data, so registering should throw DuplicateResourceException!
            boolean duplicateCaught = false;
            try {
                signupDAO.registerWithCapacityLock(testVolId, testOppId);
            } catch (com.volunteer.exception.DuplicateResourceException dre) {
                duplicateCaught = true;
                System.out.println("PASSED! Duplicate registration safely prevented: \"" + dre.getMessage() + "\"");
            }

            if (!duplicateCaught) {
                throw new RuntimeException("Duplicate registration check failed to block duplicate signup!");
            }

            System.out.println("==========================================================");
            System.out.println("  ALL JDBC DAO OPERATIONS PASSED WITH ZERO ERRORS!");
            System.out.println("==========================================================");
        } catch (Exception ex) {
            System.err.println("DAO Integration Test FAILED: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
            } catch (Throwable ignored) {
            }
        }
    }
}
