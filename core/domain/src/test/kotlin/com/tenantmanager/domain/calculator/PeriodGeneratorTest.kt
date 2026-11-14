package com.tenantmanager.domain.calculator

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PeriodGeneratorTest {

    @Test
    fun testResolveDueDate_leapYearFebruary() {
        // TS-01: Due day 31 in Feb 2028 (leap year)
        val month = YearMonth.of(2028, 2)
        val dueDate = PeriodGenerator.resolveDueDate(month, 31)
        assertThat(dueDate).isEqualTo(LocalDate.of(2028, 2, 29))
    }

    @Test
    fun testResolveDueDate_nonLeapYearFebruary() {
        // TS-02: Due day 31 in Feb 2027 (non-leap year)
        val month = YearMonth.of(2027, 2)
        val dueDate = PeriodGenerator.resolveDueDate(month, 31)
        assertThat(dueDate).isEqualTo(LocalDate.of(2027, 2, 28))
    }

    @Test
    fun testResolveDueDate_thirtyDayMonth() {
        // TS-03: Due day 31 or 30 in April 2026
        val month = YearMonth.of(2026, 4)
        val dueDate31 = PeriodGenerator.resolveDueDate(month, 31)
        val dueDate30 = PeriodGenerator.resolveDueDate(month, 30)
        assertThat(dueDate31).isEqualTo(LocalDate.of(2026, 4, 30))
        assertThat(dueDate30).isEqualTo(LocalDate.of(2026, 4, 30))
    }

    @Test
    fun testResolveDueDate_standardDay() {
        val month = YearMonth.of(2026, 5)
        val dueDate5 = PeriodGenerator.resolveDueDate(month, 5)
        assertThat(dueDate5).isEqualTo(LocalDate.of(2026, 5, 5))
    }

    @Test
    fun testGeneratePeriodMonths_activeTenant() {
        val join = LocalDate.of(2026, 1, 15)
        val asOf = LocalDate.of(2026, 4, 10)
        val months = PeriodGenerator.generatePeriodMonths(join, null, asOf)

        assertThat(months).containsExactly(
            YearMonth.of(2026, 1),
            YearMonth.of(2026, 2),
            YearMonth.of(2026, 3),
            YearMonth.of(2026, 4)
        ).inOrder()
    }

    @Test
    fun testGeneratePeriodMonths_futureAsOfDateBeforeJoin() {
        val join = LocalDate.of(2026, 5, 1)
        val asOf = LocalDate.of(2026, 3, 10)
        val months = PeriodGenerator.generatePeriodMonths(join, null, asOf)

        assertThat(months).containsExactly(YearMonth.of(2026, 5))
    }

    @Test
    fun testGeneratePeriodMonths_tenantWithLeaveDate() {
        val join = LocalDate.of(2026, 1, 1)
        val leave = LocalDate.of(2026, 3, 15)
        val asOf = LocalDate.of(2026, 6, 1)
        val months = PeriodGenerator.generatePeriodMonths(join, leave, asOf)

        assertThat(months).containsExactly(
            YearMonth.of(2026, 1),
            YearMonth.of(2026, 2),
            YearMonth.of(2026, 3)
        ).inOrder()
    }

    @Test
    fun testGeneratePeriodMonths_invalidLeaveBeforeJoin() {
        val join = LocalDate.of(2026, 5, 1)
        val leave = LocalDate.of(2026, 3, 1)
        val months = PeriodGenerator.generatePeriodMonths(join, leave, LocalDate.of(2026, 5, 1))

        assertThat(months).isEmpty()
    }
}

