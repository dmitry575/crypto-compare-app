package com.cryptocompare.pairs.util

/**
 * Сколько подписей времени поместится под графиком, не налезая друг на друга.
 *
 * Подписи ставятся внутри кадра на равном шаге: `count` штук делят ширину на
 * `count + 1` промежутков, и шаг должен быть не меньше ширины подписи. Раньше
 * число считалось как `plotWidth / labelWidth`, то есть на одну больше: при
 * обычном шрифте это прятал потолок в [PairsConstants.Chart.TIME_LABEL_COUNT],
 * а при масштабе 1.3 шаг выходил на пару пикселей уже подписи, и даты
 * сливались в «29.07.2610.08.2622.08.26».
 *
 * [labelWidth] — ширина подписи вместе с отступами по бокам.
 */
internal fun timeLabelCount(
    plotWidth: Float,
    labelWidth: Float,
): Int {
    if (labelWidth <= 0f) return PairsConstants.Chart.TIME_LABEL_COUNT

    return ((plotWidth / labelWidth).toInt() - 1).coerceIn(1, PairsConstants.Chart.TIME_LABEL_COUNT)
}
