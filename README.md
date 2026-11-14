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
# Run unit tests across all modules
./gradlew :core:domain:test :core:data:testDebugUnitTest :app:testLocalDebugUnitTest

# Generate JaCoCo test coverage report for pure domain logic
./gradlew :core:domain:jacocoTestReport
```

### Building Local Debug APK
```bash
# Build the local debug APK
./gradlew assembleLocalDebug
```

### Local APK Outputs
- **Root APK copy:** `TenantManager-debug.apk` (20.8 MB)
- **Gradle output path:** `app/build/outputs/apk/local/debug/app-local-debug.apk`

---

## 6. Checkpoints Summary (Phase 1)

- **Checkpoint 1:** Multi-module architecture, pinned version catalog, flavor dimensions configured. *(PASSED)*
- **Checkpoint 2:** Pure-Kotlin domain models, `Money`, `RentCalculator`, 13 test scenarios with 98.65% line coverage. *(PASSED)*
- **Checkpoint 3:** Room schema v1, encrypted AES-GCM + PBKDF2 backup round-trip, migration scaffold. *(PASSED)*
- **Checkpoint 4:** Complete Jetpack Compose Material 3 UI, ViewModels, navigation, and unit/UI tests. *(PASSED)*
- **Checkpoint 5:** WorkManager notifications, Room-persisted deduplication, Android 13+ permission rationale, `BootReceiver`. *(PASSED)*
