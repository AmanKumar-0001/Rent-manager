package com.tenantmanager.domain.sync

import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.model.Property
import com.tenantmanager.domain.model.Tenant

object PrivacyFilter {

    // Regex to match Indian PAN (5 letters, 4 digits, 1 letter)
    private val PAN_REGEX = Regex("[A-Z]{5}[0-9]{4}[A-Z]", RegexOption.IGNORE_CASE)
    // Regex to match 12-digit Aadhaar numbers (with optional spaces or dashes)
    private val AADHAAR_REGEX = Regex("\\b\\d{4}[ -]?\\d{4}[ -]?\\d{4}\\b")
    // Regex to match bank account numbers (9 to 18 consecutive digits)
    private val BANK_ACCOUNT_REGEX = Regex("\\b\\d{9,18}\\b")
    // Regex to match IFSC codes (4 letters, 0, 6 alphanumeric)
    private val IFSC_REGEX = Regex("[A-Z]{4}0[A-Z0-9]{6}", RegexOption.IGNORE_CASE)

    fun sanitizeText(text: String?): String? {
        if (text.isNullOrBlank()) return null

        var sanitized = text
        sanitized = AADHAAR_REGEX.replace(sanitized, "[REDACTED_AADHAAR]")
        sanitized = PAN_REGEX.replace(sanitized, "[REDACTED_PAN]")
        sanitized = IFSC_REGEX.replace(sanitized, "[REDACTED_IFSC]")
        sanitized = BANK_ACCOUNT_REGEX.replace(sanitized, "[REDACTED_ACCOUNT]")
        return sanitized
    }

    fun filterSnapshot(snapshot: SyncSnapshot): SyncSnapshot {
        val sanitizedTenants = snapshot.tenants.map { tenant ->
            val finalNotes = if (snapshot.includeNotes) {
                sanitizeText(tenant.notes)
            } else {
                null
            }
            tenant.copy(notes = finalNotes)
        }

        val sanitizedPayments = snapshot.payments.map { payment ->
            val finalNotes = if (snapshot.includeNotes) {
                sanitizeText(payment.notes)
            } else {
                null
            }
            payment.copy(notes = finalNotes)
        }

        return snapshot.copy(
            tenants = sanitizedTenants,
            payments = sanitizedPayments
        )
    }
}

