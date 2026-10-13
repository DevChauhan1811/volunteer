# 🌟 MyVolunteer — Community Volunteer Management Platform

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-007396?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Apache%20Maven-3.9+-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.4%20LTS-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![FlatLaf](https://img.shields.io/badge/UI-FlatLaf%203.5-brightgreen)](https://www.formdev.com/flatlaf/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Theme](https://img.shields.io/badge/Theme-Sky%20Blue-38BDF8)](https://github.com/)

**MyVolunteer** is a full-stack, enterprise-grade volunteer management platform designed to connect passionate volunteers with verified non-profit organizations, charities, and community initiatives. 

The application offers both an **interactive, modern web application** (responsive Sky-Blue design with left-to-right sparkle animations) and a **native Java Swing desktop client** powered by a shared MySQL persistence layer and RESTful service architecture.

---

## 📸 Key Highlights & Features

### ✨ 1. Brand Experience & Sparkle Sweep Intro
- **Dynamic Intro Animation**: Sweeping sparkle wand illuminates and writes `MyVolunteer` character-by-character from left to right with particle trails and glowing text shadows before smoothly dissolving into the platform.
- **Sparkle Replay Button (`✨`)**: Convenient replay trigger located in the navigation bar and footer to experience the animation at any time.
- **Sky-Blue Design System**: Curated color palette (`#0284C7`, `#38BDF8`, `#0F172A`) paired with glassmorphism, responsive cards, and clean typography.

---

### 👥 2. Dedicated 4-Role Portal Architecture

| Role | Portal Capabilities |
| :--- | :--- |
| **Volunteer** | • Discover vetted community causes across categories (Environment, Healthcare, Education, Relief)<br>• **Full Program Registration Modal** (Emergency contact, phone, skills, accessibility notes, safety agreement)<br>• Manage active signups with instant **Cancel Registration** & slot release<br>• Log completed service hours for nonprofit validation |
| **Organization** | • Create and publish community volunteer opportunities with capacity limits<br>• Real-time applicant roster monitoring<br>• **Working Accept & Reject Hours Queue**: Verify or decline submitted volunteer service hours with immediate stat recalculation |
| **System Admin** | • Governance console and security oversight<br>• **Working Listing Moderation Queue**: Accept & publish listings to live search or reject listings with custom/preset feedback reasons<br>• Platform user accounts directory and system analytics |
| **Public Guest** | • Instant 1-click guest exploration to search initiatives and inspect community impact without credentials |

---

### 🔐 3. Dual Authentication & Security
- **Email & Password**: Salted and securely hashed using industry-standard **BCrypt**.
- **One-Time Passcode (OTP)**: Secure 6-digit challenge system with 5-minute countdown expiry, rate-limiting, and single-use challenge destruction to prevent replay attacks.
- **Strict Post-Logout Security**: Session and tokens are destroyed upon sign-out; users must re-authenticate to re-enter protected routes.

---

### ⚙️ 4. Real-World Program Registration & Moderation
- **Real Program Signups**: Decrements available slots dynamically and captures contact, emergency, and skill information.
- **Registration Cancellation**: Frees occupied slots and returns capacity back to community programs.
- **Admin Moderation Flow**:
  - `✓ Accept & Publish`: Validates listing and publishes it to the public discovery feed.
  - `✕ Reject`: Triggers modal with preset feedback (missing credentials, safety guidelines, schedule conflicts) or custom notes.
- **Nonprofit Hours Verification**:
  - `✓ Accept`: Increments volunteer's verified hours and updates platform-wide impact counters.
  - `✕ Reject`: Flags hours claim as rejected with transparent status tags.

---

## 🛠️ Technology Stack

- **Backend & Core Logic**: Java 21 LTS, Java HTTP Server API
- **Persistence & ORM**: MySQL 8.4 LTS, JDBC Connection Pooling
- **Security**: jBCrypt (0.4) for password hashing
- **Serialization**: Google Gson (2.11.0)
- **Frontend Web UI**: Vanilla HTML5, Modern CSS3 (Sky Blue Tokens, Keyframe Animations), Vanilla ES6 JavaScript
- **Desktop UI**: Java Swing with FlatLaf (3.5.4) Modern Theme
- **Build & Dependency Management**: Apache Maven 3.9+
- **Testing**: JUnit 5 (Jupiter 5.10.2)

---

## 📂 Project Structure

```plaintext
volunteer/
├── pom.xml                               # Maven project configuration (Java 21, dependencies, plugins)
├── start.bat                             # One-click Windows CMD launcher
├── run.ps1                               # PowerShell launcher with port & MySQL checks
├── sql/
│   ├── schema.sql                        # Database schema (tables, foreign keys, constraints)
│   └── seed.sql                          # Seed data (demo users, opportunities, organizations)
├── src/
│   ├── main/
│   │   ├── java/com/volunteer/
│   │   │   ├── app/Main.java             # Entry point (auto-starts Web Server or Desktop UI)
│   │   │   ├── config/                   # Database & application properties loader
│   │   │   ├── controller/               # MVC Controllers for UI & business workflows
│   │   │   ├── dao/                      # Data Access Objects (User, Opportunity, Signup, Hours)
│   │   │   ├── model/                    # Entity models (User, Opportunity, Volunteer, Organization)
│   │   │   ├── service/                  # Business service interfaces & implementations
│   │   │   ├── util/                     # DatabaseConnection, PasswordUtil (BCrypt)
│   │   │   ├── view/                     # Swing Desktop UI Panels & Dialogs
│   │   │   └── web/WebServer.java        # Embedded HTTP Web Server & REST API Handlers
│   │   └── resources/
│   │       ├── application.properties    # MySQL connection credentials
│   │       └── web/                      # Packaged web assets
│   └── test/java/com/volunteer/          # Automated JUnit 5 unit tests
└── web/
    ├── index.html                        # Single-page web application UI
    ├── css/style.css                     # Design tokens, Sky-Blue theme, Sparkle keyframes
    └── js/app.js                         # State management, API sync, Sparkle intro, Modals
```

---

## 🚀 Quick Start Guide

### 1. Prerequisites
- **Java Development Kit (JDK) 21** or later installed and configured (`java -version`).
- **Apache Maven 3.9+** (`mvn -version`).
- **MySQL Server 8.0+** running locally on port `3306`.

---

### 2. Database Setup

1. Open your MySQL terminal or GUI client (Workbench, DBeaver, HeidiSQL):
   ```bash
   mysql -u root -p
   ```
2. Execute the schema script:
   ```sql
   SOURCE sql/schema.sql;
   ```
3. Populate sample opportunities, non-profit partners, and seed users:
   ```sql
   SOURCE sql/seed.sql;
   ```
4. Verify or adjust database credentials in [`src/main/resources/application.properties`](file:///src/main/resources/application.properties):
   ```properties
   db.host=localhost
   db.port=3306
   db.name=volunteer_platform_db
   db.username=root
   db.password=YOUR_MYSQL_PASSWORD
   ```

---

### 3. Launching the Application

#### Option A: Windows Quick Launcher (Recommended)
Double-click [`start.bat`](file:///start.bat) or run from Command Prompt:
```cmd
start.bat
```

#### Option B: PowerShell Launcher
```powershell
.\run.ps1
```

#### Option C: Maven Command Line
```bash
mvn clean compile exec:java
```

Once started, open your web browser and navigate to:
👉 **`http://localhost:8080`**

---

### 4. Running the Desktop Swing UI
To launch the native Java Swing Desktop application rather than the web server:
```bash
mvn exec:java -Dexec.arguments=""
```

---

## 🔑 Pre-Configured Demo Accounts

For instant testing, use the following demo accounts or click the **"Use Demo Account"** quick-fill chip on the login portal:

| Portal Role | Email | Password | Primary Functions |
| :--- | :--- | :--- | :--- |
| **Volunteer** | `alice.johnson@example.com` | `password123` | Register for programs, cancel signups, log service hours |
| **Organization** | `contact@greenearth.org` | `password123` | Publish events, verify/reject volunteer hours |
| **Admin** | `admin@platform.com` | `password123` | Approve/reject listings with feedback, audit users |
| **Public Guest** | *(No credentials required)* | — | Browse live community causes & impact statistics |

---

## 📡 REST API Reference

The embedded Java HTTP server exposes the following REST endpoints under `/api`:

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Authenticate using email and BCrypt password |
| `POST` | `/api/auth/register` | Register new Volunteer or Organization account |
| `POST` | `/api/auth/otp/request` | Generate secure time-limited One-Time Passcode |
| `POST` | `/api/auth/otp/verify` | Validate OTP and issue authenticated session |
| `GET` | `/api/stats` | Retrieve platform-wide metrics (volunteers, hours, causes) |
| `GET` | `/api/opportunities` | Retrieve live volunteer opportunities with filters |
| `POST` | `/api/signups` | Register volunteer for an opportunity |
| `GET` | `/api/signups?volunteerId={id}` | Retrieve signups for a specific volunteer |
| `GET` | `/api/hours?volunteerId={id}` | Retrieve logged hours history |
| `POST` | `/api/hours` | Submit new volunteer hours for nonprofit verification |

---

## 🧪 Running Automated Tests

Run the JUnit 5 test suite to validate business services and DAO logic:
```bash
mvn test
```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) — see the LICENSE file for details.
