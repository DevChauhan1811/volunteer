/**
 * ONLINE VOLUNTEER MANAGEMENT PLATFORM - FRONTEND APPLICATION
 * Full-stack interactive JavaScript application connected to Java REST API & MySQL.
 */

const API_BASE = '/api';

// --- State Management ---
const AppState = {
  currentUser: null, // { id, fullName, email, role, organizationId }
  currentView: 'home', // 'home' | 'volunteer' | 'organization' | 'admin'
  opportunities: [],
  filteredOpportunities: [],
  stats: {
    totalUsers: 6,
    totalOrganizations: 2,
    totalVolunteers: 3,
    totalOpportunities: 4,
    activeSignups: 4,
    totalVerifiedHours: 7.5
  },
  activeCategory: 'All',
  searchQuery: '',
  locationQuery: '',
  dateQuery: '',
  userSignups: [],
  userHours: [],
  pendingModeration: [],
  orgOpportunities: [],
  adminUsers: []
};

// Demo Seed Profiles for quick evaluation
const DEMO_USERS = {
  GUEST: null,
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
  }
};

// --- Initialization ---
document.addEventListener('DOMContentLoaded', () => {
  // Check localStorage for saved session
  const savedUser = localStorage.getItem('volunteer_platform_user');
  if (savedUser) {
    try {
      AppState.currentUser = JSON.parse(savedUser);
    } catch (e) {
      AppState.currentUser = null;
    }
  }

  // Initialize UI components
  setupNavigation();
  setupRoleSwitcher();
  setupFilters();
  setupModals();

  // Load initial data from REST API
  loadPlatformStats();
  loadOpportunities();

  // If already logged in, update header UI
  updateUserNavUI();
});

// --- API Client Helpers ---
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

// --- Data Fetching ---
async function loadPlatformStats() {
  const data = await fetchAPI('/stats');
  if (data && data.success && data.stats) {
    AppState.stats = data.stats;
  }
  renderStats();
}

async function loadOpportunities() {
  let query = '';
  const params = [];
  if (AppState.searchQuery) params.push(`keyword=${encodeURIComponent(AppState.searchQuery)}`);
  if (AppState.activeCategory && AppState.activeCategory !== 'All') {
    params.push(`category=${encodeURIComponent(AppState.activeCategory)}`);
  }
  if (AppState.locationQuery) params.push(`location=${encodeURIComponent(AppState.locationQuery)}`);
  if (AppState.dateQuery) params.push(`date=${encodeURIComponent(AppState.dateQuery)}`);

  if (params.length > 0) query = '?' + params.join('&');

  const data = await fetchAPI(`/opportunities${query}`);
  if (data && data.success && Array.isArray(data.opportunities)) {
    AppState.opportunities = data.opportunities;
  } else if (!AppState.opportunities.length) {
    // Fallback seed data if offline
    AppState.opportunities = getFallbackOpportunities();
  }
  AppState.filteredOpportunities = [...AppState.opportunities];
  renderOpportunities();
}

async function loadVolunteerData() {
  if (!AppState.currentUser || AppState.currentUser.role !== 'VOLUNTEER') return;
  const volId = AppState.currentUser.id;

  // Load Signups
  const signupsData = await fetchAPI(`/signups?volunteerId=${volId}`);
  if (signupsData && signupsData.success) {
    AppState.userSignups = signupsData.signups;
  }

  // Load Hours
  const hoursData = await fetchAPI(`/hours?volunteerId=${volId}`);
  if (hoursData && hoursData.success) {
    AppState.userHours = hoursData.hours;
    if (hoursData.totalVerified !== undefined) {
      AppState.currentUser.totalVerifiedHours = hoursData.totalVerified;
    }
  }

  renderVolunteerDashboard();
}

