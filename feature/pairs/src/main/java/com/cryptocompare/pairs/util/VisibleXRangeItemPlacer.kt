package com.cryptocompare.pairs.util

import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import kotlin.math.abs

/**
 * Размещатель подписей оси X, который заодно отдаёт наружу видимый диапазон.
 *
 * Ось Y строится по видимому кадру, а границы кадра знает только vico: скролл он
 * меряет в пикселях от начала содержимого, по границам слоя и с учётом падингов
 * и зума. Считать это в композиции по `scrollState` — гадание, и ошибка растёт
 * вместе с отъездом кадра; [HorizontalAxis.ItemPlacer.getLabelValues] получает
 * готовый `visibleXRange` на каждой отрисовке.
 *
 * Вызывается из фазы отрисовки, поэтому [onVisibleXRange] дёргается только на
 * заметный сдвиг кадра — см. [differsNoticeablyFrom].
 */
internal class VisibleXRangeItemPlacer(
    private val delegate: HorizontalAxis.ItemPlacer,
    private val onVisibleXRange: (ClosedFloatingPointRange<Double>) -> Unit,
) : HorizontalAxis.ItemPlacer by delegate {
    private var reported: ClosedFloatingPointRange<Double>? = null

    override fun getLabelValues(
        context: CartesianDrawingContext,
        visibleXRange: ClosedFloatingPointRange<Double>,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ): List<Double> {
        if (reported.differsNoticeablyFrom(visibleXRange)) {
            reported = visibleXRange
            onVisibleXRange(visibleXRange)
        }
        return delegate.getLabelValues(context, visibleXRange, fullXRange, maxLabelWidth)
    }
}

/**
 * Сдвинулся ли кадр настолько, чтобы пересчитывать по нему ось Y.
 *
 * Порог нужен из-за петли: ширина кадра — это ширина графика минус ось цен, а
 * ширину оси задают её подписи, которые считаются по кадру. Смена подписи на один
 * знак двигает кадр на доли процента его ширины; без порога пара таких состояний
 * может зациклиться и гонять перерисовку вхолостую. Порог задан долей кадра,
 * поэтому работает одинаково на любом зуме.
 */
internal fun ClosedFloatingPointRange<Double>?.differsNoticeablyFrom(next: ClosedFloatingPointRange<Double>): Boolean {
    if (this == null) return true

    val threshold = (next.endInclusive - next.start) * PairsConstants.Chart.FRAME_UPDATE_RATIO
    if (threshold <= 0.0) return true

    return abs(next.start - start) > threshold || abs(next.endInclusive - endInclusive) > threshold
}
