package com.tenantmanager.domain.model

import java.time.LocalDate

data class TenantLedger(
    val tenant: Tenant,
    val periods: List<RentalPeriod>,
    val totalPaid: Money,
    val totalBalance: Money,
    val status: TenantStatus,
    val nextPaymentDate: LocalDate?
)

data class PortfolioSummary(
    val activeTenantsCount: Int,
    val totalExpectedCurrentMonth: Money,
    val totalCollectedCurrentMonth: Money,
    val totalOverdue: Money
)

