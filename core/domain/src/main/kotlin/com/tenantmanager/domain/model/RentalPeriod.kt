package com.tenantmanager.domain.model

import java.time.LocalDate
import java.time.YearMonth

enum class PeriodStatus {
    PAID,
    PARTIALLY_PAID,
    UNPAID
}

data class RentalPeriod(
    val periodMonth: YearMonth,
    val dueDate: LocalDate,
    val expectedRent: Money,
    val directPaymentsTotal: Money,
    val creditApplied: Money,
    val totalPaid: Money,
    val balance: Money,
    val status: PeriodStatus
)

