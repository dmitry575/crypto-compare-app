package com.cryptocompare.data.mapper

import com.cryptocompare.data.util.DataConstants.History
import com.cryptocompare.model.chart.Candle
import com.cryptocompare.network.dto.apiDTO.historyDTO.GetHistoryResponse
import com.cryptocompare.network.dto.apiDTO.historyDTO.HistoryCandleDto

fun HistoryCandleDto.toDomainOrNull(index: Int): Candle? {
    val open = open ?: return null
    val high = high ?: return null
    val low = low ?: return null
    val close = close ?: return null
    val time = time

    // Периоды без торгов приходят нулями (иногда занулено только high или low).
    // Такие свечи и шкалу растягивают до нуля, и ломают инвариант графика,
    // поэтому отбрасываем целиком: неположительной цены не бывает.
    if (open <= 0.0 || high <= 0.0 || low <= 0.0 || close <= 0.0) return null
    if (!open.isFinite() || !high.isFinite() || !low.isFinite() || !close.isFinite()) return null

    // время приходит в секундах; если его нет — используем индекс
    val millis =
        when {
            time == null -> index.toLong()
            time < History.MILLIS_THRESHOLD -> time * 1000
            else -> time
        }

    // страхуемся от рассогласованных данных: график требует low <= open/close <= high
    return Candle(
        timeMillis = millis,
        open = open,
        high = maxOf(open, close, high, low),
        low = minOf(open, close, high, low),
        close = close,
    )
}

fun GetHistoryResponse.toCandles(): List<Candle> =
    data
        ?.entries
        .orEmpty()
        .mapIndexedNotNull { index, dto -> dto.toDomainOrNull(index) }

fun GetHistoryResponse.errorMessageOrNull(): String? =
    if (response.equals(History.ERROR_RESPONSE, ignoreCase = true)) {
        message ?: error?.message ?: History.UNKNOWN_ERROR
    } else {
        error?.message
    }
