package com.cryptocompare.pairs.util

import com.cryptocompare.model.chart.Candle
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

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
 * Диапазон оси цен под видимый кадр графика. Считать его по всему ряду нельзя:
 * одна свеча из глубины истории (памп на 10x) сплющивает текущие в тонкую ленту.
 * Но и кадр нельзя брать «примерно» — если окно оси отстанет от свечей, они
 * уедут за границы и график окажется пустым, поэтому [visibleXRange] приходит от
 * самого vico.
 *
 * Границы округляются до кратных шагу: ось пересчитывается на каждом кадре
 * скролла, и без округления подписи дрожали бы непрерывно.
 */
internal fun List<Candle>.chartPriceRange(
    visibleXRange: ClosedFloatingPointRange<Double>?,
    labelCount: Int = PairsConstants.Chart.PRICE_LABEL_COUNT,
): ChartPriceRange {
    val steps = (labelCount - 1).coerceAtLeast(1)
    if (isEmpty()) return emptyRange(steps)

    val visible = visibleSlice(visibleXRange)
    val low = visible.minOf { it.low }
    val high = visible.maxOf { it.high }
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

/**
 * Свечи видимого кадра. `x` свечи — её индекс в ряду, но vico отдаёт диапазон с
 * запасом за края данных, поэтому индексы подрезаем. Пока первой отрисовки не
 * было, диапазон пустой — берём хвост ряда: график открывается на свежем крае.
 */
private fun List<Candle>.visibleSlice(visibleXRange: ClosedFloatingPointRange<Double>?): List<Candle> {
    if (visibleXRange == null || visibleXRange.isEmpty()) return takeLast(PairsConstants.Chart.VISIBLE_CANDLES)

    val first = floor(visibleXRange.start).toInt().coerceIn(0, lastIndex)
    val last = ceil(visibleXRange.endInclusive).toInt().coerceIn(first, lastIndex)
    return subList(first, last + 1)
}

/** Ближайший «круглый» шаг не меньше [rawStep]: 1, 2 или 5 на своём порядке. */
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