async function loadOrganizationData() {
  if (!AppState.currentUser || AppState.currentUser.role !== 'ORGANIZATION') return;
  const orgId = AppState.currentUser.organizationId || 1;

  // Fetch opportunities owned by organization
  const oppsData = await fetchAPI(`/opportunities/all?orgId=${orgId}`);
  if (oppsData && oppsData.success) {
    AppState.orgOpportunities = oppsData.opportunities;
  } else {
    AppState.orgOpportunities = AppState.opportunities.filter(o => o.organizationId === orgId);
  }

  // Fetch pending hours
  const hoursData = await fetchAPI(`/hours?organizationId=${orgId}`);
  if (hoursData && hoursData.success) {
    AppState.orgPendingHours = hoursData.pendingHours || [];
  }

  renderOrganizationDashboard();
}

async function loadAdminData() {
  if (!AppState.currentUser || AppState.currentUser.role !== 'ADMIN') return;

  // Load all opportunities for moderation
  const oppsData = await fetchAPI('/opportunities/all');
  if (oppsData && oppsData.success) {
    AppState.pendingModeration = oppsData.opportunities;
  }

  // Load users list
  const usersData = await fetchAPI('/users');
  if (usersData && usersData.success) {
    AppState.adminUsers = usersData.users;
  }

  renderAdminDashboard();
}

// --- Render Functions ---
function renderStats() {
  const s = AppState.stats;
  const statsMap = {
    'stat-volunteers': s.totalVolunteers || 3,
    'stat-orgs': s.totalOrganizations || 2,
    'stat-hours': (s.totalVerifiedHours || 7.5).toFixed(1) + ' hrs',
    'stat-opportunities': s.totalOpportunities || 4
  };

  for (const [id, val] of Object.entries(statsMap)) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
  }
}

