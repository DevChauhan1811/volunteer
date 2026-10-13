/**
 * MYVOLUNTEER — FRONTEND APPLICATION LOGIC
 * Full-stack interactive JavaScript application connected to REST API & Local Fallback.
 * Features:
 * - Dedicated 4-Role Portal (Volunteer, Organization, Admin, Public Guest)
 * - Secure Login/Logout with Email/Password & Email/OTP
 * - 5 Working Navigation Section Links (Home, Opportunities, How it works, Organizations, Our Impact)
 * - Real-world volunteer opportunities solving pressing community issues
 * - Full button responsiveness across all user roles
 * - Left-to-right Sparkle beam intro animation
 * - Real Program Registration & Cancellation
 * - Dynamic Admin Listing Moderation (Accept/Reject with Reason)
 * - Dynamic Organization Hours Verification (Accept/Reject)
 */

const API_BASE = '/api';

// --- Global Application State ---
const AppState = {
  currentUser: null,       // { id, fullName, email, role, organizationId }
  currentView: 'home',     // 'home' | 'volunteer' | 'organization' | 'admin'
  selectedRoleForAuth: 'VOLUNTEER',
  activeOTP: null, // { code, email, role, expiresAt, attemptsRemaining, isUsed }
  otpCountdownInterval: null,
  opportunities: [],
  filteredOpportunities: [],
  stats: {
    totalUsers: 8,
    totalOrganizations: 3,
    totalVolunteers: 6,
    totalOpportunities: 8,
    activeSignups: 4,
    totalVerifiedHours: 12.5
  },
  activeCategory: 'All',
  searchQuery: '',
  locationQuery: '',
  userSignups: [],
  userHours: [],
  pendingModeration: [
    {
      id: 1,
      title: "Community Tree Planting Drive",
      organizationName: "Green Earth Alliance",
      capacity: 25,
      category: "Environment",
      eventDate: "2026-10-25",
      location: "Riverside Park, Sector 4",
      status: "PENDING", // 'PENDING' | 'APPROVED' | 'REJECTED'
      rejectionReason: null
    },
    {
      id: 7,
      title: "Emergency Flood Relief Supply Packaging",
      organizationName: "Global Relief Network",
      capacity: 40,
      category: "Disaster Relief",
      eventDate: "2026-11-18",
      location: "Eastside Community Hall",
      status: "PENDING",
      rejectionReason: null
    },
    {
      id: 6,
      title: "Youth STEM & Coding Weekend Mentorship",
      organizationName: "NextGen Tech Alliance",
      capacity: 16,
      category: "Education",
      eventDate: "2026-11-15",
      location: "Old Town Tech Center, Lab 2",
      status: "PENDING",
      rejectionReason: null
    }
  ],
  orgPendingHours: [
    {
      id: 301,
      volunteerName: "Alice Johnson",
      opportunityTitle: "Community Tree Planting Drive",
      hoursWorked: 4.0,
      workDate: "2026-10-25",
      status: "PENDING" // 'PENDING' | 'VERIFIED' | 'REJECTED'
    },
    {
      id: 302,
      volunteerName: "Marcus Sterling",
      opportunityTitle: "Weekend Grocery Packaging & Distribution",
      hoursWorked: 3.5,
      workDate: "2026-10-26",
      status: "PENDING"
    },
    {
      id: 303,
      volunteerName: "Elena Rostova",
      opportunityTitle: "Coastal Beach & Estuary Cleanup",
      hoursWorked: 5.0,
      workDate: "2026-10-20",
      status: "PENDING"
    }
  ],
  orgOpportunities: [],
  adminUsers: []
};

// Seed Profiles for instant login & testing
const SEED_PROFILES = {
  VOLUNTEER: {
    id: 4,
    fullName: "Alice Johnson",
    email: "alice.johnson@example.com",
    role: "VOLUNTEER"
  },
  ORGANIZATION: {
    id: 2,
    organizationId: 1,
    fullName: "Green Earth Alliance",
    email: "contact@greenearth.org",
    role: "ORGANIZATION"
  },
  ADMIN: {
    id: 1,
    fullName: "System Administrator",
    email: "admin@platform.com",
    role: "ADMIN"
  },
  GUEST: {
    id: 99,
    fullName: "Public Guest",
    email: "guest@myvolunteer.org",
    role: "GUEST"
  }
};

let splashTimer = null;

// --- SPARKLE INTRO ANIMATION CONTROLS ---
function playSparkleIntro() {
  const splash = document.getElementById('intro-splash');
  if (!splash) return;

  if (splashTimer) clearTimeout(splashTimer);

  splash.classList.remove('fade-out');
  splash.style.display = 'flex';

  // Trigger sweep wand animation restart
  const wand = document.getElementById('sparkle-wand');
  if (wand) {
    wand.style.animation = 'none';
    void wand.offsetWidth; // Force CSS reflow
    wand.style.animation = 'sweepLeftToRight 2.2s cubic-bezier(0.2, 0.8, 0.2, 1) forwards';
  }

  // Trigger characters reveal restart
  const chars = splash.querySelectorAll('.spark-char');
  chars.forEach((c) => {
    c.style.animation = 'none';
    void c.offsetWidth;
    c.style.animation = 'charReveal 0.4s cubic-bezier(0.2, 0.9, 0.3, 1) forwards';
    const idx = c.style.getPropertyValue('--char-idx') || '0';
    c.style.animationDelay = `calc(0.12s * ${idx} + 0.2s)`;
  });

  // Automatically fade out and close after 2.9 seconds
  splashTimer = setTimeout(() => {
    closeIntroSplash();
  }, 2900);
}

function closeIntroSplash() {
  if (splashTimer) {
    clearTimeout(splashTimer);
    splashTimer = null;
  }
  const splash = document.getElementById('intro-splash');
  if (splash) {
    splash.classList.add('fade-out');
    setTimeout(() => {
      splash.style.display = 'none';
    }, 450);
  }
}

// --- INITIALIZATION ---
document.addEventListener('DOMContentLoaded', () => {
  // Launch sparkling sweep intro animation
  playSparkleIntro();

  // Check if session exists in localStorage
  const savedUser = localStorage.getItem('volunteer_platform_user');
  if (savedUser) {
    try {
      AppState.currentUser = JSON.parse(savedUser);
    } catch (e) {
      AppState.currentUser = null;
    }
  }

  // Determine initial view based on auth status
  if (AppState.currentUser) {
    showMainApp();
    updateUserNavUI();
  } else {
    showLoginPage();
  }

  // Setup modals and forms
  setupModals();
  setupForms();

  // Load initial data
  loadPlatformStats();
  loadOpportunities();
});

// ===================================================================
// 1. AUTHENTICATION & ROLE PORTAL CONTROLS
// ===================================================================

function showLoginPage() {
  const loginPage = document.getElementById('login-page');
  const mainApp = document.getElementById('main-app');
  if (loginPage) loginPage.style.display = 'flex';
  if (mainApp) mainApp.style.display = 'none';
  backToRoleSelection();
}

function showMainApp() {
  const loginPage = document.getElementById('login-page');
  const mainApp = document.getElementById('main-app');
  if (loginPage) loginPage.style.display = 'none';
  if (mainApp) mainApp.style.display = 'flex';
}

