package com.cryptocompare.pairs.util

import com.cryptocompare.model.chart.Candle

/**
 * Скользящее окно свечей одной биржи. Кеша нет, всю историю в память не тянем:
 * держим не больше [ChartWindow] в [PairsConstants.Chart.WINDOW_MAX_CANDLES] свечей.
 * Уходишь вглубь истории — свежий край выгружается; возвращаешься — догружается
 * обратно. Свечи отсортированы от старых к новым.
 *
 * Смещения бэкенда считаются в «возрасте» свечи: 0 — самая свежая, больше — старее
 * (`offset` = сколько свежих пропустить). [newestSkip] — возраст самой свежей
 * загруженной серверной свечи (0 = мы на живом крае). [liveCount] — свечи, которые
 * дорисовал живой тик поверх серверных (бывают только на живом крае).
 */
internal data class ChartWindow(
    val candles: List<Candle>,
    val newestSkip: Int,
    val liveCount: Int,
    val oldestReached: Boolean,
) {
    /** Серверных свечей в окне (без дорисованных живым тиком). */
    val serverCount: Int get() = candles.size - liveCount

    /** Смещение для следующей, более старой страницы. */
    val oldestSkip: Int get() = newestSkip + serverCount

    /** Есть ли что грузить глубже в историю. */
    val canLoadOlder: Boolean get() = !oldestReached

    /** Свежий край подрезан — значит, есть что догрузить в сторону настоящего. */
    val canLoadNewer: Boolean get() = newestSkip > 0

    companion object {
        fun initial(
            page: List<Candle>,
            windowMax: Int,
        ): ChartWindow {
            val trimmed = if (page.size > windowMax) page.takeLast(windowMax) else page
            return ChartWindow(
                candles = trimmed,
                newestSkip = 0,
                liveCount = 0,
                // пустая первая страница = у биржи вообще нет истории по паре
                oldestReached = page.isEmpty(),
            )
        }
    }
}

/**
 * Дописывает более старую страницу слева и подрезает свежий край до размера окна.
 * Пустая страница или страница без новых свечей — конец истории у биржи.
 */
internal fun ChartWindow.withOlderPage(
    older: List<Candle>,
    windowMax: Int,
): ChartWindow {
    if (older.isEmpty()) return copy(oldestReached = true)

    val merged = mergeOlderCandles(candles, older)
    if (merged.size == candles.size) return copy(oldestReached = true)

    val over = (merged.size - windowMax).coerceAtLeast(0)
    if (over == 0) return copy(candles = merged)

    // подрезаем самый свежий край: сперва уходят живые свечи, потом серверные
    val trimmed = merged.subList(0, merged.size - over).toList()
    val trimmedLive = minOf(over, liveCount)
    val trimmedServer = over - trimmedLive
    return copy(
        candles = trimmed,
        newestSkip = newestSkip + trimmedServer,
        liveCount = liveCount - trimmedLive,
        oldestReached = false,
    )
}

/**
 * Дописывает более свежую страницу справа (её загружаем, только когда свежий край
 * подрезан) и подрезает старый край до размера окна. Подрезали старый край — значит,
 * глубже снова есть что грузить, поэтому [ChartWindow.oldestReached] сбрасывается.
 */
internal fun ChartWindow.withNewerPage(
    newer: List<Candle>,
    offset: Int,
    windowMax: Int,
): ChartWindow {
    if (newer.isEmpty()) return copy(newestSkip = offset)

    val merged = mergeOlderCandles(candles, newer)
    val over = (merged.size - windowMax).coerceAtLeast(0)
    val trimmed = if (over > 0) merged.subList(over, merged.size).toList() else merged
    return copy(
        candles = trimmed,
        newestSkip = offset,
        oldestReached = if (over > 0) false else oldestReached,
    )
}
