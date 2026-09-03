package com.cryptocompare.helpers

import com.cryptocompare.helpers.util.PriceFormatConstants
import java.util.Locale
import kotlin.math.abs

/**
 * Изменение цены за 24 часа: «+2.35%», «-1.20%», «0.00%».
 *
 * Знак пишется явно, а не только цветом: цвет читают не все, а в скриншотах и
 * в скринридере направление пропадает совсем. У нуля знака нет — «%+.2f» дал бы
 * «+0.00%» для 0.0001 и «-0.00%» для -0.0001, то есть разное для одного и того же
 * стоящего рынка.
 */
fun Double.toSignedPercentString(): String {
    if (isNaN() || isInfinite()) return PriceFormatConstants.NON_FINITE_PLACEHOLDER
    if (abs(this) < PriceFormatConstants.PERCENT_PRECISION) return PriceFormatConstants.ZERO_PERCENT

    return String.format(Locale.US, PriceFormatConstants.SIGNED_PERCENT_FORMAT, this)
}

/**
 * Направление изменения для выбора цвета: 1 — рост, -1 — падение, 0 — стоит.
 *
 * Порог тот же, что у [toSignedPercentString]: иначе «0.00%» могло бы
 * оказаться зелёным, а подпись и цвет разошлись бы.
 */
fun Double.priceChangeSign(): Int =
    when {
        !isFinite() -> 0
        this >= PriceFormatConstants.PERCENT_PRECISION -> 1
        this <= -PriceFormatConstants.PERCENT_PRECISION -> -1
        else -> 0
    }
