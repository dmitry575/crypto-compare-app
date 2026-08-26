package com.cryptocompare.pairs.util

import com.cryptocompare.model.chart.Candle

/**
 * Загруженная история графика одной пары (биржа + масштаб). Растёт только влево:
 * страницы дописываются в начало, свежий край не выгружается. Кеша на диске нет,
 * поэтому глубина ограничена [PairsConstants.Chart.MAX_CANDLES].
 *
 * Смещения бэкенда считаются в «возрасте» свечи: 0 — самая свежая серверная,
 * дальше в прошлое. [liveCount] — бары, которые дорисовал живой тик поверх
 * серверных; они всегда в хвосте и в offset не участвуют.
 *
 * @property candles от старых к новым.
 */
internal data class ChartHistory(
    val candles: List<Candle>,
    val liveCount: Int = 0,
    val oldestReached: Boolean = false,
) {
    /** Серверных свечей (без дорисованных живым тиком). */
    val serverCount: Int get() = candles.size - liveCount

    /** Offset следующей, более старой страницы: возраст самой старой загруженной + 1. */
    val oldestSkip: Int get() = serverCount

    /** Абсолютный индекс самой старой загруженной свечи (см. [ChartViewport]). */
    val oldestAbs: Int get() = 1 - serverCount

    /** Абсолютный индекс самой свежей свечи. */
    val newestAbs: Int get() = liveCount

    /** Есть ли смысл просить ещё страницу. */
    val canLoadOlder: Boolean
        get() = !oldestReached && candles.size < PairsConstants.Chart.MAX_CANDLES

    companion object {
        fun initial(page: List<Candle>): ChartHistory =
            ChartHistory(
                candles = page,
                liveCount = 0,
                // пустая первая страница = у биржи вообще нет истории по паре
                oldestReached = page.isEmpty(),
            )
    }
}

/**
 * Дописывает более старую страницу слева. Страница без новых свечей — конец
 * истории у биржи: дальше просить бессмысленно.
 */
internal fun ChartHistory.withOlderPage(page: List<Candle>): ChartHistory {
    if (page.isEmpty()) return copy(oldestReached = true)

    val merged = mergeOlderCandles(candles, page)
    if (merged.size == candles.size) return copy(oldestReached = true)

    return copy(candles = merged)
}

/**
 * Принимает ряд, пересчитанный живым тиком. Тик либо двигает последний бар, либо
 * открывает новый — тогда это ещё одна «живая» свеча поверх серверных.
 */
internal fun ChartHistory.withLiveCandles(updated: List<Candle>): ChartHistory =
    copy(candles = updated, liveCount = liveCount + (updated.size - candles.size))