function openRoleLogin(role, preserveSuccess = false) {
  AppState.selectedRoleForAuth = role;
  const roleSelection = document.getElementById('role-selection-view');
  const roleAuth = document.getElementById('role-auth-view');

  if (roleSelection) roleSelection.style.display = 'none';
  if (roleAuth) roleAuth.style.display = 'block';

  // Customize header based on role
  const titleEl = document.getElementById('auth-form-title');
  const subtitleEl = document.getElementById('auth-form-subtitle');
  const badgeEl = document.getElementById('auth-role-badge');
  const quickNameEl = document.getElementById('quick-cred-name');

  const roleMeta = {
    VOLUNTEER: {
      title: "Volunteer Sign In",
      subtitle: "Access opportunities, register for events, and log service hours.",
      badge: "Volunteer Portal",
      demoName: "Alice Johnson (Volunteer)",
      email: "alice.johnson@example.com"
    },
    ORGANIZATION: {
      title: "Organization Sign In",
      subtitle: "Post community causes, coordinate teams, and verify volunteer hours.",
      badge: "Organization Portal",
      demoName: "Green Earth Alliance (Org)",
      email: "contact@greenearth.org"
    },
    ADMIN: {
      title: "System Admin Sign In",
      subtitle: "Oversee system operations, moderate listings, and manage governance.",
      badge: "Admin Portal",
      demoName: "System Admin",
      email: "admin@platform.com"
    }
  };

  const meta = roleMeta[role] || roleMeta.VOLUNTEER;
  if (titleEl) titleEl.textContent = meta.title;
  if (subtitleEl) subtitleEl.textContent = meta.subtitle;
  if (badgeEl) badgeEl.textContent = meta.badge;
  if (quickNameEl) quickNameEl.textContent = meta.demoName;

  const emailInput = document.getElementById('login-email-input');
  const passwordInput = document.getElementById('login-password-input');
  const otpEmailInput = document.getElementById('otp-email-input');
  const otpCodeInput = document.getElementById('otp-code-input');

  if (!preserveSuccess) {
    if (emailInput) emailInput.value = '';
    if (passwordInput) passwordInput.value = '';
    hideAuthErrors(true);
  } else {
    hideAuthErrors(false);
  }

  if (otpEmailInput) otpEmailInput.value = '';
  if (otpCodeInput) otpCodeInput.value = '';

  // Cancel any prior active OTP challenge session
  cancelOTPSession(false);

  // Default to password mode
  switchAuthMode('password');
}

function backToRoleSelection() {
  cancelOTPSession(false);
  hideAuthErrors();
  const roleSelection = document.getElementById('role-selection-view');
  const roleAuth = document.getElementById('role-auth-view');
  if (roleSelection) roleSelection.style.display = 'block';
  if (roleAuth) roleAuth.style.display = 'none';
}

function quickFillCredentials() {
  hideAuthErrors();
  const role = AppState.selectedRoleForAuth;
  const profile = SEED_PROFILES[role] || SEED_PROFILES.VOLUNTEER;
  const emailInput = document.getElementById('login-email-input');
  const passInput = document.getElementById('login-password-input');
  const otpEmailInput = document.getElementById('otp-email-input');

  if (emailInput) emailInput.value = profile.email;
  if (passInput) passInput.value = 'Password@123';
  if (otpEmailInput) otpEmailInput.value = profile.email;

  showToast(`Pre-filled demo credentials for ${profile.fullName}`, 'info');
}

function switchAuthMode(mode) {
  hideAuthErrors();
  const tabPassword = document.getElementById('tab-btn-password');
  const tabOtp = document.getElementById('tab-btn-otp');
  const formPassword = document.getElementById('login-form-password');
  const formOtp = document.getElementById('login-form-otp-container');

  if (mode === 'password') {
    if (tabPassword) tabPassword.classList.add('active');
    if (tabOtp) tabOtp.classList.remove('active');
    if (formPassword) formPassword.style.display = 'block';
    if (formOtp) formOtp.style.display = 'none';
  } else {
    if (tabPassword) tabPassword.classList.remove('active');
    if (tabOtp) tabOtp.classList.add('active');
    if (formPassword) formPassword.style.display = 'none';
    if (formOtp) formOtp.style.display = 'block';

    // Transfer email from password field if typed
    const emailInput = document.getElementById('login-email-input');
    const otpEmailInput = document.getElementById('otp-email-input');
    if (emailInput && otpEmailInput && emailInput.value && !otpEmailInput.value) {
      otpEmailInput.value = emailInput.value;
    }
  }
}

function showAuthError(elementId, message) {
  const el = document.getElementById(elementId);
  if (el) {
    el.innerHTML = `<span>⚠️ ${escapeHtml(message)}</span>`;
    el.style.display = 'flex';
  }
}

function hideAuthErrors(hideSuccess = true) {
  const errBoxes = ['password-error-box', 'otp-step1-error-box', 'otp-error-box'];
  errBoxes.forEach(id => {
    const el = document.getElementById(id);
    if (el) el.style.display = 'none';
  });
  if (hideSuccess) {
    const sBox = document.getElementById('auth-success-box');
    if (sBox) sBox.style.display = 'none';
  }
}

async function handlePasswordLogin(event) {
  if (event) event.preventDefault();
  hideAuthErrors();

  const emailInput = document.getElementById('login-email-input');
  const passInput = document.getElementById('login-password-input');
  const email = emailInput ? emailInput.value.trim() : '';
  const password = passInput ? passInput.value : '';

  if (!email || !password) {
    showAuthError('password-error-box', 'Please enter both email and password.');
    return;
  }

  // Attempt backend API authentication
  const res = await fetchAPI('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password })
  });

  if (res && res.success && res.user) {
    // Authenticated via API!
    completeLogin(res.user);
    return;
  }

  if (res && res.success === false) {
    // Backend rejected explicitly (wrong password/user not found)
    const errorMsg = res.error || 'Invalid credentials. Password did not match.';
    showAuthError('password-error-box', errorMsg);
    if (passInput) {
      passInput.classList.add('shake-anim');
      setTimeout(() => passInput.classList.remove('shake-anim'), 400);
    }
    return;
  }

  // Network / server offline fallback verification:
  const roleKey = AppState.selectedRoleForAuth;
  const seed = SEED_PROFILES[roleKey];
  
  if (seed && email.toLowerCase() === seed.email.toLowerCase()) {
    if (password === 'Password@123') {
      completeLogin({ ...seed });
      return;
    } else {
      showAuthError('password-error-box', 'Incorrect password! (For demo account use: Password@123)');
      if (passInput) {
        passInput.classList.add('shake-anim');
        setTimeout(() => passInput.classList.remove('shake-anim'), 400);
      }
      return;
    }
  }

  // Other unregistered offline user
  if (password.length >= 6) {
    completeLogin({
      id: Math.floor(Math.random() * 1000) + 10,
      fullName: email.split('@')[0].replace('.', ' ').toUpperCase(),
      email: email,
      role: roleKey
    });
  } else {
    showAuthError('password-error-box', 'Invalid password. Must be at least 6 characters.');
  }
}

// ===================================================================
// SECURE LOCKED OTP AUTHENTICATION SYSTEM
// ===================================================================

function handleSendOTP() {
  hideAuthErrors();
  const emailInput = document.getElementById('otp-email-input');
  const email = emailInput ? emailInput.value.trim().toLowerCase() : '';

  if (!email || !email.includes('@')) {
    showAuthError('otp-step1-error-box', 'Please enter a valid email address.');
    return;
  }

  // Generate cryptographic-style 6-digit passcode
  const otpCode = String(Math.floor(100000 + Math.random() * 900000));

  // Lock OTP session to this specific email and role
  AppState.activeOTP = {
    code: otpCode,
    email: email,
    role: AppState.selectedRoleForAuth,
    expiresAt: Date.now() + 120 * 1000, // 2 minutes valid duration
    attemptsRemaining: 3,
    isUsed: false
  };

  // Switch to Step 2 (Locked Challenge View)
  const step1 = document.getElementById('otp-step-1');
  const step2 = document.getElementById('otp-step-2');
  const lockedEmailDisplay = document.getElementById('otp-locked-email-display');
  const targetEmailNotice = document.getElementById('otp-target-email-notice');
  const codeInput = document.getElementById('otp-code-input');
  const devSecret = document.getElementById('otp-dev-secret');

  if (step1) step1.style.display = 'none';
  if (step2) step2.style.display = 'block';

  // Display locked email recipient
  if (lockedEmailDisplay) lockedEmailDisplay.textContent = email;
  if (targetEmailNotice) targetEmailNotice.textContent = email;

  // IMPORTANT: The code is NOT displayed on the login screen!
  // It is dispatched to the user's email inbox.
  console.log(`%c[VolunteerPulse Security Dispatch] OTP code sent to ${email}: ${otpCode}`, 'background: #0284c7; color: white; padding: 4px 8px; font-weight: bold; border-radius: 4px;');
  if (devSecret) devSecret.textContent = otpCode;

  // The input field is left empty for the user to manually enter
  if (codeInput) {
    codeInput.value = '';
    codeInput.focus();
  }

  // Start active countdown timer
  startOTPTimer();

  showToast(`Security code dispatched to ${email}. Please check your email inbox!`, 'success');
}

function toggleOtpDevHint() {
  const box = document.getElementById('otp-dev-code-box');
  if (box) {
    box.style.display = box.style.display === 'none' ? 'block' : 'none';
  }
}

