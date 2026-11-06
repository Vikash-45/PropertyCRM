# Property Follow-up CRM — Native Android Application

A lightweight, 100% offline-first native Android Customer Relationship Management (CRM) application tailored specifically for property dealers, real-estate consultants, and brokers.

---

## 🌟 Key Features

1. **Customer Management**:
   - Register customers with Name, Phone, Inquiry Reason, Property Type (Plot / Flat / House / Land / Commercial / Other), Preferred Location, Budget, Lead Source, First Visit Date, Customer Stage (New, Contacted, Site Visit, Negotiation, Won, Lost), and Notes.
   - Inline duplicate phone number detection warning (without blocking legitimate entries).
   - Edit, archive, unarchive, and delete customers.

2. **Automated Follow-up System**:
   - Creating a customer with a follow-up date automatically generates a pending follow-up task.
   - Complete follow-up history is preserved: Rescheduling creates a new pending record while archiving previous history.
   - 5 dedicated views: **Today**, **Overdue**, **Upcoming**, **Completed**, and **All**.
   - One-tap actions: Direct Dial (`android.intent.action.DIAL`), Mark Completed (with notes), Reschedule (new date/time/reason), and Add Another Follow-up.

3. **High-Priority Daily Notification Reminders**:
   - Default reminder time: **9:00 AM** local device time.
   - Calculates total pending calls for today and overdue calls.
   - Grouped notification: e.g., *"8 customers need follow-up today. 3 additional customers are overdue."*
   - Suppresses notifications when no follow-ups are due or overdue.
   - Tapping notification deep-links directly into the filtered "Today" customer list.
   - Privacy-first: Sensitive notes and phone numbers are hidden from lock screens (`VISIBILITY_PRIVATE`).
   - Automatically reschedules after device reboot (`BOOT_COMPLETED`) and time/timezone shifts.
   - Fallback inexact alarms if exact alarm permissions (`SCHEDULE_EXACT_ALARM`) are restricted.

4. **Data Safety & Backup (SAF)**:
   - Android Storage Access Framework (SAF) integration: User can pick any local folder or Google Drive directory.
   - **CSV Export**: Spreadsheet-friendly table for Excel and Google Sheets.
   - **JSON Complete Backup**: Full relational state including all customers and follow-up timeline.
   - **JSON Restore**: Validated schema, automatic duplicate merging by phone number, and foreign key reconstruction.
   - 100% Offline: Zero telemetry, zero external database servers, zero internet requirements.

5. **Design & Localization**:
   - Modern, professional Dark UI (Material 3).
   - Color coding: Emerald Green for completed tasks, Red/Coral for overdue tasks, Sky Blue for today's tasks.
   - Complete English and Hindi (हिन्दी) localization.

---

## 🏗️ Architecture & Technology Stack

- **Language**: Kotlin 1.9.24
- **UI Toolkit**: Jetpack Compose BOM 2024.04.01 + Material 3
- **Architecture**: MVVM (Model-View-ViewModel) with StateFlow
- **Database**: AndroidX Room 2.6.1 (with SQLite Foreign Keys, TypeConverters, Indexing, KSP)
- **Preferences**: Jetpack DataStore Preferences
- **Scheduling**: Android `AlarmManager` (with `canScheduleExactAlarms()` check) + `BroadcastReceiver` + WorkManager fallback
- **Serialization**: Google Gson 2.10.1
- **Target SDK**: 34 | **Min SDK**: 26

---

## 📁 Project Structure

```
PropertyCRM/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/propertycrm/app/
│   │   │   │   ├── PropertyCrmApp.kt          # Application initialization & alarm sync
│   │   │   │   ├── MainActivity.kt            # Deep-link navigation & Compose entry point
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/                 # Customer, FollowUp, Relations, BackupData
│   │   │   │   │   ├── local/                 # AppDatabase, DAOs, TypeConverters, DataStore
│   │   │   │   │   └── repository/            # Customer, FollowUp, Backup repositories
│   │   │   │   ├── notification/              # AlarmReceiver, BootReceiver, Scheduler, Manager
│   │   │   │   ├── ui/
│   │   │   │   │   ├── theme/                 # DarkColorScheme, Typography, Colors
│   │   │   │   │   ├── components/            # StatusChip, StageChip, EmptyState, Dialogs
│   │   │   │   │   ├── navigation/            # Screen routes & AppNavigation
│   │   │   │   │   ├── dashboard/             # Screen 1: Dashboard
│   │   │   │   │   ├── customer/              # Screen 2 & 4: Add/Edit, List, Details
│   │   │   │   │   ├── followup/              # Screen 3: Follow-up Management
│   │   │   │   │   └── settings/              # Screen 5: Settings, Reminders, SAF Backup
│   │   │   │   └── util/                      # DateUtils, PhoneUtils
│   │   │   └── res/
│   │   │       ├── values/strings.xml         # English UI Strings
│   │   │       └── values-hi/strings.xml      # Hindi UI Strings
│   │   └── test/java/com/propertycrm/app/     # DateUtils, PhoneUtils, Backup Unit Tests
├── .github/workflows/
│   └── android.yml                            # GitHub Actions CI build & verification
└── property_crm_build.ipynb                   # Colab automated build & Drive export notebook
```

---

## 🛠️ Remote Build & CI Pipeline

### Option 1: GitHub Actions (Automated)
Pushing to the `main` branch automatically triggers `.github/workflows/android.yml`, which:
1. Runs `./gradlew testDebugUnitTest`
2. Runs `./gradlew lintDebug`
3. Builds the debug APK `./gradlew assembleDebug`
4. Calculates and prints the SHA-256 checksum
5. Publishes artifacts (APK and test reports)

### Option 2: Google Colab Remote Build
Open `property_crm_build.ipynb` on Google Colab:
- Builds APK on high-performance cloud runtime
- Archives debug APK, SHA-256 checksum, test reports, and source zip directly to Google Drive:
  `MyDrive/PropertyCRM/APKBuilds/`
  `MyDrive/PropertyCRM/TestReports/`
  `MyDrive/PropertyCRM/SourceArchives/`
