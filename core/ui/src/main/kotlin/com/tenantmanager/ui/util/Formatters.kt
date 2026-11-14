package com.tenantmanager.ui.util

import com.tenantmanager.domain.model.Money
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {

    private val displayDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    private val displayMonthFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

    fun formatDate(date: LocalDate?): String {
        return date?.format(displayDateFormatter) ?: "—"
    }

    fun formatDisplayDate(date: LocalDate?): String {
        return formatDate(date)
    }

    fun formatDisplayDate(dateIsoString: String?): String {
        if (dateIsoString == null) return "—"
        return try {
            LocalDate.parse(dateIsoString).format(displayDateFormatter)
        } catch (e: Exception) {
            dateIsoString
        }
    }

    fun formatPeriodMonth(month: YearMonth): String {
        return month.format(displayMonthFormatter)
    }

    fun formatCurrency(money: Money, symbol: String = "₹"): String {
        return money.formatINR(symbol)
    }
}
