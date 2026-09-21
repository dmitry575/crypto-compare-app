package com.cryptocompare.helpers

import com.cryptocompare.helpers.util.PriceFormatConstants

/**
 * Прибыль в деньгах: «+345.67», «-12.3», «0».
 *
 * Плюс пишется явно по той же причине, что и у процента изменения: цвет читают
 * не все, а в скриншотах и в скринридере направление пропадает совсем. Минус
 * приходит из самого числа — второй раз его дописывать не нужно.
 *
 * Ноль остаётся без знака: «+0» у позиции, которая стоит ровно столько же,
 * сколько за неё заплатили, выглядел бы как прибыль, которую округлили.
 */
fun Double.toSignedPriceString(): String {
    val price = toPriceString()
    if (price == PriceFormatConstants.NON_FINITE_PLACEHOLDER) return price

    return if (this > 0.0) PriceFormatConstants.PLUS + price else price
}
