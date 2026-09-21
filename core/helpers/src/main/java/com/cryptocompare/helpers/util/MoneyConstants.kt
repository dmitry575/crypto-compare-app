package com.cryptocompare.helpers.util

import java.math.MathContext
import java.math.RoundingMode

/**
 * Одна политика денежных расчётов на приложение.
 *
 * Умножение и вычитание идут в `BigDecimal` точно, без округления: округлять
 * там нечего, а `Double` на тех же числах оставляет хвосты вроде
 * `0.010000000000000002` — и прибыль по паре, купленной и стоящей одинаково,
 * получалась не нулём.
 *
 * Округляется только деление — процент прибыли. Значащих цифр берём с запасом:
 * до двух знаков его режет уже формат вывода, а здесь важнее не потерять
 * мелкий процент у крупной позиции.
 */
object MoneyConstants {
    val DIVISION: MathContext = MathContext(16, RoundingMode.HALF_UP)

    /** Доля → проценты. */
    const val PERCENT_MULTIPLIER = 100L
}