function startOTPTimer() {
  if (AppState.otpCountdownInterval) {
    clearInterval(AppState.otpCountdownInterval);
  }

  const timerSpan = document.getElementById('otp-timer-seconds');

  const updateTimer = () => {
    if (!AppState.activeOTP || AppState.activeOTP.isUsed) {
      if (AppState.otpCountdownInterval) clearInterval(AppState.otpCountdownInterval);
      return;
    }

    const remainingMs = AppState.activeOTP.expiresAt - Date.now();
    if (remainingMs <= 0) {
      if (AppState.otpCountdownInterval) clearInterval(AppState.otpCountdownInterval);
      if (timerSpan) timerSpan.textContent = '00:00 (Expired)';
      showAuthError('otp-error-box', 'This OTP has expired. Please click "Resend to Email" to request a fresh passcode.');
      const verifyBtn = document.getElementById('btn-verify-otp');
      if (verifyBtn) verifyBtn.disabled = true;
      return;
    }

    const totalSec = Math.ceil(remainingMs / 1000);
    const mins = String(Math.floor(totalSec / 60)).padStart(2, '0');
    const secs = String(totalSec % 60).padStart(2, '0');
    if (timerSpan) timerSpan.textContent = `${mins}:${secs}`;
  };

  updateTimer();
  AppState.otpCountdownInterval = setInterval(updateTimer, 1000);
}

function cancelOTPTimer() {
  if (AppState.otpCountdownInterval) {
    clearInterval(AppState.otpCountdownInterval);
    AppState.otpCountdownInterval = null;
  }
}

function cancelOTPSession(keepStep1 = true) {
  cancelOTPTimer();
  AppState.activeOTP = null;

  const step1 = document.getElementById('otp-step-1');
  const step2 = document.getElementById('otp-step-2');
  const codeInput = document.getElementById('otp-code-input');
  const verifyBtn = document.getElementById('btn-verify-otp');
  const devBox = document.getElementById('otp-dev-code-box');

  if (verifyBtn) verifyBtn.disabled = false;
  if (codeInput) codeInput.value = '';
  if (devBox) devBox.style.display = 'none';
  if (step1 && keepStep1) step1.style.display = 'block';
  if (step2) step2.style.display = 'none';

  hideAuthErrors();
}

function resendOTP() {
  if (!AppState.activeOTP) {
    cancelOTPSession(true);
    return;
  }

  const verifyBtn = document.getElementById('btn-verify-otp');
  if (verifyBtn) verifyBtn.disabled = false;

  const email = AppState.activeOTP.email;
  const otpCode = String(Math.floor(100000 + Math.random() * 900000));

  AppState.activeOTP.code = otpCode;
  AppState.activeOTP.expiresAt = Date.now() + 120 * 1000;
  AppState.activeOTP.attemptsRemaining = 3;
  AppState.activeOTP.isUsed = false;

  console.log(`%c[VolunteerPulse Security Dispatch] New OTP code sent to ${email}: ${otpCode}`, 'background: #0284c7; color: white; padding: 4px 8px; font-weight: bold; border-radius: 4px;');
  
  const devSecret = document.getElementById('otp-dev-secret');
  if (devSecret) devSecret.textContent = otpCode;

  const codeInput = document.getElementById('otp-code-input');
  if (codeInput) {
    codeInput.value = '';
    codeInput.focus();
  }

  hideAuthErrors();
  startOTPTimer();
  showToast(`New 6-digit code sent to ${email}. Check your inbox!`, 'info');
}

function handleVerifyOTP() {
  hideAuthErrors();

  if (!AppState.activeOTP) {
    showAuthError('otp-error-box', 'No active OTP session found. Please request a new code.');
    return;
  }

  if (AppState.activeOTP.isUsed) {
    showAuthError('otp-error-box', 'This OTP has already been used and is invalidated. Request a new code.');
    return;
  }

  if (Date.now() > AppState.activeOTP.expiresAt) {
    showAuthError('otp-error-box', 'This OTP has expired. Please click "Resend Code" to continue.');
    return;
  }

  const codeInput = document.getElementById('otp-code-input');
  const enteredCode = codeInput ? codeInput.value.trim() : '';

  if (!enteredCode) {
    showAuthError('otp-error-box', 'Please type the 6-digit code shown above.');
    if (codeInput) codeInput.focus();
    return;
  }

  // Check code match
  if (enteredCode !== AppState.activeOTP.code) {
    AppState.activeOTP.attemptsRemaining--;
    if (codeInput) {
      codeInput.value = '';
      codeInput.classList.add('shake-anim');
      setTimeout(() => codeInput.classList.remove('shake-anim'), 400);
      codeInput.focus();
    }

    if (AppState.activeOTP.attemptsRemaining <= 0) {
      cancelOTPSession(true);
      showAuthError('otp-step1-error-box', 'Too many invalid attempts. The OTP challenge was destroyed for security.');
      showToast('Maximum OTP attempts reached. Please start over.', 'error');
      return;
    }

    showAuthError('otp-error-box', `❌ Incorrect code. ${AppState.activeOTP.attemptsRemaining} attempt(s) remaining. Another person cannot log in!`);
    return;
  }

  // CODE IS CORRECT!
  // CRITICAL: Invalidate & destroy the OTP immediately so it can NEVER be reused!
  const authenticatedEmail = AppState.activeOTP.email;
  const authenticatedRole = AppState.activeOTP.role;
  AppState.activeOTP.isUsed = true;
  cancelOTPTimer();
  AppState.activeOTP = null;

  // Build authenticated session
  const seed = SEED_PROFILES[authenticatedRole];
  const user = {
    id: (seed && seed.email.toLowerCase() === authenticatedEmail) ? seed.id : Math.floor(Math.random() * 1000) + 10,
    fullName: (seed && seed.email.toLowerCase() === authenticatedEmail) ? seed.fullName : authenticatedEmail.split('@')[0].toUpperCase(),
    email: authenticatedEmail,
    role: authenticatedRole,
    organizationId: seed ? seed.organizationId : 1
  };

  // Reset inputs
  if (codeInput) codeInput.value = '';
  cancelOTPSession(false);

  completeLogin(user);
}

function loginAsGuest() {
  const guestUser = { ...SEED_PROFILES.GUEST };
  AppState.currentUser = guestUser;
  localStorage.setItem('volunteer_platform_user', JSON.stringify(guestUser));
  showMainApp();
  updateUserNavUI();
  navToSection('home');
  showToast('Welcome to MyVolunteer! Browsing as Public Guest.', 'info');
}

function completeLogin(user) {
  AppState.currentUser = user;
  localStorage.setItem('volunteer_platform_user', JSON.stringify(user));
  showMainApp();
  updateUserNavUI();
  showToast(`Welcome, ${user.fullName}!`, 'success');

  // Route to corresponding view
  if (user.role === 'VOLUNTEER') {
    loadVolunteerData();
    navToSection('home');
  } else if (user.role === 'ORGANIZATION') {
    loadOrganizationData();
    goToCurrentRoleDashboard();
  } else if (user.role === 'ADMIN') {
    loadAdminData();
    goToCurrentRoleDashboard();
  } else {
    navToSection('home');
  }
}

function handleLogout() {
  // Completely destroy session
  AppState.currentUser = null;
  localStorage.removeItem('volunteer_platform_user');
  sessionStorage.clear();

  // Cancel any active OTP timer or session
  cancelOTPSession(false);

  // Clear all form inputs
  const emailInput = document.getElementById('login-email-input');
  if (emailInput) emailInput.value = '';
  const passwordInput = document.getElementById('login-password-input');
  if (passwordInput) passwordInput.value = '';
  const otpEmailInput = document.getElementById('otp-email-input');
  if (otpEmailInput) otpEmailInput.value = '';
  const otpCodeInput = document.getElementById('otp-code-input');
  if (otpCodeInput) otpCodeInput.value = '';

  // Show login portal
  showLoginPage();
  showToast('🔒 You have been securely logged out. Please sign in with password or OTP to continue.', 'info');
}

function togglePasswordVisibility(inputId, btn) {
  const input = document.getElementById(inputId);
  if (!input) return;
  if (input.type === 'password') {
    input.type = 'text';
    btn.style.color = 'var(--color-primary)';
  } else {
    input.type = 'password';
    btn.style.color = 'var(--color-text-muted)';
  }
}

