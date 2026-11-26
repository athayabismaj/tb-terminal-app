package com.tbterminal.app.data.local.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Exact 2-decimal fixed-point representation stored as 64-bit integer minor units (value * 100).
 *
 * Keeps SQLite SUM, comparison, and ORDER BY operations exact without floating-point drift.
 */
@JvmInline
value class ScaledDecimal2(val minorUnits: Long) : Comparable<ScaledDecimal2> {
    fun toBigDecimal(): BigDecimal = BigDecimal.valueOf(minorUnits, SCALE)

    operator fun plus(other: ScaledDecimal2): ScaledDecimal2 =
        ScaledDecimal2(Math.addExact(minorUnits, other.minorUnits))

    operator fun minus(other: ScaledDecimal2): ScaledDecimal2 =
        ScaledDecimal2(Math.subtractExact(minorUnits, other.minorUnits))

    operator fun unaryMinus(): ScaledDecimal2 =
        ScaledDecimal2(Math.negateExact(minorUnits))

    override fun compareTo(other: ScaledDecimal2): Int = minorUnits.compareTo(other.minorUnits)

    val isZero: Boolean get() = minorUnits == 0L
    val isPositive: Boolean get() = minorUnits > 0L
    val isNegative: Boolean get() = minorUnits < 0L

    fun coerceAtLeastZero(): ScaledDecimal2 = if (minorUnits < 0L) ZERO else this

    companion object {
        const val SCALE = 2
        val ZERO = ScaledDecimal2(0L)

        /**
         * Converts [value] to exact minor units. Fails if [value] has more than 2 significant
         * decimal places (after stripping trailing zeros) or overflows 64-bit signed minor units.
         */
        fun ofExact(value: BigDecimal): ScaledDecimal2 {
            val normalized = if (value.compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal.ZERO
            } else {
                value.stripTrailingZeros()
            }
            require(normalized.scale() <= SCALE) {
                "Nilai melebihi $SCALE angka desimal: ${value.toPlainString()}"
            }
            val minorUnits = normalized
                .setScale(SCALE, RoundingMode.UNNECESSARY)
                .movePointRight(SCALE)
                .longValueExact()
            return ScaledDecimal2(minorUnits)
        }

        /**
         * Explicitly rounds [value] to 2 decimal places using [mode] before storing minor units.
         */
        fun ofRounded(
            value: BigDecimal,
            mode: RoundingMode = RoundingMode.HALF_UP
        ): ScaledDecimal2 {
            val minorUnits = value
                .setScale(SCALE, mode)
                .movePointRight(SCALE)
                .longValueExact()
            return ScaledDecimal2(minorUnits)
        }

        /**
         * Converts legacy SQLite REAL values during Room v6 -> v7 migration using HALF_UP to 2 decimals.
         */
        fun fromLegacyRealHalfUp(value: Double): ScaledDecimal2 {
            require(value.isFinite()) { "Nilai REAL tidak valid saat migrasi: $value" }
            return ofRounded(BigDecimal.valueOf(value), RoundingMode.HALF_UP)
        }

        fun ofMinorUnits(minorUnits: Long): ScaledDecimal2 = ScaledDecimal2(minorUnits)

        fun ofWhole(whole: Long): ScaledDecimal2 = ScaledDecimal2(Math.multiplyExact(whole, 100L))
    }
}
