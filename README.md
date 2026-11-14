# Collect Your Rent (Android)

<p align="center">
  <img src="docs/brand/logo-512.png" width="140" alt="Collect Your Rent Logo" />
</p>

<p align="center">
  <strong>Collect Your Rent</strong> is a personal, premium Android app designed for landlords to effortlessly track properties, tenants, rent periods, and payments with a strict <strong>local-first</strong> architecture.
</p>

<p align="center">
  <a href="#1-brand-identity--design-system">Brand Identity</a> •
  <a href="#2-architecture-principles">Architecture</a> •
  <a href="#3-ui-screens--visual-preview">Visual Preview</a> •
  <a href="#4-release-apks">Release APKs</a> •
  <a href="#5-security--offline-guarantee">Security & Offline</a> •
  <a href="#6-build--test-verification">Verification</a>
</p>

---

## 1. Brand Identity & Design System

### The Brand Concept
The identity for **Collect Your Rent** is built around dependability, clarity, and prosperity.
- **Mark:** A modern squircle badge framing a minimalist house silhouette with an arched open threshold, paired with a radiant golden coin stamped with the Indian Rupee symbol (`₹`).
- **Typography:** Android Material 3 typography with monospace tabular figures (`fontFeatureSettings = "tnum"`) to eliminate jitter and ensure exact decimal alignment across financial tables.
- **Dual-Channel Status Indicators:** Every status badge combines an unambiguous semantic icon with high-contrast text to strictly adhere to **WCAG 2.1 Success Criterion 1.4.1 (Use of Color)**:
  - **PAID:** Emerald Green with `CheckCircle` icon (✓)
  - **DUE SOON:** Warm Amber with `Schedule` / `Hourglass` icon (⏳)
  - **OVERDUE:** Brick Crimson with `Warning` icon (!)
  - **LEFT:** Slate Grey with `Logout` icon (←)

### Color Palette
| Token | Light Theme | Dark Theme | Purpose |
| :--- | :--- | :--- | :--- |
| **Primary (Royal Emerald)** | `#0F5132` | `#34D399` | Brand anchor, collection progress, affirmative actions |
| **Secondary (Slate Navy)** | `#1E293B` | `#94A3B8` | Neutral surfaces, headers, secondary buttons |
| **Accent (Warm Amber)** | `#D97706` | `#FBBF24` | Rupee medallion, due-soon rent warnings |
| **Danger (Brick Crimson)** | `#DC2626` | `#F87171` | Overdue balances, destructive confirmations |
| **Background / Surface** | `#F8FAFC` | `#0F172A` | Background cards, elevated containers |

### Adaptive Launcher Icon & Splash Screen
- **Adaptive Icon:** Full vector foreground (`ic_launcher_foreground.xml`), emerald base (`ic_launcher_background.xml`), and Android 13+ Material You monochrome themed icon (`ic_launcher_monochrome.xml`).
- **Android 12+ SplashScreen:** Implemented via `androidx.core:core-splashscreen:1.0.1` (`Theme.App.Starting`), eliminating cold-start blank screens and transitioning smoothly into the app theme.

---

## 2. Architecture Principles

- **Local-First & Offline-Only:** Room SQLite on device is the single source of truth. Every core feature functions completely offline.
- **Zero Internet Permission on Local Build:** The default `local` flavor does not declare or merge `android.permission.INTERNET`.
- **Package Stability:** Package name is strictly preserved as `com.tenantmanager` to ensure persistent Google OAuth compatibility across iterations.
- **Pure-Kotlin Domain:** `:core:domain` has zero Android dependencies, encapsulating all rent calculations, grace periods, leap years, and financial rules.
- **Haptic Tactile Feedback:** Subtle, pleasant confirmation vibration (`HapticHelper`) on recording payments and completing key actions, configurable via Settings.
- **Optional Push-Only Mirror:** The `sheets` flavor offers optional Google Sheets backup strictly scoped to `drive.file` (only accesses the spreadsheet it creates: `"Collect Your Rent - Data"`). Zero sensitive financial data (PAN, bank accounts) is ever exported.

---

## 3. UI Screens & Visual Preview

