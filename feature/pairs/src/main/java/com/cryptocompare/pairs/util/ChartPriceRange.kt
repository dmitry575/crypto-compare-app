package com.cryptocompare.pairs.util

import com.cryptocompare.model.chart.Candle
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Окно оси цен: границы [min]..[max] и шаг между подписями. Границы кратны шагу,
 * поэтому подписи всегда «круглые».
 */
internal data class ChartPriceRange(
    val min: Double,
    val max: Double,
    val step: Double,
)

/**
 * Диапазон оси цен по свечам видимого кадра (получатель — уже нарезанный кадр).
 * Считать по всему загруженному ряду нельзя: одна свеча из глубины истории (памп
 * на 10x) сплющивает текущие в тонкую ленту.
 *
 * Границы округляются до кратных шагу: ось пересчитывается на каждом кадре
 * скролла, и без округления подписи дрожали бы непрерывно.
 */
internal fun List<Candle>.chartPriceRange(labelCount: Int = PairsConstants.Chart.PRICE_LABEL_COUNT): ChartPriceRange {
    val steps = (labelCount - 1).coerceAtLeast(1)
    if (isEmpty()) return emptyRange(steps)

    val low = minOf { it.low }
    val high = maxOf { it.high }
    if (!low.isFinite() || !high.isFinite() || high < low) return emptyRange(steps)

    val span =
        when {
            high > low -> high - low
            high != 0.0 -> abs(high) * PairsConstants.Chart.FLAT_SPAN_RATIO
            else -> return emptyRange(steps)
        }
    val padding = span * PairsConstants.Chart.RANGE_PADDING
    val paddedLow = low - padding
    val paddedHigh = high + padding
    val step = niceStep((paddedHigh - paddedLow) / steps) ?: return emptyRange(steps)

    return ChartPriceRange(
        min = floor(paddedLow / step) * step,
        max = ceil(paddedHigh / step) * step,
        step = step,
    )
}

/** Ближайший «круглый» шаг не меньше [rawStep]: 1, 2, 2.5 или 5 на своём порядке. */
private fun niceStep(rawStep: Double): Double? {
    if (!rawStep.isFinite() || rawStep <= 0.0) return null

    val magnitude = 10.0.pow(floor(log10(rawStep)))
    if (!magnitude.isFinite() || magnitude <= 0.0) return null

    val normalized = rawStep / magnitude
    val multiplier =
        PairsConstants.Chart.NICE_STEP_MULTIPLIERS.firstOrNull { normalized <= it }
            ?: PairsConstants.Chart.NICE_STEP_MULTIPLIERS.last()
    return multiplier * magnitude
}

/** Данных нет или они бессмысленны — рисуем нейтральную шкалу вместо пустоты. */
private fun emptyRange(steps: Int) = ChartPriceRange(min = 0.0, max = 1.0, step = 1.0 / steps)

/**
 * Цены подписей оси: от [ChartPriceRange.min] до [ChartPriceRange.max] с шагом.
 * Границы кратны шагу, поэтому подписи получаются круглыми.
 */
internal fun ChartPriceRange.priceLabels(): List<Double> {
    if (!step.isFinite() || step <= 0.0) return listOf(min, max)

    val count =
        ((max - min) / step)
            .roundToInt()
            .coerceIn(1, PairsConstants.Chart.MAX_PRICE_LABELS)
    return (0..count).map { min + it * step }
}
