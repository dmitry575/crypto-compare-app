package com.cryptocompare.pairs.util

import kotlin.math.ceil
import kotlin.math.floor

/**
 * Кадр графика: какой участок ряда сейчас на экране.
 *
 * Считается **в свечах, а не в пикселях**, и в «абсолютных» индексах — таких, где
 * ноль это самая свежая серверная свеча на момент открытия графика, история уходит
 * в минус, а дорисованные живым тиком бары — в плюс. Абсолютный индекс свечи не
 * меняется, когда слева дописывается страница истории: список растёт, но у уже
 * загруженных свечей тот же номер. Поэтому догрузка не двигает кадр вообще, и
 * компенсировать сдвиг не нужно — в этом вся разница с готовыми библиотеками,
 * которые меряют скролл в пикселях от начала содержимого.
 *
 * @property leftEdge абсолютный индекс левого края кадра, дробный — скролл плавный.
 * @property visibleCount сколько свечей помещается в кадр; это и есть зум.
 */
internal data class ChartViewport(
    val leftEdge: Float,
    val visibleCount: Float,
) {
    /** Абсолютный индекс правого края (за последней видимой свечой). */
    val rightEdge: Float get() = leftEdge + visibleCount
}

/** Кадр на свежем крае: последняя свеча прижата к правому краю. */
internal fun freshEdgeViewport(
    newestAbs: Int,
    visibleCount: Int = PairsConstants.Chart.VISIBLE_CANDLES,
): ChartViewport =
    ChartViewport(
        leftEdge = newestAbs + 1f - visibleCount,
        visibleCount = visibleCount.toFloat(),
    )

/** Сдвиг кадра на [candles] свечей: положительное — к настоящему, отрицательное — в историю. */
internal fun ChartViewport.scrolledBy(candles: Float): ChartViewport {
    if (!candles.isFinite()) return this
    return copy(leftEdge = leftEdge + candles)
}

/**
 * Зум вокруг точки касания: свеча под пальцем остаётся на месте.
 * [focusFraction] — доля ширины кадра слева от точки (0 — левый край, 1 — правый).
 */
internal fun ChartViewport.zoomedBy(
    factor: Float,
    focusFraction: Float,
): ChartViewport {
    if (!factor.isFinite() || factor <= 0f) return this

    val focusAbs = leftEdge + visibleCount * focusFraction
    val zoomed = visibleCount / factor
    if (!zoomed.isFinite() || zoomed <= 0f) return this

    return ChartViewport(leftEdge = focusAbs - zoomed * focusFraction, visibleCount = zoomed)
}

/**
 * Загоняет кадр в границы загруженного: за края данных не уехать и не отдалиться
 * дальше, чем загружено. Из-за этого пустоты на графике не бывает — вместо неё у
 * края подтягивается следующая страница ([needsOlderPage]).
 */
internal fun ChartViewport.clampedTo(
    oldestAbs: Int,
    newestAbs: Int,
): ChartViewport {
    val loaded = (newestAbs - oldestAbs + 1).coerceAtLeast(1).toFloat()
    val minVisible =
        PairsConstants.Chart.MIN_VISIBLE_CANDLES
            .toFloat()
            .coerceAtMost(loaded)
    val maxVisible =
        PairsConstants.Chart.MAX_VISIBLE_CANDLES
            .toFloat()
            .coerceAtMost(loaded)
    val visible = visibleCount.coerceIn(minVisible, maxVisible)

    // правый край не дальше последней свечи; левый — не левее первой загруженной
    val maxLeft = newestAbs + 1f - visible
    return ChartViewport(leftEdge = leftEdge.coerceIn(oldestAbs.toFloat(), maxLeft), visibleCount = visible)
}

/**
 * Пора ли просить следующую страницу истории: до левого края загруженного осталось
 * меньше [marginScreens] экранов. Запас нужен, чтобы страница успела доехать раньше,
 * чем пользователь упрётся в край.
 */
internal fun ChartViewport.needsOlderPage(
    oldestAbs: Int,
    marginScreens: Float = PairsConstants.Chart.PREFETCH_SCREENS,
): Boolean = leftEdge - oldestAbs < visibleCount * marginScreens

/**
 * Индексы видимых свечей в списке (от старых к новым) с запасом в одну свечу по
 * краям, чтобы половинки на границе кадра не пропадали.
 */
internal fun ChartViewport.visibleIndices(
    size: Int,
    serverCount: Int,
): IntRange {
    if (size <= 0) return IntRange.EMPTY

    val first = (floor(leftEdge).toInt() + serverCount - 1).coerceIn(0, size - 1)
    val last = (ceil(rightEdge).toInt() + serverCount - 1).coerceIn(first, size - 1)
    return first..last
}

/** Прижимает кадр к свежему краю, сохраняя зум: за живым баром надо успевать. */
internal fun ChartViewport.pinnedToFreshEdge(newestAbs: Int): ChartViewport =
    copy(leftEdge = newestAbs + 1f - visibleCount)
