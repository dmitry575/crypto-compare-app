package com.cryptocompare.data.mapper

import com.cryptocompare.model.chart.Candle
import com.cryptocompare.network.dto.apiDTO.klinesDTO.GetKlinesResponse
import com.cryptocompare.network.dto.apiDTO.klinesDTO.KlineEntryDto
import java.time.Instant

fun KlineEntryDto.toDomainOrNull(): Candle? {
    val open = openPrice ?: return null
    val high = highPrice ?: return null
    val low = lowPrice ?: return null
    val close = closePrice ?: return null

    // Периоды без торгов приходят нулями (иногда занулено только high или low).
    // Такие свечи и шкалу растягивают до нуля, и ломают инвариант графика,
    // поэтому отбрасываем целиком: неположительной цены не бывает.
    if (open <= 0.0 || high <= 0.0 || low <= 0.0 || close <= 0.0) return null
    if (!open.isFinite() || !high.isFinite() || !low.isFinite() || !close.isFinite()) return null

    // openTime приходит ISO-8601 в UTC; свечу без разборчивого времени пропускаем,
    // иначе она склеится с соседями по индексу и собьёт подписи оси
    val millis = openTime?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: return null

    // страхуемся от рассогласованных данных: график требует low <= open/close <= high
    return Candle(
        timeMillis = millis,
        open = open,
        high = maxOf(open, close, high, low),
        low = minOf(open, close, high, low),
        close = close,
    )
}

/**
 * Свечи страницы, отсортированные от старых к новым и без дублей по времени.
 * Немаркетные периоды просто отсутствуют — разрывы во времени сохраняются,
 * график рисует свечи подряд по индексу, а не по «настенным» часам.
 */
fun GetKlinesResponse.toCandles(): List<Candle> =
    klines
        .orEmpty()
        .mapNotNull { it.toDomainOrNull() }
        .distinctBy { it.timeMillis }
        .sortedBy { it.timeMillis }