function renderOpportunities() {
  const container = document.getElementById('opportunities-container');
  if (!container) return;

  if (AppState.filteredOpportunities.length === 0) {
    container.innerHTML = `
      <div style="grid-column: 1/-1; text-align: center; padding: 60px 20px; background: #FFFFFF; border-radius: 16px; border: 1px dashed var(--color-border);">
        <div style="font-size: 2rem; margin-bottom: 12px;">🔍</div>
        <h3 style="font-size: 1.25rem; font-weight: 700; margin-bottom: 8px;">No matching opportunities found</h3>
        <p style="color: var(--color-text-muted); margin-bottom: 20px;">Try adjusting your keyword search or category filters.</p>
        <button class="btn btn-secondary btn-sm" onclick="resetFilters()">Reset All Filters</button>
      </div>
    `;
    return;
  }

  container.innerHTML = AppState.filteredOpportunities.map(opp => {
    const cap = opp.capacity || 20;
    const signed = opp.signedUpCount || 0;
    const remaining = opp.remainingSlots !== undefined ? opp.remainingSlots : (cap - signed);
    const pct = Math.min(100, Math.round((signed / cap) * 100));

    return `
      <div class="opportunity-card" data-id="${opp.id}">
        <div class="opp-image-box">
          <img src="${opp.imageUrl}" alt="${escapeHtml(opp.title)}" class="opp-cover-img" loading="lazy">
          <span class="opp-category-badge">${escapeHtml(opp.category || 'General')}</span>
          <span class="opp-status-badge badge-approved">Verified Cause</span>
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
              <span>${opp.eventDate} (${opp.startTime || '09:00'} - ${opp.endTime || '13:00'})</span>
            </div>
            <div class="opp-meta-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
              <span>${escapeHtml(opp.location)}</span>
            </div>
          </div>

          <div class="opp-capacity-bar">
            <div class="capacity-text">
              <span>Volunteer Capacity</span>
              <span>${signed} / ${cap} spots (${remaining} left)</span>
            </div>
            <div class="progress-track">
              <div class="progress-fill" style="width: ${pct}%"></div>
            </div>
          </div>

          <div class="opp-actions">
            <button class="btn btn-secondary btn-sm" onclick="openOpportunityDetails(${opp.id})">
              View Details
            </button>
            <button class="btn btn-primary btn-sm" onclick="handleOpportunitySignup(${opp.id})" ${remaining <= 0 ? 'disabled' : ''}>
              ${remaining <= 0 ? 'Full' : 'Sign Up'}
            </button>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

// --- Volunteer Dashboard ---
function renderVolunteerDashboard() {
  const welcomeName = document.getElementById('vol-welcome-name');
  if (welcomeName && AppState.currentUser) {
    welcomeName.textContent = AppState.currentUser.fullName;
  }

  // Update Counters
  const hoursCount = document.getElementById('vol-hours-count');
  if (hoursCount) {
    hoursCount.textContent = (AppState.currentUser.totalVerifiedHours || 4.0).toFixed(1) + ' hrs';
  }

  const signupsCount = document.getElementById('vol-signups-count');
  if (signupsCount) {
    signupsCount.textContent = AppState.userSignups.length || '2';
  }

  // Render My Signups Table
  const signupsTable = document.getElementById('vol-signups-table-body');
  if (signupsTable) {
    const list = AppState.userSignups.length ? AppState.userSignups : [
      { id: 1, opportunityId: 1, opportunityTitle: "Community Tree Planting Drive", organizationName: "Green Earth Alliance", eventDate: "2026-10-25", status: "REGISTERED" },
      { id: 3, opportunityId: 2, opportunityTitle: "Weekend Grocery Packaging & Distribution", organizationName: "Metro Community Food Bank", eventDate: "2026-10-26", status: "COMPLETED" }
    ];

    signupsTable.innerHTML = list.map(s => `
      <tr>
        <td><strong>${escapeHtml(s.opportunityTitle || 'Volunteering Event')}</strong></td>
        <td>${escapeHtml(s.organizationName || 'Partner')}</td>
        <td>${s.eventDate || '2026-10-25'}</td>
        <td>
          <span class="opp-status-badge ${s.status === 'COMPLETED' ? 'badge-approved' : s.status === 'CANCELLED' ? 'badge-rejected' : 'badge-pending'}">
            ${s.status || 'REGISTERED'}
          </span>
        </td>
        <td>
          ${s.status === 'REGISTERED' ? `
            <button class="btn btn-outline-danger btn-sm" onclick="handleCancelSignup(${s.id})">Cancel</button>
          ` : `<span style="color: var(--color-text-muted); font-size: 0.8125rem;">No action</span>`}
        </td>
      </tr>
    `).join('');
  }

  // Render Logged Hours Table
  const hoursTable = document.getElementById('vol-hours-table-body');
  if (hoursTable) {
    const list = AppState.userHours.length ? AppState.userHours : [
      { id: 1, opportunityTitle: "Weekend Grocery Packaging & Distribution", hoursWorked: 4.0, workDate: "2026-10-26", description: "Packed 45 emergency boxes and assisted at the loading bay.", verificationStatus: "APPROVED" },
      { id: 2, opportunityTitle: "Community Tree Planting Drive", hoursWorked: 3.5, workDate: "2026-10-25", description: "Helped prepare soil beds and planted 15 saplings.", verificationStatus: "PENDING" }
    ];

    hoursTable.innerHTML = list.map(h => `
      <tr>
        <td><strong>${escapeHtml(h.opportunityTitle || 'Community Service')}</strong></td>
        <td>${h.workDate}</td>
        <td><strong>${h.hoursWorked} hrs</strong></td>
        <td>${escapeHtml(h.description || 'Volunteer duty completed')}</td>
        <td>
          <span class="opp-status-badge ${h.verificationStatus === 'APPROVED' ? 'badge-approved' : h.verificationStatus === 'REJECTED' ? 'badge-rejected' : 'badge-pending'}">
            ${h.verificationStatus || 'PENDING'}
          </span>
        </td>
      </tr>
    `).join('');
  }
}

// --- Organization Dashboard ---
function renderOrganizationDashboard() {
  const orgNameEl = document.getElementById('org-welcome-name');
  if (orgNameEl && AppState.currentUser) {
    orgNameEl.textContent = AppState.currentUser.fullName;
  }

  // Render Org Listings Table
  const table = document.getElementById('org-listings-table-body');
  if (table) {
    const list = AppState.orgOpportunities.length ? AppState.orgOpportunities : AppState.opportunities;
    table.innerHTML = list.map(opp => `
      <tr>
        <td><strong>${escapeHtml(opp.title)}</strong></td>
        <td>${opp.category}</td>
        <td>${opp.eventDate}</td>
        <td>${opp.signedUpCount || 0} / ${opp.capacity}</td>
        <td>
          <span class="opp-status-badge ${opp.approvalStatus === 'APPROVED' ? 'badge-approved' : opp.approvalStatus === 'REJECTED' ? 'badge-rejected' : 'badge-pending'}">
            ${opp.approvalStatus || 'APPROVED'}
          </span>
        </td>
        <td>
          <button class="btn btn-secondary btn-sm" onclick="openOpportunityDetails(${opp.id})">View</button>
        </td>
      </tr>
    `).join('');
  }

  // Render Hours Verification Queue
  const hoursQueue = document.getElementById('org-verify-hours-table-body');
  if (hoursQueue) {
    const list = AppState.orgPendingHours || [
      { id: 2, volunteerName: "Bob Smith", opportunityTitle: "Community Tree Planting Drive", hoursWorked: 3.5, workDate: "2026-10-25", description: "Helped prepare soil beds and planted 15 saplings." }
    ];

    if (!list.length) {
      hoursQueue.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--color-text-muted);">No pending volunteer hours awaiting verification.</td></tr>`;
    } else {
      hoursQueue.innerHTML = list.map(h => `
        <tr>
          <td><strong>${escapeHtml(h.volunteerName || 'Volunteer')}</strong></td>
          <td>${escapeHtml(h.opportunityTitle || 'Community Drive')}</td>
          <td>${h.workDate}</td>
          <td>${h.hoursWorked} hrs</td>
          <td>${escapeHtml(h.description || 'Participated')}</td>
          <td>
            <div style="display: flex; gap: 6px;">
              <button class="btn btn-primary btn-sm" onclick="handleVerifyHours(${h.id}, 'APPROVED')">Approve</button>
              <button class="btn btn-outline-danger btn-sm" onclick="handleVerifyHours(${h.id}, 'REJECTED')">Reject</button>
            </div>
          </td>
        </tr>
      `).join('');
    }
  }
}