### Before & After Comparison

| Screen | Phase 1/2 (Before) | Phase 3 (After - Light & Dark) |
| :--- | :--- | :--- |
| **Dashboard** | Plain boxes, unstyled summary text | Executive Monthly Rent hero card, 76% animated collection progress, overdue alert card, and upcoming dues feed. |
| **Tenants List** | Basic text rows without avatars | Urgency-ordered cards (Overdue first), initials avatar, tabular rupee figures, filter chips, and accessible quick-action icons. |
| **Tenant Detail** | Standard unstyled list | Hero profile card with direct Call/SMS/Email actions, next payment due countdown, agreement details, and month-by-month ledger. |
| **Record Payment** | Centered basic dialog | Modal Bottom Sheet with quick-select chips (`Full Rent`, `Remaining Due`, `₹5,000`, `₹10,000`), bold tabular amount, date picker, and haptic feedback. |
| **Settings** | Long monolithic list | Grouped Material 3 cards: General Preferences (Haptics toggle, Currency, Grace days), Rent Notifications, Security (App Lock), Google Sheets Sync, and Danger Zone. |

*All screenshots are archived under [`docs/screenshots/before/`](file:///X:/ideee/tenent-app/docs/screenshots/before/) and [`docs/screenshots/after/`](file:///X:/ideee/tenent-app/docs/screenshots/after/).*

---

## 4. Release APKs

Pre-built installable APKs are provided directly in the repository root and Gradle outputs:

1. **`Collect Your Rent.apk`** (Local Flavor)
   - Size: ~21.8 MB
   - Merged Manifest: **Zero `android.permission.INTERNET`**
   - Location: `X:/ideee/tenent-app/Collect Your Rent.apk` (and `app/build/outputs/apk/local/debug/Collect Your Rent.apk`)

2. **`Collect Your Rent - Sheets.apk`** (Sheets Flavor)
   - Size: ~21.8 MB
   - Permissions: Includes `android.permission.INTERNET` for optional push-only Google Sheets backup.
   - Location: `X:/ideee/tenent-app/Collect Your Rent - Sheets.apk` (and `app/build/outputs/apk/sheets/debug/Collect Your Rent - Sheets.apk`)

---

## 5. Security & Offline Guarantee

### 100% Offline Manifest Verification
Run the following PowerShell verification to confirm the local build is completely air-gapped:
```powershell
Select-String -Path 'app/build/intermediates/packaged_manifests/localDebug/processLocalDebugManifestForPackage/AndroidManifest.xml' -Pattern 'INTERNET'
# Output: (Empty - no match found)
```

### Encryption & App Lock
- **AES-256-GCM + PBKDF2-HMAC-SHA256:** Encrypted backup exports require a landlord-defined passphrase (derived using 600,000 PBKDF2 iterations with salt and 12-byte GCM nonce).
- **Biometric App Lock:** Optional biometric authentication (Fingerprint, Face Unlock) with device PIN/Pattern fallback via `BiometricPrompt`.

---

## 6. Build & Test Verification

### Running Tests
```bash
# Run unit tests across all modules and flavors
./gradlew testLocalDebugUnitTest testSheetsDebugUnitTest :core:domain:test :core:data:testDebugUnitTest

# Run Android Lint (Zero errors)
./gradlew lintLocalDebug lintSheetsDebug
```

### Test Suite Summary
- **Domain Business Rules (`:core:domain`):** 13 comprehensive edge cases including leap year 2028-02-29, 30-day month clamping, overpayment chains, and grace day boundaries (100% passed).
- **Data & Crypto Roundtrip (`:core:data`):** AES-GCM encryption/decryption roundtrip, plaintext JSON backup exporter/importer with schema version 1, and Room schema validation (100% passed).
- **UI & Accessibility (`:app`):** Robolectric Compose UI tests for TenantCard, StatusChips, RecordPaymentDialog, DangerZone typed confirmation, and AppLock security (100% passed).
- **Total Test Count:** 65+ unit and UI tests passing cleanly.

---

## 7. License
Proprietary personal landlord tool. Built with Kotlin, Jetpack Compose, Room SQLite, and Google Material Design 3.
