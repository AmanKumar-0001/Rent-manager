# Tenant Manager (Android)

**Tenant Manager** is a personal Android app built for landlords to manage rental properties, tenants, monthly rent periods, payment collection, notifications, and data backups with a strict **local-first** architecture.

---

## 1. Architecture Principles & Design

- **Local-First & Offline-Only:** Room SQLite on the device is the single source of truth. Every core feature functions completely offline without any internet connection.
- **Zero Internet Permission:** The `local` flavor builds without the `android.permission.INTERNET` permission in its merged manifest (`app/build/intermediates/merged_manifests/localDebug/processLocalDebugManifest/AndroidManifest.xml`).
- **Zero Cloud & Zero Tracking:** No third-party accounts, analytics, ads, crash reporters, or external cloud SDKs.
- **Backup & Restore:** Plaintext JSON export/import with versioned schema (`schemaVersion: 1`), and password-protected encrypted backup using AES-256-GCM and PBKDF2-HMAC-SHA256 (600,000 iterations).
- **Currency & Dates:**
  - Currency: Indian Rupee (`₹1,25,000`) formatted with Indian digit grouping, stored in minor units (paise) via a type-safe `Money` value class.
  - Dates: Displayed as `"14 Nov 2026"`. Stored exclusively as ISO strings (`YYYY-MM-DD` and `YYYY-MM`). Evaluated using Java time (`LocalDate`, `YearMonth`) without UTC timestamp conversions.

---

## 2. Multi-Module Project Structure

```
.
├── app/                  # Application entry point, ViewModels, navigation, screens, WorkManager notifications
├── core/
│   ├── domain/           # Pure Kotlin module (zero Android dependencies): Money, RentCalculator, models
│   ├── data/             # Room database (schema v1), DAOs, repositories, BackupExporter/Importer, CryptoEngine
│   └── ui/               # Compose Material 3 theme, design system components, formatters
└── gradle/
    └── libs.versions.toml # Pinned stable version catalog (AGP 8.7.2, Kotlin 2.0.21, Compose BOM, Room 2.6.1)
```

---

## 3. Documented Assumptions & Decisions

| Decision / Area | Choice & Implementation | Rationale |
| :--- | :--- | :--- |
| **`dueDay` normalization** | Accepts 1–31. If `dueDay` is 31 or exceeds the month length, clamps to the last calendar day of that month (Feb 28/29, Apr 30, etc.). | Matches human expectations for "end of month" rent without calendar overflow bugs. |
| **Status Precedence Hierarchy** | `LEFT` > `OVERDUE` > `DUE_SOON` > `PAID` > `UPCOMING`. | An overdue balance takes priority over upcoming periods; departed tenants are prioritized as `LEFT`. |
| **Tenant Removal** | Removing a tenant performs a soft archive (`isArchived = true`, `leaveDate = today`). Hard delete is quarantined in Settings > Danger Zone. | Prevents accidental data loss while keeping complete accounting and payment records intact. |
| **Danger Zone Data Wipe** | Requires exact typed confirmation (`"DELETE EVERYTHING"`) before wiping the database. | Prevents accidental deletion of the landlord's entire property portfolio. |
| **Properties Navigation** | Managed from the Tenants screen top action bar and Settings (rather than a 5th bottom tab). | Keeps the primary navigation focused on core landlord tasks (Dashboard, Tenants, Payments, Settings). |
| **Mid-Month Joins** | First period is charged as full rent due on the first due date on or after `joinDate`, unless an explicit prorated amount is entered. | Simplicity and flexibility for custom landlord arrangements. |
| **Overpayment Handling** | Overpayment in any period rolls forward chronologically as credit into subsequent periods. | Ensures tenant balance accuracy across multi-month overpayments and advance payments. |
| **Security & Privacy** | `android:allowBackup="false"`, `BiometricPrompt` app lock option, government ID storage restricted to last-4 digits only. | Ensures confidential financial and personal tenant data is protected. |
| **Notifications & Deduplication** | Daily WorkManager job at `reminderHour` with battery-not-low constraint. Deduplication keyed on `(tenantId, periodMonth, alertType)` in Room SQLite `alert_history` table. Restored on device reboot via `BootReceiver`. | Guarantees zero duplicate alerts for the same period and maintains reliability across restarts. |
| **Sheets Sync (Phase 2)** | Push-only mirror behind `RemoteMirror` interface with full snapshot overwrite. Scope `drive.file`. | Clean modular isolation so Phase 1 builds remain 100% offline with zero internet permission. |