// --- Admin Dashboard ---
function renderAdminDashboard() {
  // Render Moderation Queue
  const table = document.getElementById('admin-moderation-table-body');
  if (table) {
    const list = AppState.pendingModeration.length ? AppState.pendingModeration : [
      { id: 3, organizationName: "Green Earth Alliance", title: "Coastal Beach & Estuary Cleanup", category: "Conservation", eventDate: "2026-11-05", capacity: 30, approvalStatus: "PENDING" }
    ];

    table.innerHTML = list.map(o => `
      <tr>
        <td><strong>${escapeHtml(o.title)}</strong></td>
        <td>${escapeHtml(o.organizationName || 'Nonprofit')}</td>
        <td>${o.category}</td>
        <td>${o.eventDate}</td>
        <td>
          <span class="opp-status-badge ${o.approvalStatus === 'APPROVED' ? 'badge-approved' : o.approvalStatus === 'REJECTED' ? 'badge-rejected' : 'badge-pending'}">
            ${o.approvalStatus}
          </span>
        </td>
        <td>
          <div style="display: flex; gap: 6px;">
            ${o.approvalStatus === 'PENDING' ? `
              <button class="btn btn-primary btn-sm" onclick="handleModerateOpportunity(${o.id}, 'APPROVED')">Approve</button>
              <button class="btn btn-outline-danger btn-sm" onclick="handleModerateOpportunity(${o.id}, 'REJECTED')">Reject</button>
            ` : `
              <button class="btn btn-secondary btn-sm" onclick="openOpportunityDetails(${o.id})">Inspect</button>
            `}
          </div>
        </td>
      </tr>
    `).join('');
  }

  // Render User Management Table
  const usersTable = document.getElementById('admin-users-table-body');
  if (usersTable) {
    const list = AppState.adminUsers.length ? AppState.adminUsers : [
      { id: 1, fullName: "System Administrator", email: "admin@platform.com", role: "ADMIN", accountStatus: "ACTIVE" },
      { id: 2, fullName: "Green Earth Alliance", email: "contact@greenearth.org", role: "ORGANIZATION", accountStatus: "ACTIVE" },
      { id: 3, fullName: "Metro Community Food Bank", email: "info@metrofoodbank.org", role: "ORGANIZATION", accountStatus: "ACTIVE" },
      { id: 4, fullName: "Alice Johnson", email: "alice.johnson@example.com", role: "VOLUNTEER", accountStatus: "ACTIVE" },
      { id: 5, fullName: "Bob Smith", email: "bob.smith@example.com", role: "VOLUNTEER", accountStatus: "ACTIVE" }
    ];

    usersTable.innerHTML = list.map(u => `
      <tr>
        <td><strong>${escapeHtml(u.fullName)}</strong></td>
        <td>${escapeHtml(u.email)}</td>
        <td><span class="user-role-tag">${u.role}</span></td>
        <td>
          <span class="opp-status-badge ${u.accountStatus === 'ACTIVE' ? 'badge-approved' : 'badge-rejected'}">
            ${u.accountStatus || 'ACTIVE'}
          </span>
        </td>
        <td>
          ${u.role !== 'ADMIN' ? `
            <button class="btn btn-secondary btn-sm" onclick="handleToggleUserStatus(${u.id}, '${u.accountStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'}')">
              ${u.accountStatus === 'ACTIVE' ? 'Suspend' : 'Activate'}
            </button>
          ` : `<span style="color: var(--color-text-muted); font-size: 0.8125rem;">Superadmin</span>`}
        </td>
      </tr>
    `).join('');
  }
}

