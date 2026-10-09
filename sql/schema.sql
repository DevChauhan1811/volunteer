-- ====================================================================
-- Online Volunteer Management Platform — Database Schema (DDL)
-- Database Engine: MySQL 8.4+ / InnoDB
-- Character Set: utf8mb4 / Collation: utf8mb4_unicode_ci
-- ====================================================================

CREATE DATABASE IF NOT EXISTS volunteer_platform_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE volunteer_platform_db;

-- --------------------------------------------------------------------
-- 1. Table: users
-- Stores all system actors: Admins, Organizations, and Volunteers
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'ORGANIZATION', 'VOLUNTEER') NOT NULL,
    account_status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_role (role),
    INDEX idx_users_status (account_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- 2. Table: organizations
-- Organization profile linked 1-to-1 with a user having role='ORGANIZATION'
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS organizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    organization_name VARCHAR(150) NOT NULL,
    description TEXT,
    contact_information VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_organizations_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_org_name (organization_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- 3. Table: opportunities
-- Volunteer listings created by organizations, moderated by Admins
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS opportunities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    event_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    location VARCHAR(255) NOT NULL,
    capacity INT NOT NULL,
    approval_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_opportunities_org FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE RESTRICT,
    INDEX idx_opp_org (organization_id),
    INDEX idx_opp_status (approval_status),
    INDEX idx_opp_date (event_date),
    INDEX idx_opp_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- 4. Table: signups
-- Registration records for volunteers signing up for opportunities
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS signups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    volunteer_id BIGINT NOT NULL,
    opportunity_id BIGINT NOT NULL,
    registration_status ENUM('REGISTERED', 'CANCELLED', 'COMPLETED', 'ATTENDED', 'NO_SHOW') NOT NULL DEFAULT 'REGISTERED',
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_signups_volunteer FOREIGN KEY (volunteer_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_signups_opportunity FOREIGN KEY (opportunity_id)
        REFERENCES opportunities(id) ON DELETE CASCADE,
    INDEX idx_signups_vol (volunteer_id),
    INDEX idx_signups_opp (opportunity_id),
    INDEX idx_signups_status (registration_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- 5. Table: volunteer_hours
-- Work hours logged by volunteers, verified by organizations
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS volunteer_hours (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    volunteer_id BIGINT NOT NULL,
    opportunity_id BIGINT NOT NULL,
    hours_worked DECIMAL(5, 2) NOT NULL,
    work_date DATE NOT NULL,
    description TEXT NOT NULL,
    verification_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    verified_by BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hours_volunteer FOREIGN KEY (volunteer_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_hours_opportunity FOREIGN KEY (opportunity_id)
        REFERENCES opportunities(id) ON DELETE CASCADE,
    CONSTRAINT fk_hours_verified_by FOREIGN KEY (verified_by)
        REFERENCES organizations(id) ON DELETE SET NULL,
    INDEX idx_hours_vol (volunteer_id),
    INDEX idx_hours_opp (opportunity_id),
    INDEX idx_hours_status (verification_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- 6. Table: messages
-- Messaging between volunteers and organizations regarding opportunities
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    opportunity_id BIGINT NULL,
    message_text TEXT NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_recipient FOREIGN KEY (recipient_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_opp FOREIGN KEY (opportunity_id)
        REFERENCES opportunities(id) ON DELETE SET NULL,
    INDEX idx_messages_pair (sender_id, recipient_id),
    INDEX idx_messages_opp (opportunity_id),
    INDEX idx_messages_sent (sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- 7. Table: system_settings
-- System-wide administrative key-value configurations
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    CONSTRAINT fk_settings_user FOREIGN KEY (updated_by)
        REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
