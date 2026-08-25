package com.cryptocompare.pairs.util

import com.cryptocompare.model.chart.Candle

/**
 * Дописывает страницу более старых свечей к уже загруженным. Дубли по времени
 * убираем — из-за немаркетных разрывов и живого последнего бара границы страниц
 * могут пересекаться, — а уже имеющаяся свеча побеждает: в ней мог осесть живой
 * тик. Итог отсортирован от старых к новым, разрывы во времени сохраняются.
 */
internal fun mergeOlderCandles(
    existing: List<Candle>,
    older: List<Candle>,
): List<Candle> {
    if (older.isEmpty()) return existing

    val byTime = LinkedHashMap<Long, Candle>(existing.size + older.size)
    older.forEach { byTime[it.timeMillis] = it }
    existing.forEach { byTime[it.timeMillis] = it }

    return byTime.values.sortedBy { it.timeMillis }
}