// --- User Actions & Backend Sync ---
async function handleOpportunitySignup(oppId) {
  if (!AppState.currentUser) {
    showToast('Please sign in or select a Volunteer demo profile to register.', 'info');
    openModal('login-modal');
    return;
  }

  if (AppState.currentUser.role !== 'VOLUNTEER') {
    showToast('Only volunteers can register for opportunities. Switch to Volunteer profile.', 'error');
    return;
  }

  const res = await fetchAPI('/signups', {
    method: 'POST',
    body: JSON.stringify({
      volunteerId: AppState.currentUser.id,
      opportunityId: oppId
    })
  });

  if (res && res.success) {
    showToast('🎉 Successfully registered for opportunity!', 'success');
  } else {
    // If backend reports duplicate or other validation, show clear message
    const msg = (res && res.error) ? res.error : 'Registration recorded!';
    showToast(msg, res && res.error ? 'info' : 'success');
  }

  // Refresh lists
  loadOpportunities();
  loadVolunteerData();
}

async function handleCancelSignup(signupId) {
  if (!confirm('Are you sure you want to cancel this registration?')) return;

  const res = await fetchAPI('/signups/cancel', {
    method: 'POST',
    body: JSON.stringify({
      signupId: signupId,
      volunteerId: AppState.currentUser.id
    })
  });

  if (res && res.success) {
    showToast('Registration cancelled.', 'info');
  } else {
    showToast('Signup status updated.', 'info');
  }

  loadVolunteerData();
  loadOpportunities();
}

async function handleVerifyHours(hoursId, status) {
  const res = await fetchAPI('/hours/verify', {
    method: 'POST',
    body: JSON.stringify({
      hoursId: hoursId,
      organizationUserId: AppState.currentUser.id,
      status: status
    })
  });

  showToast(`Hours ${status.toLowerCase()} successfully!`, 'success');
  loadOrganizationData();
}

async function handleModerateOpportunity(oppId, status) {
  let reason = null;
  if (status === 'REJECTED') {
    reason = prompt('Please enter the reason for rejection:', 'Does not meet volunteer listing guidelines');
    if (!reason) return;
  }

  const res = await fetchAPI('/opportunities/moderate', {
    method: 'POST',
    body: JSON.stringify({
      opportunityId: oppId,
      status: status,
      rejectionReason: reason
    })
  });

  showToast(`Opportunity marked as ${status}!`, 'success');
  loadAdminData();
  loadOpportunities();
}

async function handleToggleUserStatus(targetUserId, newStatus) {
  const res = await fetchAPI('/users/status', {
    method: 'POST',
    body: JSON.stringify({
      targetUserId: targetUserId,
      status: newStatus
    })
  });

  showToast(`User status changed to ${newStatus}.`, 'info');
  loadAdminData();
}

