# Property Follow-up CRM — Native Android Application (v1.1.0)

A lightweight, 100% offline-first native Android Customer Relationship Management (CRM) application designed specifically for real-estate consultants, property dealers, and brokers.

---

## 🌟 Enhanced Feature Set (v1.1.0)

### 1. Customer & Lead Management
- Full profile recording: Customer Name, Phone Number, Requirement (Buy / Sell / Rent), Property Type (Plot / Flat / House / Land / Commercial / Other), Preferred Location, Preferred Size (sq.ft / BHK), Preferred Contact Time, Budget in INR (`₹`), Lead Source, Assigned Agent, Next Action, and Notes.
- Inline duplicate phone-number warning with non-blocking confirmation.
- Filter and search by name, phone, lead stage, property type, and location.

### 2. Follow-ups vs. Site Visits (Appointments)
- **Separation of Concerns**: Follow-up calls and physical site visits are managed as separate entities.
- **Site Visit Records**: Scheduled site inspections with property address, date, time, notes, and appointment status (`Scheduled`, `Completed`, `Cancelled`, `Rescheduled`).
- **Follow-up Tasks**: Individual call schedules with independent lifecycles.

### 3. Call Outcome Recording & Interaction Timeline
- After dialing via `ACTION_DIAL`, record structured call outcomes:
  - `Answered`
  - `No Answer`
  - `Busy`
  - `Requested Callback`
  - `Interested`
  - `Not Interested`
  - `Site Visit Requested`
  - `Negotiation`
  - `Wrong Number`
- Saves notes and optionally schedules the next follow-up.
- **Chronological History Timeline**: Immutable audit trail of customer creation, calls, outcomes, site visits, rescheduling events, and stage updates.

### 4. Do Not Call (DNC) Compliance
- Explicit Do Not Call toggle per customer.
- Automatically excludes DNC customers from daily reminder notifications and active calling queues.
- Gates accidental dialing with confirmation prompt.
- Retains customer history while allowing manual authorized reactivation.
- Won, Lost, and Archived leads are also automatically excluded from calling reminders.

### 5. High-Reliability Reminder Engine & Grouped Notifications
- **9:00 AM Grouped Notification**: Computes unique customers and total tasks due today + overdue.
- **Notification Tap Navigation**: Opens combined screen showing:
  - **SECTION A: Due Today**
  - **SECTION B: Overdue**
- **Channel & Exact Alarm Reliability**: Detects notification channel status, checks `canScheduleExactAlarms()`, handles `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, reschedules on `BOOT_COMPLETED` and time/timezone shifts.
- **MIUI / HyperOS Troubleshooting**: In-app guide and shortcut intents for Autostart and Battery Optimization settings.

### 6. Security & Encrypted Backups
- **Application PIN Lock**: Optional 4-6 digit security PIN hashed with salted SHA-256.
- **Password-Protected Backups**: Strong AES-256-GCM authenticated encryption with PBKDF2WithHmacSHA256 key derivation.
- **Integrity Validation & Rollback**: Validates backup authenticity before import; atomic database transactions with automatic rollback on error.
- **Android Auto Backup Rules**: Explicitly excludes raw customer database from cloud auto-backup to preserve offline privacy.
- **CSV Export**: Clean spreadsheet export for Excel and Google Sheets.

---

## 🏗️ Technical Architecture

- **Language**: Kotlin 1.9.24
- **UI Toolkit**: Jetpack Compose BOM 2024.04.01 + Material 3 Dark Palette
- **Architecture**: MVVM with Kotlin Coroutines & Flow
- **Database**: AndroidX Room 2.6.1 (Version 2 with automatic `MIGRATION_1_2`)
- **Security**: AES-256-GCM + PBKDF2WithHmacSHA256
- **Storage**: Android Storage Access Framework (SAF)
- **Min SDK**: 26 (Android 8.0) | **Target SDK**: 34 (Android 14)

---

## 📦 Build & Release

### 1. Remote Google Colab Build
Open `property_crm_build.ipynb` on Colab to build both Debug and Signed Release APKs:
- APKs saved to `MyDrive/PropertyCRM/APKBuilds/`
- Checksums recorded in `MyDrive/PropertyCRM/APKBuilds/checksums_*.txt`
- Unit test reports in `MyDrive/PropertyCRM/TestReports/`
- Release notes in `MyDrive/PropertyCRM/ReleaseNotes/`

### 2. GitHub Actions CI
Every commit to `main` executes `.github/workflows/android.yml`:
- Runs Gradle unit tests
- Compiles Debug & Signed Release APKs
- Computes SHA-256 checksums
- Uploads APKs and test reports as workflow artifacts
