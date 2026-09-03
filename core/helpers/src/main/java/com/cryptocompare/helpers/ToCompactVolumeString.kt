package com.cryptocompare.helpers

import com.cryptocompare.helpers.util.PriceFormatConstants
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Объём за 24 часа в компактной записи: 98750000 -> «98.75M».
 *
 * Полное число здесь не нужно и вредно: в строке каталога «98750000.0» съедает
 * всю ширину и всё равно не читается — порядок величины по нему на глаз не берётся.
 * Хвостовые нули обрезаются, поэтому ровный миллион это «1M», а не «1.00M».
 */
fun Double.toCompactVolumeString(): String {
    if (isNaN() || isInfinite()) return PriceFormatConstants.NON_FINITE_PLACEHOLDER
    if (this == 0.0) return PriceFormatConstants.ZERO

    val units = PriceFormatConstants.VOLUME_UNITS
    var index = units.indexOfFirst { abs(this) >= it.first }.takeIf { it >= 0 } ?: units.lastIndex
    var mantissa = scaled(units[index].first)

    // округление может перекинуть через порог: 999_999 дало бы «1000K» вместо «1M»
    if (index > 0 && mantissa.abs() >= BigDecimal.valueOf(PriceFormatConstants.VOLUME_UNIT_STEP)) {
        index -= 1
        mantissa = scaled(units[index].first)
    }

    return mantissa.stripTrailingZeros().toPlainString() + units[index].second
}

private fun Double.scaled(unit: Double): BigDecimal =
    BigDecimal.valueOf(this / unit).setScale(PriceFormatConstants.VOLUME_SCALE, RoundingMode.HALF_UP)
