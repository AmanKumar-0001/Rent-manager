package com.tenantmanager.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoneyTest {

    @Test
    fun testMoneyArithmetic() {
        val m1 = Money.ofRupees(100)
        val m2 = Money.ofRupees(50)

        assertThat((m1 + m2).minorUnits).isEqualTo(15000L)
        assertThat((m1 - m2).minorUnits).isEqualTo(5000L)
        assertThat((m2 * 2L).minorUnits).isEqualTo(10000L)
        assertThat((m2 * 3).minorUnits).isEqualTo(15000L)
    }

    @Test
    fun testComparisonAndZero() {
        val zero = Money.ZERO
        val positive = Money.ofPaise(500)
        val negative = Money.ofPaise(-500)

        assertThat(zero.isZero).isTrue()
        assertThat(positive.isZero).isFalse()
        assertThat(positive.isPositive).isTrue()
        assertThat(negative.isPositive).isFalse()

        assertThat(positive > zero).isTrue()
        assertThat(negative < zero).isTrue()
        assertThat(Money.ofPaise(100) == Money.ofRupees(1)).isTrue()
    }

    @Test
    fun testFormatINRGrouping() {
        // Spec: Currency INR with Indian digit grouping (₹1,25,000)
        assertThat(Money.ofRupees(125000).formatINR()).isEqualTo("₹1,25,000")
        assertThat(Money.ofRupees(500).formatINR()).isEqualTo("₹500")
        assertThat(Money.ofRupees(1000).formatINR()).isEqualTo("₹1,000")
        assertThat(Money.ofRupees(10000).formatINR()).isEqualTo("₹10,000")
        assertThat(Money.ofRupees(100000).formatINR()).isEqualTo("₹1,00,000")
        assertThat(Money.ofRupees(12500000).formatINR("$")).isEqualTo("$1,25,00,000")

        // With paise
        assertThat(Money.ofPaise(12500050).formatINR()).isEqualTo("₹1,25,000.50")
        assertThat(Money.ofPaise(12500005).formatINR()).isEqualTo("₹1,25,000.05")

        // Negative
        assertThat(Money.ofRupees(-1500).formatINR()).isEqualTo("-₹1,500")
        assertThat(Money.ofPaise(-150025).formatINR()).isEqualTo("-₹1,500.25")
    }
}

