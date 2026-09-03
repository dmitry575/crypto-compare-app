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

    /** Два знака: цены не доживают до четвёртого, а «53.5235%» это ложная точность. */
    const val PERCENT_FORMAT = "%.2f%%"
    const val ZERO_PERCENT = "0.00%"

    /** Ненулевой разброс мельче двух знаков — иначе он выглядит отсутствующим. */
    const val BELOW_PRECISION_PERCENT = "<0.01%"

    /** Ниже этого значения формат «%.2f» округляет до нуля. */
    const val PERCENT_PRECISION = 0.005
    const val MINUS = "-"

    /** Ниже этого разброс тонет в комиссиях и подсветки не заслуживает. */
    const val NOTABLE_SPREAD_PERCENT = 0.1

    /** Изменение за 24ч со знаком: «+2.35%», «-1.20%». */
    const val SIGNED_PERCENT_FORMAT = "%+.2f%%"

    /**
     * Пороги компактной записи объёма, от крупного к мелкому. Последняя пара —
     * значения меньше тысячи: суффикса нет, число пишется как есть.
     */
    val VOLUME_UNITS: List<Pair<Double, String>> =
        listOf(
            1_000_000_000_000.0 to "T",
            1_000_000_000.0 to "B",
            1_000_000.0 to "M",
            1_000.0 to "K",
            1.0 to "",
        )

    /** Знаков после запятой в мантиссе объёма: «98.75M». */
    const val VOLUME_SCALE = 2

    /** Шаг между суффиксами объёма — на нём мантисса переходит к старшему. */
    const val VOLUME_UNIT_STEP = 1_000.0
}
