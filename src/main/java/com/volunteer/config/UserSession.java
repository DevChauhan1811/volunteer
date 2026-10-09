package com.volunteer.config;

import com.volunteer.model.User;

import java.time.LocalDateTime;

/**
 * Thread-safe global session manager holding the currently authenticated user.
 */
public final class UserSession {

    private static User currentUser;
    private static LocalDateTime loginTime;

    private UserSession() {
    }

    public static synchronized void login(User user) {
        currentUser = user;
        loginTime = LocalDateTime.now();
    }

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public static synchronized LocalDateTime getLoginTime() {
        return loginTime;
    }

    public static synchronized void logout() {
        currentUser = null;
        loginTime = null;
    }
}