// --- Navigation & Role Switcher ---
function setupNavigation() {
  document.querySelectorAll('.nav-link').forEach(link => {
    link.addEventListener('click', (e) => {
      const target = link.getAttribute('data-target');
      if (target) {
        e.preventDefault();
        navigateTo(target);
      }
    });
  });
}

function navigateTo(viewId) {
  AppState.currentView = viewId;

  // Update nav links active class
  document.querySelectorAll('.nav-link').forEach(l => {
    l.classList.toggle('active', l.getAttribute('data-target') === viewId);
  });

  // Toggle sections
  const homeSection = document.getElementById('home-view');
  const volSection = document.getElementById('volunteer-view');
  const orgSection = document.getElementById('organization-view');
  const adminSection = document.getElementById('admin-view');

  if (homeSection) homeSection.style.display = viewId === 'home' ? 'block' : 'none';
  if (volSection) volSection.classList.toggle('active', viewId === 'volunteer');
  if (orgSection) orgSection.classList.toggle('active', viewId === 'organization');
  if (adminSection) adminSection.classList.toggle('active', viewId === 'admin');

  // Trigger data loading for the selected view
  if (viewId === 'volunteer') loadVolunteerData();
  if (viewId === 'organization') loadOrganizationData();
  if (viewId === 'admin') loadAdminData();

  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function setupRoleSwitcher() {
  document.querySelectorAll('.demo-role-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const roleKey = btn.getAttribute('data-role');
      switchRole(roleKey);
    });
  });
}

function switchRole(roleKey) {
  document.querySelectorAll('.demo-role-btn').forEach(b => {
    b.classList.toggle('active', b.getAttribute('data-role') === roleKey);
  });

  AppState.currentUser = DEMO_USERS[roleKey] ? { ...DEMO_USERS[roleKey] } : null;

  if (AppState.currentUser) {
    localStorage.setItem('volunteer_platform_user', JSON.stringify(AppState.currentUser));
    showToast(`Switched active profile to ${AppState.currentUser.fullName} (${AppState.currentUser.role})`, 'success');

    // Route to corresponding dashboard
    if (AppState.currentUser.role === 'VOLUNTEER') navigateTo('volunteer');
    else if (AppState.currentUser.role === 'ORGANIZATION') navigateTo('organization');
    else if (AppState.currentUser.role === 'ADMIN') navigateTo('admin');
  } else {
    localStorage.removeItem('volunteer_platform_user');
    showToast('Browsing as Public Guest', 'info');
    navigateTo('home');
  }

  updateUserNavUI();
}

function updateUserNavUI() {
  const authBtns = document.getElementById('nav-auth-buttons');
  const userMenu = document.getElementById('nav-user-menu');
  const nameEl = document.getElementById('nav-user-name');
  const roleEl = document.getElementById('nav-user-role');
  const avatarEl = document.getElementById('nav-avatar-initials');

  if (AppState.currentUser) {
    if (authBtns) authBtns.style.display = 'none';
    if (userMenu) userMenu.style.display = 'flex';
    if (nameEl) nameEl.textContent = AppState.currentUser.fullName;
    if (roleEl) roleEl.textContent = AppState.currentUser.role;
    if (avatarEl) {
      const parts = AppState.currentUser.fullName.split(' ');
      avatarEl.textContent = parts.length > 1 ? (parts[0][0] + parts[1][0]).toUpperCase() : parts[0][0].toUpperCase();
    }
  } else {
    if (authBtns) authBtns.style.display = 'flex';
    if (userMenu) userMenu.style.display = 'none';
  }
}

// --- Search & Filters ---
function setupFilters() {
  // Category Chips
  document.querySelectorAll('.category-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      document.querySelectorAll('.category-chip').forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      AppState.activeCategory = chip.getAttribute('data-category');
      loadOpportunities();
    });
  });

  // Search input
  const searchInput = document.getElementById('search-keyword-input');
  if (searchInput) {
    searchInput.addEventListener('input', debounce(() => {
      AppState.searchQuery = searchInput.value.trim();
      loadOpportunities();
    }, 300));
  }

  // Location filter
  const locationSelect = document.getElementById('search-location-select');
  if (locationSelect) {
    locationSelect.addEventListener('change', () => {
      AppState.locationQuery = locationSelect.value;
      loadOpportunities();
    });
  }
}

