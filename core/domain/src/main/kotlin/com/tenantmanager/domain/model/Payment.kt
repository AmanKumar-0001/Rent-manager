package com.tenantmanager.domain.model

import java.time.LocalDate
import java.time.YearMonth

data class Payment(
    val id: String,
    val tenantId: String,
    val amount: Money,
    val paidDate: LocalDate,
    val periodMonth: YearMonth,
    val notes: String? = null,
    val lastModified: Long = 0L
)

