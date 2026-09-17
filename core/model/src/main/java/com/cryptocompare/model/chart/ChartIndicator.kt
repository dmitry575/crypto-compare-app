package com.cryptocompare.model.chart

/**
 * Скользящие средние поверх свечей.
 *
 * Набор закрытый и маленький намеренно: это не конструктор индикаторов, а четыре
 * линии, по которым смотрят тренд. Период у SMA и EMA одинаковый (20 и 50) —
 * различаются они тем, как считают вес свежих свечей.
 */
enum class ChartIndicator(
    val period: Int,
    val exponential: Boolean,
) {
    SMA_20(period = 20, exponential = false),
    SMA_50(period = 50, exponential = false),
    EMA_20(period = 20, exponential = true),
    EMA_50(period = 50, exponential = true),
}