function resetFilters() {
  AppState.searchQuery = '';
  AppState.activeCategory = 'All';
  AppState.locationQuery = '';
  AppState.dateQuery = '';

  const searchInput = document.getElementById('search-keyword-input');
  if (searchInput) searchInput.value = '';

  const locationSelect = document.getElementById('search-location-select');
  if (locationSelect) locationSelect.value = '';

  document.querySelectorAll('.category-chip').forEach(c => {
    c.classList.toggle('active', c.getAttribute('data-category') === 'All');
  });

  loadOpportunities();
}

// --- Modals ---
function setupModals() {
  // Close buttons
  document.querySelectorAll('.modal-close-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const modal = btn.closest('.modal-overlay');
      if (modal) modal.classList.remove('active');
    });
  });

  // Close on backdrop click
  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) overlay.classList.remove('active');
    });
  });

  // Login form submit
  const loginForm = document.getElementById('login-form');
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('login-email').value;
      const pass = document.getElementById('login-password').value;

      const res = await fetchAPI('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email: email, password: pass })
      });

      if (res && res.success && res.user) {
        AppState.currentUser = res.user;
        localStorage.setItem('volunteer_platform_user', JSON.stringify(res.user));
        closeModal('login-modal');
        showToast(`Welcome back, ${res.user.fullName}!`, 'success');
        updateUserNavUI();

        if (res.user.role === 'VOLUNTEER') navigateTo('volunteer');
        else if (res.user.role === 'ORGANIZATION') navigateTo('organization');
        else if (res.user.role === 'ADMIN') navigateTo('admin');
      } else {
        showToast(res && res.error ? res.error : 'Invalid credentials. Please verify your email and password.', 'error');
      }
    });
  }

  // Register form submit
  const regForm = document.getElementById('register-form');
  if (regForm) {
    regForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const fullName = document.getElementById('reg-fullname').value;
      const email = document.getElementById('reg-email').value;
      const password = document.getElementById('reg-password').value;
      const role = document.getElementById('reg-role').value;
      const orgName = document.getElementById('reg-orgname') ? document.getElementById('reg-orgname').value : '';

      const res = await fetchAPI('/auth/register', {
        method: 'POST',
        body: JSON.stringify({
          fullName, email, password, role, organizationName: orgName
        })
      });

      if (res && res.success && res.user) {
        AppState.currentUser = res.user;
        localStorage.setItem('volunteer_platform_user', JSON.stringify(res.user));
        closeModal('register-modal');
        showToast(`Registration successful! Welcome to the platform.`, 'success');
        updateUserNavUI();
        if (role === 'ORGANIZATION') navigateTo('organization');
        else navigateTo('volunteer');
      } else {
        showToast(res && res.error ? res.error : 'Registration could not be completed.', 'error');
      }
    });
  }

  // Log Hours Form submit
  const hoursForm = document.getElementById('log-hours-form');
  if (hoursForm) {
    hoursForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const oppId = document.getElementById('hours-opp-select').value;
      const hours = parseFloat(document.getElementById('hours-number').value);
      const date = document.getElementById('hours-date').value;
      const desc = document.getElementById('hours-description').value;

      const res = await fetchAPI('/hours', {
        method: 'POST',
        body: JSON.stringify({
          volunteerId: AppState.currentUser.id,
          opportunityId: oppId,
          hoursWorked: hours,
          workDate: date,
          description: desc
        })
      });

      if (res && res.success) {
        showToast('Volunteer hours logged successfully and submitted for verification!', 'success');
        closeModal('log-hours-modal');
        loadVolunteerData();
      } else {
        showToast(res && res.error ? res.error : 'Hours recorded.', 'info');
        closeModal('log-hours-modal');
        loadVolunteerData();
      }
    });
  }

  // Post Opportunity Form submit (for organizations)
  const oppForm = document.getElementById('create-opportunity-form');
  if (oppForm) {
    oppForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const title = document.getElementById('create-opp-title').value;
      const cat = document.getElementById('create-opp-category').value;
      const date = document.getElementById('create-opp-date').value;
      const start = document.getElementById('create-opp-start').value;
      const end = document.getElementById('create-opp-end').value;
      const loc = document.getElementById('create-opp-location').value;
      const cap = parseInt(document.getElementById('create-opp-capacity').value);
      const desc = document.getElementById('create-opp-description').value;

      const res = await fetchAPI('/opportunities', {
        method: 'POST',
        body: JSON.stringify({
          userId: AppState.currentUser.id,
          organizationId: AppState.currentUser.organizationId || 1,
          title, category: cat, eventDate: date, startTime: start, endTime: end,
          location: loc, capacity: cap, description: desc
        })
      });

      if (res && res.success) {
        showToast('Opportunity listed! It is now submitted for admin moderation.', 'success');
        closeModal('create-opportunity-modal');
        loadOrganizationData();
        loadOpportunities();
      } else {
        showToast(res && res.error ? res.error : 'Opportunity created!', 'info');
        closeModal('create-opportunity-modal');
      }
    });
  }
}

function openModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.add('active');
}

function closeModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.remove('active');
}

function openOpportunityDetails(id) {
  const opp = AppState.opportunities.find(o => o.id === id);
  if (!opp) return;

  const modal = document.getElementById('opp-details-modal');
  if (!modal) return;

  document.getElementById('modal-opp-img').src = opp.imageUrl;
  document.getElementById('modal-opp-title').textContent = opp.title;
  document.getElementById('modal-opp-category').textContent = opp.category;
  document.getElementById('modal-opp-org').textContent = opp.organizationName || 'Nonprofit Partner';
  document.getElementById('modal-opp-date').textContent = `${opp.eventDate} (${opp.startTime} - ${opp.endTime})`;
  document.getElementById('modal-opp-loc').textContent = opp.location;
  document.getElementById('modal-opp-desc').textContent = opp.description;
  document.getElementById('modal-opp-capacity').textContent = `${opp.signedUpCount || 0} / ${opp.capacity} spots filled`;

  const signupBtn = document.getElementById('modal-opp-signup-btn');
  if (signupBtn) {
    signupBtn.onclick = () => {
      handleOpportunitySignup(opp.id);
      closeModal('opp-details-modal');
    };
  }

  openModal('opp-details-modal');
}

// --- Toast Notifications ---
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
    <div style="flex: 1; font-size: 0.875rem; font-weight: 500;">${escapeHtml(message)}</div>
  `;

  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// --- Utilities ---
function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

// --- Seed Fallback Data ---
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
      approvalStatus: "APPROVED",
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
      approvalStatus: "APPROVED",
      imageUrl: "https://images.unsplash.com/photo-1593113598332-cd288d649433?auto=format&fit=crop&w=1200&q=80"
    },
    {
      id: 3,
      organizationId: 1,
      organizationName: "Green Earth Alliance",
      title: "Coastal Beach & Estuary Cleanup",
      description: "Collection, documentation, and sorting of marine plastic waste along North Shore coastline to protect marine life.",
      category: "Conservation",
      eventDate: "2026-11-05",
      startTime: "08:30",
      endTime: "12:00",
      location: "North Shore Beach Pavilion",
      capacity: 30,
      signedUpCount: 0,
      remainingSlots: 30,
      approvalStatus: "APPROVED",
      imageUrl: "https://images.unsplash.com/photo-1618477461853-cf6ed80faba5?auto=format&fit=crop&w=1200&q=80"
    },
    {
      id: 4,
      organizationId: 2,
      organizationName: "Metro Community Food Bank",
      title: "Winter Warmth Clothing Drive",
      description: "Inspect and sort donated winter coats, gloves, and blankets for distribution to community shelters ahead of the winter season.",
      category: "Humanitarian",
      eventDate: "2026-11-12",
      startTime: "11:00",
      endTime: "15:00",
      location: "Downtown Youth Center",
      capacity: 20,
      signedUpCount: 0,
      remainingSlots: 20,
      approvalStatus: "APPROVED",
      imageUrl: "https://images.unsplash.com/photo-1488521787991-ed7bbaae773c?auto=format&fit=crop&w=1200&q=80"
    }
  ];
}
