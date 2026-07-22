package com.cryptocompare.helpers

import com.cryptocompare.helpers.util.PriceFormatConstants
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs

/**
 * Форматирует цену под UI: крупные значения — 2–4 знака после запятой,
 * мелкие — по значащим цифрам, чтобы не схлопнуться в нули. Хвостовые нули
 * обрезаются, поэтому 80145.300000000 -> "80145.3", а 0.0000000034 остаётся
 * читаемым вместо "0.000000000".
 */
fun Double.toPriceString(): String {
    if (isNaN() || isInfinite()) return PriceFormatConstants.NON_FINITE_PLACEHOLDER
    if (this == 0.0) return PriceFormatConstants.ZERO

    val abs = abs(this)
    val rounded =
        when {
            abs >= 1_000 -> BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP)
            abs >= 1 -> BigDecimal.valueOf(this).setScale(4, RoundingMode.HALF_UP)
            // значащие цифры вместо фиксированной точности: ведущий разряд
            // сохраняется при любом порядке малости
            else ->
                BigDecimal.valueOf(this).round(
                    MathContext(PriceFormatConstants.SMALL_SIGNIFICANT_FIGURES, RoundingMode.HALF_UP),
                )
        }

    return rounded.stripTrailingZeros().toPlainString()
}

/**
 * Компактный вариант для узких колонок (список): обычная запись, пока она
 * короткая, иначе научная (например, 0.00000003311 -> "3.31e-08"), чтобы
 * очень мелкие цены не обрезались колонкой в сплошные нули.
 */
fun Double.toCompactPriceString(): String {
    if (isNaN() || isInfinite()) return PriceFormatConstants.NON_FINITE_PLACEHOLDER
    if (this == 0.0) return PriceFormatConstants.ZERO

    val plain = toPriceString()
    if (plain.length <= PriceFormatConstants.COMPACT_MAX_PLAIN_LENGTH) return plain

    return String.format(Locale.US, PriceFormatConstants.COMPACT_SCIENTIFIC_FORMAT, this)
}