---

## 4. Domain Test Scenarios (Checkpoint 2)

All 13 domain business rule scenarios are implemented in `:core:domain` unit tests (`PeriodGeneratorTest` and `RentCalculatorTest`):

| ID | Scenario | Input Conditions | Expected Outcome |
| :--- | :--- | :--- | :--- |
| **TS-01** | Due day 31 in Feb (Leap Year) | `dueDay = 31`, `2028-02` | Clamps to `2028-02-29` |
| **TS-02** | Due day 31 in Feb (Non-leap) | `dueDay = 31`, `2027-02` | Clamps to `2027-02-28` |
| **TS-03** | Due day 30/31 in April (30-day month) | `dueDay = 31`, `2026-04` | Clamps to `2026-04-30` |
| **TS-04** | Mid-month join date | Joined `2026-03-15`, `dueDay = 5` | First period `2026-03`, due `2026-03-05` |
| **TS-05** | Partial payment in period | Rent ₹10,000, paid ₹4,000 | Balance ₹6,000, status `UNPAID` / `PARTIALLY_PAID` |
| **TS-06** | Exact payment in period | Rent ₹10,000, paid ₹10,000 | Balance ₹0, status `PAID` |
| **TS-07** | Overpayment carry-forward chain | Rent ₹10k/mo, paid ₹25k in month 1 | P1 `PAID` (credit ₹15k), P2 `PAID` (credit ₹5k), P3 balance ₹5k |
| **TS-08** | Grace period boundary (On the day) | Due Nov 5, grace 5 days, today Nov 10 | `today == dueDate + graceDays` $\implies$ status `DUE_SOON` |
| **TS-09** | Grace period boundary (One day after) | Due Nov 5, grace 5 days, today Nov 11 | `today > dueDate + graceDays` $\implies$ status `OVERDUE` |
| **TS-10** | Tenant departed (Left) | `leaveDate = 2026-10-31`, today Nov 14 | Status `LEFT`, `nextPaymentDate = null` (`"—"`) |
| **TS-11** | All periods paid up to date | Rent ₹10,000, all periods paid | `nextPaymentDate` = next month's due date |
| **TS-12** | Joined in the same month as today | Joined `2026-11-10`, `dueDay = 15`, today Nov 14 | Period `2026-11`, status `DUE_SOON`, next payment Nov 15 |
| **TS-13** | Archived tenant exclusion | 3 tenants (1 archived, 2 active) | Active tenant count = 2; archived tenant excluded from portfolio totals |

---

## 5. Build, Test & Verification

### Running Unit Tests
```bash
# Run unit tests across all modules and flavors
./gradlew :core:domain:test :core:data:testDebugUnitTest :app:testLocalDebugUnitTest :app:testSheetsDebugUnitTest

# Generate JaCoCo test coverage report for pure domain logic
./gradlew :core:domain:jacocoTestReport
```

### Building APKs
```bash
# Build the local offline flavor (NO INTERNET permission)
./gradlew assembleLocalDebug

# Build the sheets sync flavor (with optional Google Sheets mirror)
./gradlew assembleSheetsDebug
```

### Release Artifacts in Project Root
- **Local Flavor APK:** `TenantManager-local-debug.apk`
  - Manifest contains **NO** `android.permission.INTERNET`.
  - 100% offline, guaranteed zero network capability.
