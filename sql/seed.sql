-- ====================================================================
-- Online Volunteer Management Platform — Seed Data Script (DML)
-- Default password for all seed accounts is: Password@123
-- BCrypt Hash: $2a$12$4U51IufhzzaySdTZJkaTN.Vr6SD2ChTgSlVwnfq2lIVVtMIB1DS5C
-- ====================================================================

USE volunteer_platform_db;

-- Disable foreign key checks for clean re-seeding if needed
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE messages;
TRUNCATE TABLE volunteer_hours;
TRUNCATE TABLE signups;
TRUNCATE TABLE opportunities;
TRUNCATE TABLE organizations;
TRUNCATE TABLE system_settings;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- --------------------------------------------------------------------
-- 1. Insert Users (Admin, Organizations, Volunteers)
-- --------------------------------------------------------------------
INSERT INTO users (id, full_name, email, password_hash, role, account_status) VALUES
(1, 'System Administrator', 'admin@platform.com', '$2a$12$4U51IufhzzaySdTZJkaTN.Vr6SD2ChTgSlVwnfq2lIVVtMIB1DS5C', 'ADMIN', 'ACTIVE'),
(2, 'Green Earth Alliance', 'contact@greenearth.org', '$2a$12$4U51IufhzzaySdTZJkaTN.Vr6SD2ChTgSlVwnfq2lIVVtMIB1DS5C', 'ORGANIZATION', 'ACTIVE'),
(3, 'Metro Community Food Bank', 'info@metrofoodbank.org', '$2a$12$4U51IufhzzaySdTZJkaTN.Vr6SD2ChTgSlVwnfq2lIVVtMIB1DS5C', 'ORGANIZATION', 'ACTIVE'),
(4, 'Alice Johnson', 'alice.johnson@example.com', '$2a$12$4U51IufhzzaySdTZJkaTN.Vr6SD2ChTgSlVwnfq2lIVVtMIB1DS5C', 'VOLUNTEER', 'ACTIVE'),
(5, 'Bob Smith', 'bob.smith@example.com', '$2a$12$4U51IufhzzaySdTZJkaTN.Vr6SD2ChTgSlVwnfq2lIVVtMIB1DS5C', 'VOLUNTEER', 'ACTIVE'),
(6, 'Clara Davis', 'clara.davis@example.com', '$2a$12$4U51IufhzzaySdTZJkaTN.Vr6SD2ChTgSlVwnfq2lIVVtMIB1DS5C', 'VOLUNTEER', 'ACTIVE');

-- --------------------------------------------------------------------
-- 2. Insert Organizations
-- --------------------------------------------------------------------
INSERT INTO organizations (id, user_id, organization_name, description, contact_information) VALUES
(1, 2, 'Green Earth Alliance', 'Environmental non-profit focused on reforestation, habitat restoration, and urban greenery initiatives.', 'contact@greenearth.org | +1-555-0101'),
(2, 3, 'Metro Community Food Bank', 'Serving healthy meals, groceries, and essential supplies to underserved local families.', 'info@metrofoodbank.org | +1-555-0202');

-- --------------------------------------------------------------------
-- 3. Insert Opportunities
-- --------------------------------------------------------------------
INSERT INTO opportunities (id, organization_id, title, description, category, event_date, start_time, end_time, location, capacity, approval_status, rejection_reason) VALUES
(1, 1, 'Community Tree Planting Drive', 'Join us to plant over 300 native saplings in Riverside Park to combat local urban heat islands.', 'Environment', '2026-10-25', '09:00:00', '13:00:00', 'Riverside Park, Sector 4', 25, 'APPROVED', NULL),
(2, 2, 'Weekend Grocery Packaging & Distribution', 'Sort, pack, and distribute weekly emergency food kits to elderly residents and families in need.', 'Community Relief', '2026-10-26', '10:00:00', '14:00:00', 'Metro Warehouse, 42 Elm Street', 15, 'APPROVED', NULL),
(3, 1, 'Coastal Beach & Estuary Cleanup', 'Collection, documentation, and sorting of marine plastic waste along North Shore coastline.', 'Conservation', '2026-11-05', '08:30:00', '12:00:00', 'North Shore Beach Pavilion', 30, 'PENDING', NULL),
(4, 2, 'Winter Warmth Clothing Drive', 'Inspect and sort donated winter coats, gloves, and blankets for distribution to community shelters.', 'Humanitarian', '2026-11-12', '11:00:00', '15:00:00', 'Downtown Youth Center', 20, 'APPROVED', NULL);

-- --------------------------------------------------------------------
-- 4. Insert Volunteer Signups
-- --------------------------------------------------------------------
INSERT INTO signups (id, volunteer_id, opportunity_id, registration_status, registered_at) VALUES
(1, 4, 1, 'REGISTERED', '2026-10-01 10:00:00'),
(2, 5, 1, 'REGISTERED', '2026-10-02 11:15:00'),
(3, 4, 2, 'COMPLETED',  '2026-10-03 09:30:00'),
(4, 6, 2, 'REGISTERED', '2026-10-04 14:00:00');

-- --------------------------------------------------------------------
-- 5. Insert Volunteer Hours
-- --------------------------------------------------------------------
INSERT INTO volunteer_hours (id, volunteer_id, opportunity_id, hours_worked, work_date, description, verification_status, verified_by) VALUES
(1, 4, 2, 4.00, '2026-10-26', 'Packed 45 emergency boxes and assisted at the loading bay.', 'APPROVED', 2),
(2, 5, 1, 3.50, '2026-10-25', 'Helped prepare soil beds and planted 15 saplings.', 'PENDING', NULL);

-- --------------------------------------------------------------------
-- 6. Insert Messages
-- --------------------------------------------------------------------
INSERT INTO messages (id, sender_id, recipient_id, opportunity_id, message_text, sent_at, read_at) VALUES
(1, 4, 3, 2, 'Hello! Will parking be available near the loading bay?', '2026-10-24 14:30:00', '2026-10-24 15:00:00'),
(2, 3, 4, 2, 'Hi Alice, yes! Designated volunteer parking is available in Lot B right behind the warehouse.', '2026-10-24 15:10:00', '2026-10-24 15:15:00');

-- --------------------------------------------------------------------
-- 7. Insert System Settings
-- --------------------------------------------------------------------
INSERT INTO system_settings (setting_key, setting_value, updated_by) VALUES
('platform_name', 'Online Volunteer Management Platform', 1),
('max_volunteer_capacity', '100', 1),
('org_registration_approval_required', 'false', 1),
('allow_hour_self_submission', 'true', 1);
