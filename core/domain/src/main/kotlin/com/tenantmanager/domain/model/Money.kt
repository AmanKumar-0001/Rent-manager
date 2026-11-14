package com.tenantmanager.domain.model

@JvmInline
value class Money(val minorUnits: Long) : Comparable<Money> {
    operator fun plus(other: Money): Money = Money(Math.addExact(this.minorUnits, other.minorUnits))
    operator fun minus(other: Money): Money = Money(Math.subtractExact(this.minorUnits, other.minorUnits))
    operator fun times(multiplier: Long): Money = Money(Math.multiplyExact(this.minorUnits, multiplier))
    operator fun times(multiplier: Int): Money = Money(Math.multiplyExact(this.minorUnits, multiplier.toLong()))

    override fun compareTo(other: Money): Int = this.minorUnits.compareTo(other.minorUnits)

    val isZero: Boolean get() = minorUnits == 0L
    val isPositive: Boolean get() = minorUnits > 0L

    fun formatINR(currencySymbol: String = "₹"): String {
        val isNegative = minorUnits < 0
        val absMinor = if (isNegative) -minorUnits else minorUnits
        val rupees = absMinor / 100
        val paise = absMinor % 100

        val formattedRupees = formatIndianGrouping(rupees)
        val formatted = if (paise == 0L) {
            "$currencySymbol$formattedRupees"
        } else {
            val paiseStr = if (paise < 10) "0$paise" else "$paise"
            "$currencySymbol$formattedRupees.$paiseStr"
        }
        return if (isNegative) "-$formatted" else formatted
    }

    companion object {
        val ZERO = Money(0L)
        fun ofRupees(rupees: Long): Money = Money(Math.multiplyExact(rupees, 100L))
        fun ofPaise(paise: Long): Money = Money(paise)

        fun formatIndianGrouping(rupees: Long): String {
            if (rupees < 1000) return rupees.toString()
            val s = rupees.toString()
            val lastThree = s.substring(s.length - 3)
            val rest = s.substring(0, s.length - 3)
            val sb = StringBuilder()
            var count = 0
            for (i in rest.length - 1 downTo 0) {
                sb.append(rest[i])
                count++
                if (count == 2 && i > 0) {
                    sb.append(',')
                    count = 0
                }
            }
            return sb.reverse().toString() + "," + lastThree
        }
    }
}

