package com.cryptocompare.helpers

import com.cryptocompare.helpers.util.PriceFormatConstants
import java.util.Locale
import kotlin.math.abs

/**
 * Разброс и спред в процентах: два знака после запятой.
 *
 * Раньше спред выводился форматом «%.4f%%» и давал «53.5235%» — четыре знака
 * это ложная точность, цены до неё всё равно не доживают. Очень мелкие, но
 * ненулевые значения не схлопываются в «0.00%»: для них показываем порог,
 * иначе разброс выглядит отсутствующим там, где он есть.
 */
fun Double.toPercentString(): String {
    if (isNaN() || isInfinite()) return PriceFormatConstants.NON_FINITE_PLACEHOLDER
    if (this == 0.0) return PriceFormatConstants.ZERO_PERCENT

    // сравнивать готовую строку с «0.00%» нельзя: отрицательное значение
    // форматируется в «-0.00%» и мимо такой проверки проходит
    if (abs(this) < PriceFormatConstants.PERCENT_PRECISION) {
        val sign = if (this < 0) PriceFormatConstants.MINUS else ""
        return sign + PriceFormatConstants.BELOW_PRECISION_PERCENT
    }

    return String.format(Locale.US, PriceFormatConstants.PERCENT_FORMAT, this)
}

/**
 * Заметен ли разброс. Ниже порога разница между биржами тонет в комиссиях,
 * и подсвечивать её как возможность нельзя.
 */
fun Double.isNotableSpread(): Boolean = abs(this) >= PriceFormatConstants.NOTABLE_SPREAD_PERCENT