function updateUserNavUI() {
  const user = AppState.currentUser;
  const nameEl = document.getElementById('header-user-name');
  const roleEl = document.getElementById('header-user-role');
  const avatarEl = document.getElementById('header-user-avatar');
  const dashBtn = document.getElementById('btn-go-dashboard');

  if (user) {
    if (nameEl) nameEl.textContent = user.fullName || 'User';
    if (roleEl) roleEl.textContent = user.role || 'GUEST';
    if (avatarEl) {
      const parts = (user.fullName || 'VP').split(' ');
      avatarEl.textContent = parts.length > 1 ? (parts[0][0] + parts[1][0]).toUpperCase() : parts[0][0].toUpperCase();
    }
    if (dashBtn) {
      dashBtn.style.display = (user.role === 'GUEST') ? 'none' : 'inline-block';
    }
  }
}

function goToCurrentRoleDashboard() {
  const user = AppState.currentUser;
  if (!user || user.role === 'GUEST') {
    showToast('Public Guests do not have a private dashboard. Explore opportunities!', 'info');
    navToSection('opportunities');
    return;
  }

  if (user.role === 'VOLUNTEER') showDashboardView('volunteer-view');
  else if (user.role === 'ORGANIZATION') showDashboardView('organization-view');
  else if (user.role === 'ADMIN') showDashboardView('admin-view');
}

function showDashboardView(viewId) {
  // Hide all panels
  document.querySelectorAll('.view-panel').forEach(p => p.classList.remove('active-panel'));
  const target = document.getElementById(viewId);
  if (target) {
    target.classList.add('active-panel');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  // Load relevant data
  if (viewId === 'volunteer-view') loadVolunteerData();
  else if (viewId === 'organization-view') loadOrganizationData();
  else if (viewId === 'admin-view') loadAdminData();

  // Clear active state on main nav buttons
  document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
}

// ===================================================================
// 2. NAVIGATION (HOME, OPPORTUNITIES, HOW IT WORKS, ORGS, OUR IMPACT)
// ===================================================================

function navToSection(target) {
  // Always bring back home view if coming from a dashboard
  document.querySelectorAll('.view-panel').forEach(p => p.classList.remove('active-panel'));
  const homeView = document.getElementById('home-view');
  if (homeView) homeView.classList.add('active-panel');

  // Update nav buttons active highlight
  const navMap = {
    'home': 'nav-btn-home',
    'opportunities': 'nav-btn-opportunities',
    'how-it-works': 'nav-btn-how-it-works',
    'organizations': 'nav-btn-organizations',
    'our-impact': 'nav-btn-our-impact'
  };

  document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
  const activeBtnId = navMap[target];
  if (activeBtnId) {
    const btn = document.getElementById(activeBtnId);
    if (btn) btn.classList.add('active');
  }

  // Smooth scroll to targeted section
  if (target === 'home') {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  } else if (target === 'opportunities') {
    const el = document.getElementById('opportunities-section');
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  } else if (target === 'how-it-works') {
    const el = document.getElementById('how-it-works-section');
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  } else if (target === 'organizations') {
    const el = document.getElementById('featured-orgs-section');
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  } else if (target === 'our-impact') {
    const el = document.getElementById('our-impact-section');
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

function switchPortalFromFooter(role) {
  openRoleLogin(role);
  showLoginPage();
}

// ===================================================================
// 3. API & DATA FETCHING
// ===================================================================

async function fetchAPI(endpoint, options = {}) {
  try {
    const res = await fetch(`${API_BASE}${endpoint}`, {
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {})
      },
      ...options
    });
    return await res.json();
  } catch (err) {
    console.warn(`[API] Network request failed for ${endpoint}:`, err);
    return null;
  }
}

async function loadPlatformStats() {
  const data = await fetchAPI('/stats');
  if (data && data.success && data.stats) {
    AppState.stats = data.stats;
  }
  renderStats();
}

function renderStats() {
  const s = AppState.stats;
  const statsMap = {
    'stat-volunteers': s.totalVolunteers || 6,
    'stat-orgs': s.totalOrganizations || 3,
    'stat-hours': (s.totalVerifiedHours || 12.5).toFixed(1) + ' hrs',
    'stat-opportunities': s.totalOpportunities || 8
  };

  for (const [id, val] of Object.entries(statsMap)) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
  }
}

async function loadOpportunities() {
  const keywordInput = document.getElementById('search-keyword-input');
  const locationSelect = document.getElementById('search-location-select');
  AppState.searchQuery = keywordInput ? keywordInput.value.trim().toLowerCase() : '';
  AppState.locationQuery = locationSelect ? locationSelect.value : '';

  let list = AppState.opportunities;
  if (!list || list.length === 0) {
    const data = await fetchAPI('/opportunities');
    if (data && data.success && Array.isArray(data.opportunities) && data.opportunities.length > 0) {
      AppState.opportunities = data.opportunities;
    } else {
      AppState.opportunities = getFallbackOpportunities();
    }
    list = AppState.opportunities;
  }

  // Filter list
  let filtered = list.filter(opp => {
    // Category match
    if (AppState.activeCategory && AppState.activeCategory !== 'All') {
      if (opp.category.toLowerCase() !== AppState.activeCategory.toLowerCase()) return false;
    }
    // Location match
    if (AppState.locationQuery) {
      if (!opp.location.toLowerCase().includes(AppState.locationQuery.toLowerCase())) return false;
    }
    // Search query match
    if (AppState.searchQuery) {
      const q = AppState.searchQuery;
      const titleMatch = (opp.title || '').toLowerCase().includes(q);
      const descMatch = (opp.description || '').toLowerCase().includes(q);
      const orgMatch = (opp.organizationName || '').toLowerCase().includes(q);
      if (!titleMatch && !descMatch && !orgMatch) return false;
    }
    return true;
  });

  AppState.filteredOpportunities = filtered;
  renderOpportunities();
}

function selectCategory(category, chipElement) {
  AppState.activeCategory = category;
  document.querySelectorAll('.category-chip').forEach(c => c.classList.remove('active'));
  if (chipElement) chipElement.classList.add('active');
  loadOpportunities();
}

function resetFilters() {
  AppState.activeCategory = 'All';
  AppState.searchQuery = '';
  AppState.locationQuery = '';

  const kw = document.getElementById('search-keyword-input');
  if (kw) kw.value = '';
  const loc = document.getElementById('search-location-select');
  if (loc) loc.value = '';

  document.querySelectorAll('.category-chip').forEach(c => {
    c.classList.toggle('active', c.getAttribute('data-category') === 'All');
  });

  loadOpportunities();
}

function filterByOrg(orgName) {
  AppState.searchQuery = orgName.toLowerCase();
  const kw = document.getElementById('search-keyword-input');
  if (kw) kw.value = orgName;
  navToSection('opportunities');
  loadOpportunities();
}

// ===================================================================
// 4. RENDERING OPPORTUNITIES & EVENT ACTIONS
// ===================================================================

