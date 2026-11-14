package com.tenantmanager.domain.calculator

import java.time.LocalDate
import java.time.YearMonth

object PeriodGenerator {

    fun resolveDueDate(month: YearMonth, dueDay: Int): LocalDate {
        val clampedDay = if (dueDay >= 31 || dueDay > month.lengthOfMonth()) {
            month.lengthOfMonth()
        } else {
            dueDay
        }
        return month.atDay(clampedDay)
    }

    fun generatePeriodMonths(
        joinDate: LocalDate,
        leaveDate: LocalDate?,
        asOfDate: LocalDate
    ): List<YearMonth> {
        val startMonth = YearMonth.from(joinDate)
        val endMonth = if (leaveDate != null) {
            YearMonth.from(leaveDate)
        } else {
            val asOfMonth = YearMonth.from(asOfDate)
            if (asOfMonth.isBefore(startMonth)) startMonth else asOfMonth
        }

        if (endMonth.isBefore(startMonth)) {
            return emptyList()
        }

        val months = mutableListOf<YearMonth>()
        var current = startMonth
        while (!current.isAfter(endMonth)) {
            months.add(current)
            current = current.plusMonths(1)
        }
        return months
    }
}

