package com.cryptocompare.helpers.util

/** Правила форматирования цен. */
object PriceFormatConstants {
    /** Значащих цифр для значений меньше единицы. */
    const val SMALL_SIGNIFICANT_FIGURES = 4

    /** Порог, после которого в узких колонках переходим к научной записи. */
    const val COMPACT_MAX_PLAIN_LENGTH = 10
    const val COMPACT_SCIENTIFIC_FORMAT = "%.2e"
    const val NON_FINITE_PLACEHOLDER = "—"
    const val ZERO = "0"
}