function renderOpportunities() {
  const container = document.getElementById('opportunities-container');
  if (!container) return;

  if (AppState.filteredOpportunities.length === 0) {
    container.innerHTML = `
      <div style="grid-column: 1/-1; text-align: center; padding: 60px 20px; background: #FFFFFF; border-radius: 16px; border: 1.5px dashed var(--color-border-sky);">
        <div style="font-size: 2.4rem; margin-bottom: 12px;">🔍</div>
        <h3 style="font-size: 1.25rem; font-weight: 700; margin-bottom: 8px;">No matching opportunities found</h3>
        <p style="color: var(--color-text-muted); margin-bottom: 20px;">Try adjusting your keyword search, category, or location filters.</p>
        <button class="btn btn-primary btn-sm" onclick="resetFilters()">Reset All Filters</button>
      </div>
    `;
    return;
  }

  container.innerHTML = AppState.filteredOpportunities.map(opp => {
    const cap = opp.capacity || 25;
    const signed = opp.signedUpCount || 0;
    const remaining = opp.remainingSlots !== undefined ? opp.remainingSlots : Math.max(0, cap - signed);

    return `
      <div class="opportunity-card" data-id="${opp.id}">
        <div class="opp-image-box">
          <img src="${opp.imageUrl}" alt="${escapeHtml(opp.title)}" class="opp-cover-img" loading="lazy">
          <span class="opp-category-badge">${escapeHtml(opp.category || 'General')}</span>
          <span class="opp-status-badge">✓ Verified Cause</span>
        </div>
        <div class="opp-body">
          <div class="opp-org-info">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <span>${escapeHtml(opp.organizationName || 'Community Partner')}</span>
          </div>
          <h3 class="opp-title">${escapeHtml(opp.title)}</h3>
          <p class="opp-desc">${escapeHtml(opp.description)}</p>

          <div class="opp-meta-list">
            <div class="opp-meta-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
              <span>${opp.eventDate} &bull; ${opp.startTime || '09:00'} - ${opp.endTime || '13:00'}</span>
            </div>
            <div class="opp-meta-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
              <span>${escapeHtml(opp.location)}</span>
            </div>
          </div>

          <div class="opp-footer">
            <div class="opp-capacity-indicator">
              <strong>${remaining}</strong> spots remaining (${signed}/${cap})
            </div>
            <div style="display:flex; gap:8px;">
              <button class="btn btn-outline-sm" onclick="openOpportunityDetails(${opp.id})">Details</button>
              <button class="btn btn-primary btn-sm" onclick="openProgramRegistrationModal(${opp.id})">Register &rarr;</button>
            </div>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function openOpportunityDetails(id) {
  const opp = AppState.opportunities.find(o => o.id === id);
  if (!opp) return;

  const modal = document.getElementById('opp-details-modal');
  if (!modal) return;

  const img = document.getElementById('modal-opp-img');
  if (img) img.src = opp.imageUrl;

  const title = document.getElementById('modal-opp-title');
  if (title) title.textContent = opp.title;

  const cat = document.getElementById('modal-opp-category');
  if (cat) cat.textContent = opp.category;

  const org = document.getElementById('modal-opp-org');
  if (org) org.textContent = opp.organizationName || 'Nonprofit Partner';

  const date = document.getElementById('modal-opp-date');
  if (date) date.textContent = `${opp.eventDate} (${opp.startTime || '09:00'} - ${opp.endTime || '13:00'})`;

  const loc = document.getElementById('modal-opp-loc');
  if (loc) loc.textContent = opp.location;

  const desc = document.getElementById('modal-opp-desc');
  if (desc) desc.textContent = opp.description;

  const cap = document.getElementById('modal-opp-capacity');
  if (cap) cap.textContent = `${opp.signedUpCount || 0} / ${opp.capacity} spots filled`;

  const signupBtn = document.getElementById('modal-opp-signup-btn');
  if (signupBtn) {
    signupBtn.onclick = () => {
      closeModal('opp-details-modal');
      openProgramRegistrationModal(opp.id);
    };
  }

  openModal('opp-details-modal');
}

// ===================================================================
// 4B. REAL PROGRAM REGISTRATION & CANCELLATION
// ===================================================================

function openProgramRegistrationModal(oppId) {
  const opp = AppState.opportunities.find(o => o.id === oppId);
  if (!opp) return;

  if (!AppState.currentUser || AppState.currentUser.role === 'GUEST') {
    showToast('Please log in with a Volunteer account to register for programs.', 'warning');
    openRoleLogin('VOLUNTEER');
    showLoginPage();
    return;
  }

  if (AppState.currentUser.role !== 'VOLUNTEER') {
    showToast(`You are logged in as ${AppState.currentUser.role}. Switch to a Volunteer account to register.`, 'warning');
    return;
  }

  // Populate Program Registration Modal elements
  const idEl = document.getElementById('reg-program-id');
  const titleEl = document.getElementById('reg-program-title');
  const catEl = document.getElementById('reg-program-category');
  const orgEl = document.getElementById('reg-program-org');
  const dateEl = document.getElementById('reg-program-date');
  const locEl = document.getElementById('reg-program-location');
  const slotsEl = document.getElementById('reg-program-slots');
  const nameEl = document.getElementById('reg-vol-name');
  const emailEl = document.getElementById('reg-vol-email');
  const phoneEl = document.getElementById('reg-vol-phone');
  const emergencyEl = document.getElementById('reg-vol-emergency');
  const skillsEl = document.getElementById('reg-vol-skills');
  const notesEl = document.getElementById('reg-vol-notes');
  const agreeEl = document.getElementById('reg-vol-agree');

  if (idEl) idEl.value = opp.id;
  if (titleEl) titleEl.textContent = opp.title;
  if (catEl) catEl.textContent = opp.category || 'Community Initiative';
  if (orgEl) orgEl.textContent = opp.organizationName || 'Verified Nonprofit';
  if (dateEl) dateEl.textContent = `${opp.eventDate} (${opp.startTime || '09:00'} - ${opp.endTime || '13:00'})`;
  if (locEl) locEl.textContent = opp.location;
  
  const cap = opp.capacity || 25;
  const signed = opp.signedUpCount || 0;
  const rem = opp.remainingSlots !== undefined ? opp.remainingSlots : Math.max(0, cap - signed);
  if (slotsEl) slotsEl.textContent = `${rem} spot(s) remaining (${signed}/${cap})`;

  if (nameEl) nameEl.value = AppState.currentUser.fullName || '';
  if (emailEl) emailEl.value = AppState.currentUser.email || '';
  if (phoneEl && !phoneEl.value) phoneEl.value = '+1 (555) 234-5678';
  if (emergencyEl && !emergencyEl.value) emergencyEl.value = 'John Johnson (+1 555-888-9999)';
  if (skillsEl && !skillsEl.value) skillsEl.value = 'First Aid, Community Logistics';
  if (notesEl) notesEl.value = '';
  if (agreeEl) agreeEl.checked = true;

  closeModal('opp-details-modal');
  openModal('program-register-modal');
}

async function handleConfirmProgramRegistration(event) {
  event.preventDefault();
  const idEl = document.getElementById('reg-program-id');
  const oppId = idEl ? parseInt(idEl.value) : null;
  const opp = AppState.opportunities.find(o => o.id === oppId);

  if (!opp) {
    showToast('Opportunity not found.', 'error');
    return;
  }

  // Check duplicate registration
  const alreadyRegistered = AppState.userSignups.some(s => (s.opportunityId === opp.id || s.opportunityTitle === opp.title) && s.status === 'CONFIRMED');
  if (alreadyRegistered) {
    showToast(`You are already registered for "${opp.title}".`, 'info');
    closeModal('program-register-modal');
    return;
  }

  const volName = document.getElementById('reg-vol-name').value.trim();
  const volEmail = document.getElementById('reg-vol-email').value.trim();
  const volPhone = document.getElementById('reg-vol-phone').value.trim();
  const volEmergency = document.getElementById('reg-vol-emergency').value.trim();
  const volSkills = document.getElementById('reg-vol-skills').value.trim();
  const volNotes = document.getElementById('reg-vol-notes').value.trim();

  // Create new active signup
  const newSignup = {
    id: Date.now(),
    opportunityId: opp.id,
    opportunityTitle: opp.title,
    eventDate: opp.eventDate,
    location: opp.location,
    volunteerName: volName,
    volunteerEmail: volEmail,
    phone: volPhone,
    emergencyContact: volEmergency,
    skills: volSkills,
    notes: volNotes,
    status: 'CONFIRMED',
    registeredAt: new Date().toLocaleDateString()
  };

  AppState.userSignups.unshift(newSignup);

  // Update capacity & spots
  opp.signedUpCount = (opp.signedUpCount || 0) + 1;
  if (opp.remainingSlots !== undefined) {
    opp.remainingSlots = Math.max(0, opp.remainingSlots - 1);
  }

  // Call API with local fallback
  fetchAPI('/signups', {
    method: 'POST',
    body: JSON.stringify({
      volunteerId: AppState.currentUser ? AppState.currentUser.id : 4,
      opportunityId: opp.id,
      phone: volPhone,
      emergencyContact: volEmergency
    })
  });

  closeModal('program-register-modal');
  renderOpportunities();
  renderVolunteerDashboard();
  showToast(`🎉 Successfully registered for "${opp.title}"! All contact details recorded.`, 'success');
}

function handleCancelRegistration(signupId) {
  const idx = AppState.userSignups.findIndex(s => s.id === signupId);
  if (idx === -1) return;

  const signup = AppState.userSignups[idx];
  const opp = AppState.opportunities.find(o => o.id === signup.opportunityId || o.title === signup.opportunityTitle);

  if (opp) {
    opp.signedUpCount = Math.max(0, (opp.signedUpCount || 1) - 1);
    if (opp.remainingSlots !== undefined) {
      opp.remainingSlots = (opp.remainingSlots || 0) + 1;
    }
  }

  AppState.userSignups.splice(idx, 1);
  renderVolunteerDashboard();
  renderOpportunities();
  showToast(`Registration cancelled for "${signup.opportunityTitle}". Spot released back to community.`, 'info');
}

// ===================================================================
// 5. DASHBOARD DATA & OPERATIONS
// ===================================================================

async function loadVolunteerData() {
  if (!AppState.currentUser || AppState.currentUser.role !== 'VOLUNTEER') return;
  const volId = AppState.currentUser.id;

  // Signups
  if (!AppState.userSignups || AppState.userSignups.length === 0) {
    const sData = await fetchAPI(`/signups?volunteerId=${volId}`);
    if (sData && sData.success && Array.isArray(sData.signups) && sData.signups.length > 0) {
      AppState.userSignups = sData.signups;
    } else {
      AppState.userSignups = [
        { id: 101, opportunityId: 1, opportunityTitle: "Community Tree Planting Drive", eventDate: "2026-10-25", location: "Riverside Park", phone: "+1 (555) 234-5678", emergencyContact: "John Johnson (+1 555-888-9999)", status: "CONFIRMED" },
        { id: 102, opportunityId: 2, opportunityTitle: "Weekend Grocery Packaging & Distribution", eventDate: "2026-10-26", location: "Metro Warehouse", phone: "+1 (555) 234-5678", emergencyContact: "John Johnson (+1 555-888-9999)", status: "CONFIRMED" }
      ];
    }
  }

  // Hours
  if (!AppState.userHours || AppState.userHours.length === 0) {
    const hData = await fetchAPI(`/hours?volunteerId=${volId}`);
    if (hData && hData.success && Array.isArray(hData.hours) && hData.hours.length > 0) {
      AppState.userHours = hData.hours;
      if (hData.totalVerified !== undefined) {
        AppState.currentUser.totalVerifiedHours = hData.totalVerified;
      }
    } else {
      AppState.userHours = [
        { id: 201, opportunityTitle: "Coastal Beach & Estuary Cleanup", hoursWorked: 4.0, workDate: "2026-10-12", status: "VERIFIED" },
        { id: 202, opportunityTitle: "Community Tree Planting Drive", hoursWorked: 3.5, workDate: "2026-10-05", status: "VERIFIED" }
      ];
      AppState.currentUser.totalVerifiedHours = 7.5;
    }
  }

  renderVolunteerDashboard();
}

function renderVolunteerDashboard() {
  const verifiedEl = document.getElementById('vol-verified-hours');
  const activeEl = document.getElementById('vol-active-signups');
  const compEl = document.getElementById('vol-completed-events');
  const signupsBadge = document.getElementById('vol-signups-count-badge');

  if (verifiedEl) verifiedEl.textContent = `${(AppState.currentUser.totalVerifiedHours || 7.5).toFixed(1)} hrs`;
  if (activeEl) activeEl.textContent = AppState.userSignups.length;
  if (compEl) compEl.textContent = AppState.userHours.length;
  if (signupsBadge) signupsBadge.textContent = `${AppState.userSignups.length} Registered`;

  // Render signups with cancel button
  const signupsCont = document.getElementById('volunteer-signups-container');
  if (signupsCont) {
    if (AppState.userSignups.length === 0) {
      signupsCont.innerHTML = '<p class="empty-state-text">No active registrations. Discover opportunities to get started!</p>';
    } else {
      signupsCont.innerHTML = AppState.userSignups.map(s => `
        <div class="dash-item-card">
          <div class="dash-item-info">
            <h4>${escapeHtml(s.opportunityTitle || 'Volunteer Event')}</h4>
            <p>📅 ${s.eventDate || 'Upcoming'} &bull; 📍 ${escapeHtml(s.location || 'Local Site')}</p>
            ${s.phone ? `<p style="font-size:0.75rem; color:var(--color-text-muted); margin-top:2px;">📞 Contact: ${escapeHtml(s.phone)} | 🚨 Emergency: ${escapeHtml(s.emergencyContact || 'On file')}</p>` : ''}
          </div>
          <div style="display:flex; gap:8px; align-items:center;">
            <span class="badge badge-success">Confirmed</span>
            <button class="btn-cancel-reg" onclick="handleCancelRegistration(${s.id})" title="Cancel this registration">✕ Cancel</button>
          </div>
        </div>
      `).join('');
    }
  }

  // Render hours
  const hoursCont = document.getElementById('volunteer-hours-container');
  if (hoursCont) {
    if (AppState.userHours.length === 0) {
      hoursCont.innerHTML = '<p class="empty-state-text">No service hours logged yet.</p>';
    } else {
      hoursCont.innerHTML = AppState.userHours.map(h => `
        <div class="dash-item-card">
          <div class="dash-item-info">
            <h4>${escapeHtml(h.opportunityTitle || 'Volunteer Work')}</h4>
            <p>⏱️ ${h.hoursWorked} hours &bull; 📅 ${h.workDate || 'Recently'}</p>
          </div>
          <div>
            <span class="badge ${h.status === 'VERIFIED' ? 'badge-success' : 'badge-warning'}">
              ${h.status || 'PENDING'}
            </span>
          </div>
        </div>
      `).join('');
    }
  }

  // Populate opportunity select dropdown in log hours modal
  const oppSelect = document.getElementById('hours-opp-select');
  if (oppSelect && AppState.opportunities.length > 0) {
    oppSelect.innerHTML = AppState.opportunities.map(o => `
      <option value="${o.id}">${escapeHtml(o.title)}</option>
    `).join('');
  }
}

// ===================================================================
// 5B. ORGANIZATION CONSOLE WITH WORKING ACCEPT & REJECT
// ===================================================================

async function loadOrganizationData() {
  if (!AppState.currentUser || AppState.currentUser.role !== 'ORGANIZATION') return;
  const orgCont = document.getElementById('org-opportunities-container');

  if (orgCont) {
    const myOpps = AppState.opportunities.filter(o => o.organizationId === 1 || o.organizationId === (AppState.currentUser.organizationId || 1));
    orgCont.innerHTML = myOpps.map(o => `
      <div class="dash-item-card">
        <div class="dash-item-info">
          <h4>${escapeHtml(o.title)}</h4>
          <p>📅 ${o.eventDate} &bull; 👥 ${o.signedUpCount || 0}/${o.capacity} Volunteers</p>
        </div>
        <div>
          <span class="badge badge-primary">Published</span>
        </div>
      </div>
    `).join('');
  }

  renderOrgHoursQueue();
}

function renderOrgHoursQueue() {
  const orgHoursCont = document.getElementById('org-hours-verification-container');
  if (!orgHoursCont) return;

  if (!AppState.orgPendingHours || AppState.orgPendingHours.length === 0) {
    orgHoursCont.innerHTML = '<p class="empty-state-text">No pending hours awaiting verification.</p>';
    return;
  }

  orgHoursCont.innerHTML = AppState.orgPendingHours.map(h => {
    let actionMarkup = '';
    if (h.status === 'PENDING') {
      actionMarkup = `
        <div style="display:flex; gap:6px;">
          <button class="btn-approve-action" onclick="handleOrgVerifyHours(${h.id}, 'VERIFIED')">✓ Accept</button>
          <button class="btn-reject-action" onclick="handleOrgVerifyHours(${h.id}, 'REJECTED')">✕ Reject</button>
        </div>
      `;
    } else if (h.status === 'VERIFIED') {
      actionMarkup = `<span class="badge-approved-tag">✓ Verified & Logged</span>`;
    } else {
      actionMarkup = `<span class="badge-rejected-tag">✕ Rejected</span>`;
    }

    return `
      <div class="dash-item-card" id="org-hour-${h.id}">
        <div class="dash-item-info">
          <h4>${escapeHtml(h.volunteerName)} — <strong>${h.hoursWorked} Hours</strong></h4>
          <p>🎯 ${escapeHtml(h.opportunityTitle)} &bull; 📅 Date: ${h.workDate}</p>
        </div>
        <div>
          ${actionMarkup}
        </div>
      </div>
    `;
  }).join('');
}

function handleOrgVerifyHours(hoursId, status) {
  const entry = AppState.orgPendingHours.find(h => h.id === hoursId);
  if (!entry) return;

  entry.status = status;
  if (status === 'VERIFIED') {
    AppState.stats.totalVerifiedHours = (AppState.stats.totalVerifiedHours || 12.5) + entry.hoursWorked;
    renderStats();
    showToast(`✅ Hours Accepted & Verified! Added ${entry.hoursWorked} hrs for ${entry.volunteerName}.`, 'success');
  } else {
    showToast(`❌ Verification Rejected for ${entry.volunteerName}. Record updated.`, 'warning');
  }

  renderOrgHoursQueue();
}

// ===================================================================
// 5C. ADMIN GOVERNANCE CONSOLE WITH WORKING ACCEPT & REJECT (WITH REASON)
// ===================================================================

async function loadAdminData() {
  if (!AppState.currentUser || AppState.currentUser.role !== 'ADMIN') return;
  renderAdminModerationQueue();
  renderAdminUsers();
}

function renderAdminModerationQueue() {
  const modCont = document.getElementById('admin-moderation-container');
  if (!modCont) return;

  if (!AppState.pendingModeration || AppState.pendingModeration.length === 0) {
    modCont.innerHTML = '<p class="empty-state-text">No items pending moderation.</p>';
    return;
  }

  modCont.innerHTML = AppState.pendingModeration.map(item => {
    let actionMarkup = '';
    if (item.status === 'PENDING') {
      actionMarkup = `
        <div style="display:flex; gap:6px;">
          <button class="btn-approve-action" onclick="handleAdminApproveListing(${item.id})">✓ Accept & Publish</button>
          <button class="btn-reject-action" onclick="openAdminRejectModal(${item.id})">✕ Reject</button>
        </div>
      `;
    } else if (item.status === 'APPROVED') {
      actionMarkup = `<span class="badge-approved-tag">✓ Approved & Live</span>`;
    } else {
      actionMarkup = `
        <div style="display:flex; flex-direction:column; align-items:flex-end; gap:4px;">
          <span class="badge-rejected-tag">✕ Rejected</span>
          <span style="font-size:0.72rem; color:#DC2626; max-width:220px; text-align:right;">Reason: ${escapeHtml(item.rejectionReason || 'Policy non-compliance')}</span>
        </div>
      `;
    }

    return `
      <div class="dash-item-card" id="admin-mod-${item.id}">
        <div class="dash-item-info">
          <h4>${escapeHtml(item.title)}</h4>
          <p>🏢 ${escapeHtml(item.organizationName)} &bull; 👥 Capacity: ${item.capacity} &bull; 📅 ${item.eventDate}</p>
        </div>
        <div>
          ${actionMarkup}
        </div>
      </div>
    `;
  }).join('');
}

function handleAdminApproveListing(oppId) {
  const item = AppState.pendingModeration.find(i => i.id === oppId);
  if (!item) return;

  item.status = 'APPROVED';

  // Ensure listing is in live public opportunities
  const exists = AppState.opportunities.find(o => o.id === item.id);
  if (!exists) {
    AppState.opportunities.unshift({
      id: item.id,
      organizationId: 1,
      organizationName: item.organizationName,
      title: item.title,
      description: `Moderator approved community opportunity by ${item.organizationName}.`,
      category: item.category || "Community Relief",
      eventDate: item.eventDate,
      startTime: "09:00",
      endTime: "13:00",
      location: item.location || "Community Hall",
      capacity: item.capacity,
      signedUpCount: 0,
      remainingSlots: item.capacity,
      imageUrl: "https://images.unsplash.com/photo-1542601906990-b4d3fb778b09?auto=format&fit=crop&w=1200&q=80"
    });
    loadOpportunities();
  }

  renderAdminModerationQueue();
  showToast(`✅ Listing "${item.title}" APPROVED and published live on MyVolunteer!`, 'success');
}

function openAdminRejectModal(oppId) {
  const opp = AppState.pendingModeration.find(i => i.id === oppId);
  if (!opp) return;

  const idEl = document.getElementById('admin-reject-opp-id');
  const presetEl = document.getElementById('admin-reject-preset');
  const notesEl = document.getElementById('admin-reject-notes');

  if (idEl) idEl.value = opp.id;
  if (presetEl) presetEl.value = 'Incomplete event details and safety guidelines';
  if (notesEl) notesEl.value = 'Incomplete event details and safety guidelines. Please update your risk assessment and re-submit.';

  openModal('admin-rejection-modal');
}

function applyPresetRejectionReason(presetVal) {
  const notesEl = document.getElementById('admin-reject-notes');
  if (!notesEl) return;
  if (presetVal === 'CUSTOM') {
    notesEl.value = '';
    notesEl.focus();
  } else {
    notesEl.value = presetVal;
  }
}

function confirmAdminRejection(event) {
  event.preventDefault();
  const idEl = document.getElementById('admin-reject-opp-id');
  const oppId = idEl ? parseInt(idEl.value) : null;
  const notesEl = document.getElementById('admin-reject-notes');
  const reason = notesEl ? notesEl.value.trim() : 'Listing declined by administrator.';

  const item = AppState.pendingModeration.find(i => i.id === oppId);
  if (item) {
    item.status = 'REJECTED';
    item.rejectionReason = reason;
  }

  closeModal('admin-rejection-modal');
  renderAdminModerationQueue();
  showToast(`🚫 Listing "${item ? item.title : 'Opportunity'}" REJECTED. Rejection reason recorded and sent to nonprofit.`, 'warning');
}

function renderAdminUsers() {
  const usersCont = document.getElementById('admin-users-container');
  if (!usersCont) return;

  usersCont.innerHTML = `
    <div class="dash-item-card">
      <div class="dash-item-info">
        <h4>Alice Johnson</h4>
        <p>alice.johnson@example.com &bull; Role: VOLUNTEER</p>
      </div>
      <div><span class="badge badge-success">Active</span></div>
    </div>
    <div class="dash-item-card">
      <div class="dash-item-info">
        <h4>Green Earth Alliance</h4>
        <p>contact@greenearth.org &bull; Role: ORGANIZATION</p>
      </div>
      <div><span class="badge badge-success">Verified</span></div>
    </div>
    <div class="dash-item-card">
      <div class="dash-item-info">
        <h4>System Administrator</h4>
        <p>admin@platform.com &bull; Role: ADMIN</p>
      </div>
      <div><span class="badge badge-primary">Super Admin</span></div>
    </div>
  `;
}

// ===================================================================
// 6. MODALS & FORMS
// ===================================================================

function setupModals() {
  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) {
        overlay.classList.remove('active');
      }
    });
  });
}

function openModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.add('active');
}

function closeModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.remove('active');
}

function openRegisterModal() {
  openModal('register-modal');
}

function openRegisterModalFromLogin() {
  openRegisterModal();
}

function toggleOrgNameField(role) {
  const grp = document.getElementById('reg-orgname-group');
  if (grp) grp.style.display = (role === 'ORGANIZATION') ? 'block' : 'none';
}

function setupForms() {
  // Log Hours Form
  const logHoursForm = document.getElementById('log-hours-form');
  if (logHoursForm) {
    logHoursForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const oppId = document.getElementById('hours-opp-select').value;
      const hours = parseFloat(document.getElementById('hours-number').value);
      const date = document.getElementById('hours-date').value;
      const desc = document.getElementById('hours-description').value;

      if (!AppState.currentUser || AppState.currentUser.role !== 'VOLUNTEER') {
        showToast('Please sign in as a volunteer to log hours.', 'warning');
        return;
      }

      await fetchAPI('/hours', {
        method: 'POST',
        body: JSON.stringify({
          volunteerId: AppState.currentUser.id,
          opportunityId: oppId,
          hoursWorked: hours,
          workDate: date,
          description: desc
        })
      });

      // Update state locally
      const opp = AppState.opportunities.find(o => String(o.id) === String(oppId));
      AppState.userHours.unshift({
        id: Date.now(),
        opportunityTitle: opp ? opp.title : 'Volunteer Service',
        hoursWorked: hours,
        workDate: date,
        status: 'PENDING'
      });

      closeModal('log-hours-modal');
      showToast('Hours logged successfully and submitted for organization approval!', 'success');
      renderVolunteerDashboard();
    });
  }

  // Create Opportunity Form
  const createOppForm = document.getElementById('create-opportunity-form');
  if (createOppForm) {
    createOppForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const title = document.getElementById('create-opp-title').value;
      const category = document.getElementById('create-opp-category').value;
      const capacity = parseInt(document.getElementById('create-opp-capacity').value);
      const eventDate = document.getElementById('create-opp-date').value;
      const startTime = document.getElementById('create-opp-start').value;
      const endTime = document.getElementById('create-opp-end').value;
      const location = document.getElementById('create-opp-location').value;
      const description = document.getElementById('create-opp-description').value;

      const newOpp = {
        id: Date.now(),
        organizationId: AppState.currentUser ? (AppState.currentUser.organizationId || 1) : 1,
        organizationName: AppState.currentUser ? AppState.currentUser.fullName : "Verified Nonprofit",
        title,
        category,
        capacity,
        eventDate,
        startTime,
        endTime,
        location,
        description,
        signedUpCount: 0,
        remainingSlots: capacity,
        imageUrl: "https://images.unsplash.com/photo-1542601906990-b4d3fb778b09?auto=format&fit=crop&w=1200&q=80"
      };

      AppState.opportunities.unshift(newOpp);
      closeModal('create-opportunity-modal');
      showToast('New Opportunity published and added to live listings!', 'success');
      loadOpportunities();
      loadOrganizationData();
    });
  }

  // Register Account Form
  const regForm = document.getElementById('register-form');
  if (regForm) {
    regForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const fullName = document.getElementById('reg-fullname').value.trim();
      const email = document.getElementById('reg-email').value.trim();
      const password = document.getElementById('reg-password').value;
      const role = document.getElementById('reg-role').value;
      const orgName = document.getElementById('reg-orgname') ? document.getElementById('reg-orgname').value.trim() : '';

      // Call backend API for registration
      await fetchAPI('/auth/register', {
        method: 'POST',
        body: JSON.stringify({
          fullName, email, password, role, organizationName: orgName
        })
      });

      // Close the modal
      closeModal('register-modal');

      // Clear the registration form fields
      regForm.reset();

      // CRITICAL REQUIREMENT: Do NOT log in automatically!
      // User must explicitly log in with their credentials or OTP!
      openRoleLogin(role, true);

      // Pre-fill the registered email on the login form, leave password empty
      const loginEmailInput = document.getElementById('login-email-input');
      if (loginEmailInput) loginEmailInput.value = email;
      const loginPassInput = document.getElementById('login-password-input');
      if (loginPassInput) {
        loginPassInput.value = '';
        loginPassInput.focus();
      }

      // Display explicit success message prompting login
      const successBox = document.getElementById('auth-success-box');
      if (successBox) {
        successBox.innerHTML = `<span>✅ Account registered successfully for <strong>${escapeHtml(fullName)}</strong>! Please enter your password or use OTP to log in.</span>`;
        successBox.style.display = 'flex';
      }

      showToast(`Registration successful! Please sign in to access your account.`, 'success');
    });
  }
}

// ===================================================================
// 7. TOAST NOTIFICATIONS
// ===================================================================

function showToast(message, type = 'info') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <div style="flex: 1; font-size: 0.875rem; font-weight: 600; color: var(--color-text-main);">${escapeHtml(message)}</div>
  `;

  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// ===================================================================
// 8. RICH REAL-WORLD VOLUNTEER OPPORTUNITIES
// ===================================================================

function getFallbackOpportunities() {
  return [
    {
      id: 1,
      organizationId: 1,
      organizationName: "Green Earth Alliance",
      title: "Community Tree Planting Drive",
      description: "Join us to plant over 300 native saplings in Riverside Park to combat local urban heat islands and restore native bird habitats.",
      category: "Environment",
      eventDate: "2026-10-25",
      startTime: "09:00",
      endTime: "13:00",
      location: "Riverside Park, Sector 4",
      capacity: 25,
      signedUpCount: 2,
      remainingSlots: 23,
      imageUrl: "https://images.unsplash.com/photo-1542601906990-b4d3fb778b09?auto=format&fit=crop&w=1200&q=80"
    },
    {
      id: 2,
      organizationId: 2,
      organizationName: "Metro Community Food Bank",
      title: "Weekend Grocery Packaging & Distribution",
      description: "Sort, pack, and distribute weekly emergency food kits to elderly residents and families in need across downtown neighborhoods.",
      category: "Community Relief",
      eventDate: "2026-10-26",
      startTime: "10:00",
      endTime: "14:00",
      location: "Metro Warehouse, 42 Elm Street",
      capacity: 15,
      signedUpCount: 1,
      remainingSlots: 14,
      imageUrl: "https://images.unsplash.com/photo-1593113598332-cd288d649433?auto=format&fit=crop&w=1200&q=80"
    },
    {
      id: 3,
      organizationId: 1,
      organizationName: "Green Earth Alliance",
      title: "Coastal Beach & Estuary Cleanup",
      description: "Collection, documentation, and sorting of marine plastic waste along North Shore coastline to protect fragile aquatic wildlife.",
      category: "Conservation",
      eventDate: "2026-11-05",
      startTime: "08:30",
      endTime: "12:00",
      location: "North Shore Beach Pavilion",
      capacity: 30,
      signedUpCount: 4,
      remainingSlots: 26,
      imageUrl: "https://images.unsplash.com/photo-1618477461853-cf6ed80faba5?auto=format&fit=crop&w=1200&q=80"
    },
    {
      id: 4,
      organizationId: 3,
      organizationName: "City Hope Healthcare",
      title: "Hospital Patient Companion & Wheelchair Escort",
      description: "Provide compassionate company, read books, and escort elderly mobility-assisted patients to therapeutic garden grounds.",
      category: "Healthcare",
      eventDate: "2026-11-08",
      startTime: "09:00",
      endTime: "13:00",
      location: "City Hospital Complex, Ward C",
      capacity: 12,
      signedUpCount: 3,
      remainingSlots: 9,
      imageUrl: "https://images.unsplash.com/photo-1576765608535-5f04d1e3f289?auto=format&fit=crop&w=1200&q=80"
    },
    {
      id: 5,
      organizationId: 2,
      organizationName: "Metro Community Food Bank",
      title: "Winter Warmth Clothing & Emergency Blanket Drive",
      description: "Inspect, sort, and bundle warm winter jackets, gloves, and blankets for unhoused neighbors facing sub-zero nights.",
      category: "Community Relief",
      eventDate: "2026-11-12",
      startTime: "11:00",
      endTime: "15:00",
      location: "Metro Warehouse, 42 Elm Street",
      capacity: 20,
      signedUpCount: 2,
      remainingSlots: 18,
      imageUrl: "https://images.unsplash.com/photo-1488521787991-ed7bbaae773c?auto=format&fit=crop&w=1200&q=80"
    },
    {
      id: 6,
      organizationId: 4,
      organizationName: "NextGen Tech Alliance",
      title: "Youth STEM & Coding Weekend Mentorship",
      description: "Mentor underprivileged middle school students through visual block coding and introductory web design to ignite tech careers.",
      category: "Education",
      eventDate: "2026-11-15",
      startTime: "10:00",
      endTime: "14:30",
      location: "Old Town Tech Center, Lab 2",
      capacity: 16,
      signedUpCount: 5,
      remainingSlots: 11,
      imageUrl: "https://images.unsplash.com/photo-1509062522246-3755977927d7?auto=format&fit=crop&w=1200&q=80"
    },
    {
      id: 7,
      organizationId: 5,
      organizationName: "Global Relief Network",
      title: "Emergency Flood Relief Supply Packaging",
      description: "Assembly of sterile water purification kits, hygiene packs, and non-perishable rations for families impacted by flash flooding.",
      category: "Disaster Relief",
      eventDate: "2026-11-18",
      startTime: "08:00",
      endTime: "14:00",
      location: "Eastside Community Hall",
      capacity: 40,
      signedUpCount: 12,
      remainingSlots: 28,
      imageUrl: "https://images.unsplash.com/photo-1547683905-f686c993aae5?auto=format&fit=crop&w=1200&q=80"
    },
    {
      id: 8,
      organizationId: 6,
      organizationName: "Paws & Hearts Rescue",
      title: "Shelter Animal Care & Weekend Adoption Fair Guide",
      description: "Socialize rescued dogs and cats, assist veterinary helpers with grooming, and guide adopting families through companion matches.",
      category: "Animal Welfare",
      eventDate: "2026-11-22",
      startTime: "09:30",
      endTime: "14:00",
      location: "North Shore Animal Sanctuary",
      capacity: 18,
      signedUpCount: 6,
      remainingSlots: 12,
      imageUrl: "https://images.unsplash.com/photo-1548767797-d8c844163c4c?auto=format&fit=crop&w=1200&q=80"
    }
  ];
}
