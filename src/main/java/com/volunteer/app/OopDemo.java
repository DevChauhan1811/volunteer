package com.volunteer.app;

import com.volunteer.model.Admin;
import com.volunteer.model.Organization;
import com.volunteer.model.User;
import com.volunteer.model.Volunteer;
import com.volunteer.model.enums.AccountStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic and evaluation demonstration class proving Criterion 1 (OOP Implementation):
 * 1. Inheritance: User is an abstract base class extended by Admin, Organization, and Volunteer.
 * 2. Polymorphism: Base class references (User u) dynamically invoke overridden methods at runtime.
 * 3. Collections & Generics: List<User> polymorphism traversal.
 */
public class OopDemo {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  CRITERION 1: OOP IMPLEMENTATION & POLYMORPHISM DEMO");
        System.out.println("==========================================================");

        // Demonstrating Generics with List<User> storing polymorphically varied subclasses
        List<User> userRoster = new ArrayList<>();

        // Instantiate subclasses via parent User references
        User adminUser = new Admin(1L, "Sarah Connor", "admin@platform.com", "hash_admin", AccountStatus.ACTIVE);
        User orgUser = new Organization(2L, "EcoAction Society", "contact@ecoaction.org", "hash_org", AccountStatus.ACTIVE,
                                       10L, "EcoAction Society", "Community environmental protection", "contact@ecoaction.org");
        User volUser = new Volunteer(3L, "David Miller", "david@example.com", "hash_vol", AccountStatus.ACTIVE);

        userRoster.add(adminUser);
        userRoster.add(orgUser);
        userRoster.add(volUser);

        // Runtime Polymorphism Demonstration:
        // Notice how calling u.getDashboardWelcomeMessage() executes the SUBCLASS's specific implementation!
        for (User u : userRoster) {
            System.out.println("\n[Polymorphic Type: " + u.getClass().getSimpleName() + " (Role: " + u.getRole() + ")]");
            System.out.println("  > Welcome Msg:  \"" + u.getDashboardWelcomeMessage() + "\"");
            System.out.println("  > Badge Color:  " + u.getRoleBadgeColorHex());
            System.out.println("  > Capabilities: " + u.getPermittedCapabilities());

            // Testing role capability queries polymorphically
            System.out.println("  > Can Manage Users?          " + u.canPerformAction("MANAGE_USERS"));
            System.out.println("  > Can Post Opportunities?    " + u.canPerformAction("CREATE_OPPORTUNITY"));
            System.out.println("  > Can Browse Opportunities?  " + u.canPerformAction("BROWSE_OPPORTUNITIES"));
        }

        System.out.println("\n==========================================================");
        System.out.println("  Polymorphic dynamic dispatch executed successfully!");
        System.out.println("==========================================================");
    }
}
