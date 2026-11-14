# Security Policy & Architecture - Tenant Manager

"Tenant Manager" is designed around strict **local-first privacy, least-privilege cloud access, and zero-trust local boundaries**. This document details the security model, cryptographic primitives, transmission boundaries, OAuth scopes, and threat analysis.

---

## 1. Local Storage Security & App Boundaries

### Sandboxing & Database Storage
- **Single Source of Truth:** All tenant, property, payment, and ledger state resides in a local SQLite database (`app_database.db`) managed via Room.
- **OS-Level Isolation:** The database and DataStore preferences are stored exclusively in internal app-private storage (`/data/user/0/com.tenantmanager/`). Android Linux user-ID (UID) isolation prevents other applications from reading or accessing these files on unrooted devices.
- **Backup Disablement:** Standard Android cloud backup and ADB backup are strictly disabled:
  - `android:allowBackup="false"` in `AndroidManifest.xml`
  - `android:dataExtractionRules="@xml/data_extraction_rules"` excluding all paths from both `<cloud-backup>` and `<device-transfer>`
  - `android:fullBackupContent="@xml/backup_rules"` excluding all domains

### App Lock & Authentication
- **BiometricPrompt with PIN Fallback:**
  - Configured with `BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL`.
  - Supports biometric sensors (fingerprint, Class 3 face unlock) with automatic fallback to device PIN/pattern/password.
  - App lock is disabled by default and can be toggled by the user in Settings. When enabled, authentication is required on launch and resume before any data or screens are rendered.

### Encrypted Backups (PBKDF2 + AES-256-GCM)
- Exported backup files (`.tmbackup`) are encrypted at rest using:
  - **Key Derivation:** PBKDF2WithHmacSHA256 with 100,000 iterations and a 128-bit (16-byte) cryptographically secure random salt generated via `SecureRandom`.
  - **Encryption Cipher:** AES-256-GCM (Galois/Counter Mode) with a 96-bit (12-byte) random initialization vector (IV) per export and a 128-bit authentication tag.
  - **Integrity Guarantee:** Any tampering with ciphertext, salt, or IV causes GCM tag validation to fail with an `AEADBadTagException`, preventing corrupted or malicious payload restoration.

---

## 2. Network & Transmission Boundaries

### Flavor-Level Architectural Isolation
The codebase enforces network isolation at the Gradle build variant layer:

| Dimension | `local` Flavor | `sheets` Flavor |
| :--- | :--- | :--- |
| **`INTERNET` Permission** | **Strictly Removed** (`tools:node="remove"`) | **Declared** (`android.permission.INTERNET`) |
| **Google Play Services Auth** | Not used; stubbed `NoOpRemoteMirror` | Used for Credential Manager & Sheets API |
| **Network Capability** | Physically cannot make socket/HTTP calls | HTTPS only to official Google APIs |
| **Merged Manifest** | Zero network permissions | Only `INTERNET` and `ACCESS_NETWORK_STATE` |

- **Verification:** An automated test (`ManifestAuditTest`) checks both source manifests and merged manifests during build validation, guaranteeing `INTERNET` never enters the `local` flavor.

### Push-Only Synchronization
- Google Sheets integration is strictly **push-only**. Data is mirrored from Room $\to$ Sheets.
- The app never reads or imports rows from Google Sheets into the local Room database, eliminating remote command/data injection vectors, remote state tampering, and split-brain conflicts.
- Synchronization is a full snapshot replacement using Google Sheets API v4 `batchUpdate`, ensuring idempotent mirroring without row-ID drift.

---

## 3. OAuth 2.0 Scope & Identity (`drive.file`)

### Principle of Least Privilege
- **Scope Used:** `https://www.googleapis.com/auth/drive.file`
- **Forbidden Scopes:**
  - `https://www.googleapis.com/auth/drive` (Forbidden — grants access to all user files)
  - `https://www.googleapis.com/auth/spreadsheets` (Forbidden — grants access to all user spreadsheets)
- **Scope Contract:** `drive.file` restricts app access exclusively to files and spreadsheets created by "Tenant Manager" or opened by the user through the app. The app cannot list, view, or modify any existing personal documents, photos, or spreadsheets in the user's Google Drive.

### Token Lifecycle & Storage
- **Zero Token Persistence:** OAuth 2.0 access tokens are never stored in Room, DataStore, SharedPreferences, or unencrypted memory caches.
- **On-Demand Tokens:** Tokens are acquired dynamically via Google Identity Authorization API immediately prior to sync and discarded after the HTTP connection terminates.
- **Revocation Handling:** If the user revokes permissions from their Google Account security dashboard:
  - The API responds with HTTP 401/403.
  - `SheetsRemoteMirror` detects authorization revocation, records a non-identifying local sync failure, and updates `isGoogleSignedIn = false`.
  - The local database and app continue running offline without interruption.

---

## 4. Data Privacy & Redaction (`PrivacyFilter`)

Before any tenant snapshot is converted to Google Sheets rows, all records pass through `PrivacyFilter`:

1. **Government Identifiers Redacted:**
   - **Aadhaar Numbers:** 12-digit numbers matching `\b\d{4}\s?\d{4}\s?\d{4}\b` are redacted to `[REDACTED_AADHAAR]`.
   - **PAN Cards:** 10-character alphanumeric strings matching `\b[A-Z]{5}[0-9]{4}[A-Z]\b` are redacted to `[REDACTED_PAN]`.
2. **Banking Details Redacted:**
   - **Bank Account Numbers:** 9-18 digit account numbers are redacted to `[REDACTED_ACCOUNT]`.
   - **IFSC Codes:** 11-character codes matching `\b[A-Z]{4}0[A-Z0-9]{6}\b` are redacted to `[REDACTED_IFSC]`.
3. **Notes Exclusion:**
   - Notes are omitted by default (`""`).
   - Landlord notes are only synchronized if the user explicitly enables "Include notes in Sheet" in Settings.

---

## 5. Threat Model (STRIDE)

| Threat Category | Risk Description | Architectural Mitigation |
| :--- | :--- | :--- |
| **Spoofing** | Impersonation of Google account | Google Identity Authorization API verifies Google account token signatures directly on Google servers via TLS. |
| **Tampering** | Local database tampering | OS UID isolation; AES-256-GCM authenticated tags prevent modification of encrypted `.tmbackup` archives. |
| **Repudiation** | Unverified sync or payment operations | Local `SyncLogEntity` audit log records timestamps, status codes, and non-sensitive error summaries. |
| **Information Disclosure** | Data leak via cloud or network | `PrivacyFilter` strips PII/bank details; `drive.file` isolates drive files; `allowBackup=false` prevents cloud leak; `local` flavor lacks `INTERNET`. |
| **Denial of Service** | Network failures or rate limiting | Exponential backoff in WorkManager (`BackoffPolicy.EXPONENTIAL`); network errors fail quietly without blocking UI. |
| **Elevation of Privilege** | Unauthorized physical device access | BiometricPrompt with strong biometric and device PIN fallback blocks app access. |

---

## 6. Security Reporting
To report security vulnerabilities or concerns, please open a private GitHub advisory or contact the repository maintainer directly.

