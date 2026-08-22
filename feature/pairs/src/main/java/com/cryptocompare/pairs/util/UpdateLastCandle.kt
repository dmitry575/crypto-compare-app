package com.cryptocompare.pairs.util

import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartTimeframe

/**
 * Двигает последний бар графика живой ценой: тик либо укладывается в текущую
 * свечу (close, high, low), либо открывает новую, если бар истории уже закрылся.
 * История с API запаздывает, поэтому тик «из прошлого» текущего бара тоже
 * обновляет последнюю свечу, а не создаёт ретроспективную.
 */
internal fun updateLastCandle(
    candles: List<Candle>,
    price: Double,
    timeframe: ChartTimeframe,
    nowMillis: Long,
): List<Candle> {
    if (candles.isEmpty() || !price.isFinite() || price <= 0.0) return candles

    val bucketStart = nowMillis - nowMillis % timeframe.durationMillis()
    val lastCandle = candles.last()

    return if (bucketStart > lastCandle.timeMillis) {
        candles +
            Candle(
                timeMillis = bucketStart,
                open = price,
                high = price,
                low = price,
                close = price,
            )
    } else {
        candles.dropLast(1) +
            lastCandle.copy(
                high = maxOf(lastCandle.high, price),
                low = minOf(lastCandle.low, price),
                close = price,
            )
    }
}