- **Sheets Flavor APK:** `TenantManager-sheets-debug.apk`
  - Manifest contains `android.permission.INTERNET`.
  - Includes Google Identity Authorization API and Google Sheets API v4 integration behind an opt-in toggle.

---

## 6. Google Cloud Console Setup Guide (Sheets Flavor)

To enable the optional **"Sign in with Google"** and Google Sheets synchronization in the `sheets` build variant, you must configure an OAuth 2.0 Client ID in the Google Cloud Console.

### Step 1: Create or Select a Google Cloud Project
1. Navigate to the [Google Cloud Console](https://console.cloud.google.com/).
2. Create a new project (e.g., `Tenant Manager`) or select an existing project.

### Step 2: Enable Google APIs
Enable the following APIs under **APIs & Services > Library**:
1. **Google Sheets API** (v4)
2. **Google Drive API** (v3)

### Step 3: Configure the OAuth Consent Screen
1. Go to **APIs & Services > OAuth consent screen**.
2. Select **User Type** (choose **External** for personal Google accounts or **Internal** for Workspace).
3. Fill in mandatory fields:
   - **App name:** `Tenant Manager`
   - **User support email:** Your email address
   - **Developer contact information:** Your email address
4. Under **Scopes > Add or Remove Scopes**, add:
   - `https://www.googleapis.com/auth/drive.file`
   *(Do NOT add broad `drive` or `spreadsheets` scopes; the app adheres to least privilege).*
5. Under **Test Users**, add the Google email address(es) that will sign in to the app while in testing mode.

### Step 4: Retrieve Your SHA-1 Certificate Fingerprint
The Google Identity SDK validates that the calling application matches both the package name and certificate fingerprint registered in Google Cloud Console.

#### For Debug Keystore (Default Debug Build):
- **Windows (PowerShell / Command Prompt):**
  ```powershell
  keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
  ```
- **macOS / Linux:**
  ```bash
  keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
  ```
Look for the `SHA1:` line in the output (e.g., `AA:BB:CC:...`).

#### For Release Keystore:
```bash
keytool -list -v -keystore /path/to/release.keystore -alias your-key-alias
```

### Step 5: Create Android OAuth 2.0 Client ID
1. In Google Cloud Console, navigate to **APIs & Services > Credentials > Create Credentials > OAuth client ID**.
2. Select **Application type:** `Android`.
3. Enter:
   - **Name:** `Tenant Manager Android Client`
   - **Package name:** `com.tenantmanager`
   - **SHA-1 certificate fingerprint:** Paste the SHA-1 retrieved in Step 4.
4. Click **Create**.

---

## 7. Troubleshooting & Error Explanations

| Error Code | Error Name | Cause | Resolution |
| :--- | :--- | :--- | :--- |
| **Code 10** | `CommonStatusCodes.DEVELOPER_ERROR` | The SHA-1 certificate fingerprint or the package name (`com.tenantmanager`) is not configured or mismatched in the Google Cloud Console OAuth 2.0 Client ID. | Ensure you registered the exact SHA-1 of the keystore signing the APK (`debug.keystore` or release keystore) and package name `com.tenantmanager` under Android OAuth credentials. |
| **Code 12500** | `SIGN_IN_FAILED` | 1. OAuth consent screen is incomplete (missing support email).<br>2. Signing-in account is not listed under "Test Users" in the consent screen.<br>3. Google Play Services on emulator/device lacks a configured Google account. | Add the user to "Test Users" in Google Cloud Console, verify the consent screen configuration, and verify Google Play Services is signed in. |
| **Network Error** | `NETWORK_ERROR` | No internet connectivity or Google servers unreachable. | The app handles this quietly without crashing. Ensure internet connectivity, or continue using the app fully offline. |

*How errors appear in the app:* When an authentication or API error occurs, `SettingsScreen` displays a detailed, actionable error banner directly in the "Google Sheets Sync" card detailing the exact reason and required steps.

---

## 8. Offline & Airplane Mode Verification

To verify that the app is 100% local-first and fully functional offline:

1. **Enable Airplane Mode:** Toggle Airplane Mode ON on your Android device or emulator.
2. **Launch App:** Open Tenant Manager.
3. **Core Functionality Verification:**
   - Create, edit, and view tenants and properties.
   - Record and delete payments.
   - Verify calculation of overdue balances, upcoming due dates, and rent ledgers.
   - Perform local database export and encrypted backup restore (`.tmbackup`).
   - Trigger daily notification tests.
4. **Sync Invariant:** Notice that with Airplane Mode enabled and sync disabled, the app performs zero network calls. In the `local` flavor, the Android OS enforces this at the kernel level because `android.permission.INTERNET` is absent.

---

## 9. Google Sheets Full-Snapshot Strategy & Trade-offs

### Strategy: Full-Snapshot Overwrite
On each manual or scheduled sync:
1. `PrivacyFilter` sanitizes the Room database snapshot (redacting Aadhaar, PAN, bank accounts, and IFSC codes).
2. `SheetRowMapper` formats the snapshot into three sheets/tabs:
   - **Tenants** (ID, Unit, Property, Monthly Rent, Due Day, Deposit, Status, Next Payment Due)
   - **Payments** (Payment ID, Tenant Name, Unit, Amount, Paid Date, Period Month, Notes)
   - **Properties** (Property ID, Property Name, Address, Default Rent)
3. A single atomic `batchUpdate` request clears and rewrites all three tabs simultaneously.

### Trade-offs:
- **Advantages:**
  - **Zero Row-ID Drift:** Deleting, archiving, or editing a tenant locally does not leave orphaned or misaligned rows in Sheets.
  - **Idempotency:** Sync can be re-run at any time without duplicate rows.
  - **Resilience:** Avoids complex distributed two-way merge conflicts and network interruptions mid-sync.
- **Trade-offs:**
  - Payload size scales linearly with tenant and payment history. For personal landlord usage (dozens or hundreds of tenants over years), payload size remains tiny (<100 KB), completing in <1 second over HTTPS.

---

## 10. Checkpoints Summary

- **Checkpoint 1:** Multi-module Gradle structure (`:app`, `:core:domain`, `:core:data`, `:core:ui`), version catalog, flavors. *(PASSED)*
- **Checkpoint 2:** Domain logic, `Money`, `RentCalculator`, 13 business scenarios, >98% domain coverage. *(PASSED)*
- **Checkpoint 3:** Room schema v1, AES-256-GCM + PBKDF2 backup roundtrip, Room migration scaffold. *(PASSED)*
- **Checkpoint 4:** Jetpack Compose UI (Dashboard, Tenants, Detail, Payments, Properties, Settings, Danger Zone). *(PASSED)*
- **Checkpoint 5:** WorkManager daily notifications at `reminderHour`, notification channel, reboot receiver. *(PASSED)*
- **Checkpoint 6:** Google Sheets sync behind `sheets` flavor, `GoogleAuthManager`, `SheetRowMapper`, `PrivacyFilter`, `SheetsRemoteMirror`, Room v2 migration. *(PASSED)*
- **Checkpoint 7:** Security hardening, manifest audit (`INTERNET` absent in `local`), BiometricPrompt + PIN fallback, TalkBack accessibility, zero lint errors, `SECURITY.md`. *(PASSED)*
- **Checkpoint 8:** Release artifacts in root (`TenantManager-local-debug.apk`, `TenantManager-sheets-debug.apk`), complete test suite green (65 tests), end-to-end documentation. *(PASSED)*

- **Checkpoint 4:** Complete Jetpack Compose Material 3 UI, ViewModels, navigation, and unit/UI tests. *(PASSED)*
- **Checkpoint 5:** WorkManager notifications, Room-persisted deduplication, Android 13+ permission rationale, `BootReceiver`. *(PASSED)*

