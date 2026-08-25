package com.cryptocompare.data

import com.cryptocompare.data.mapper.toCandles
import com.cryptocompare.network.dto.apiDTO.klinesDTO.GetKlinesResponse
import com.cryptocompare.network.dto.apiDTO.klinesDTO.KlineEntryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class KlineMapperTest {
    private fun entry(
        openTime: String?,
        open: Double? = 10.0,
        high: Double? = 12.0,
        low: Double? = 9.0,
        close: Double? = 11.0,
    ) = KlineEntryDto(
        openTime = openTime,
        openPrice = open,
        highPrice = high,
        lowPrice = low,
        closePrice = close,
        volume = 1.0,
    )

    private fun response(vararg entries: KlineEntryDto) =
        GetKlinesResponse(
            errorCode = 0,
            errorMsgs = null,
            providerId = 1,
            providerName = "mexc",
            ticker = "btcusdt",
            interval = "1d",
            klines = entries.toList(),
        )

    @Test
    fun `valid candle is mapped and iso time becomes epoch millis`() {
        val candle = response(entry("2026-07-01T00:00:00Z")).toCandles().single()

        assertEquals(10.0, candle.open, 0.0)
        assertEquals(12.0, candle.high, 0.0)
        assertEquals(9.0, candle.low, 0.0)
        assertEquals(11.0, candle.close, 0.0)
        assertEquals(Instant.parse("2026-07-01T00:00:00Z").toEpochMilli(), candle.timeMillis)
    }

    @Test
    fun `fully empty candle is dropped`() {
        assertTrue(response(entry("2026-07-01T00:00:00Z", 0.0, 0.0, 0.0, 0.0)).toCandles().isEmpty())
    }

    @Test
    fun `candle with only high zeroed is dropped`() {
        // именно такие свечи роняли график: low оказывался больше high
        assertTrue(
            response(
                entry("2026-07-01T00:00:00Z", open = 0.5, high = 0.0, low = 0.5, close = 0.5),
            ).toCandles().isEmpty(),
        )
    }

    @Test
    fun `negative price is dropped`() {
        assertTrue(
            response(
                entry("2026-07-01T00:00:00Z", open = 1.0, high = 2.0, low = -1.0, close = 1.5),
            ).toCandles().isEmpty(),
        )
    }

    @Test
    fun `candle without a parseable time is dropped`() {
        // без времени свеча склеилась бы с соседями по индексу и сбила бы ось
        assertTrue(response(entry(null)).toCandles().isEmpty())
        assertTrue(response(entry("not-a-date")).toCandles().isEmpty())
    }

    @Test
    fun `inconsistent bounds are normalised so low is never above the rest`() {
        // high/low перепутаны местами — приводим к корректному виду, а не роняем экран
        val candle =
            response(
                entry("2026-07-01T00:00:00Z", open = 10.0, high = 9.0, low = 12.0, close = 11.0),
            ).toCandles().single()

        assertEquals(12.0, candle.high, 0.0)
        assertEquals(9.0, candle.low, 0.0)
        assertTrue(candle.low <= candle.open)
        assertTrue(candle.high >= candle.close)
    }

    @Test
    fun `candles are deduplicated by time and sorted from oldest to newest`() {
        val candles =
            response(
                entry("2026-07-03T00:00:00Z", close = 3.0),
                entry("2026-07-01T00:00:00Z", close = 1.0),
                entry("2026-07-03T00:00:00Z", close = 99.0),
            ).toCandles()

        assertEquals(2, candles.size)
        assertEquals(1.0, candles.first().close, 0.0)
        assertTrue(candles[0].timeMillis < candles[1].timeMillis)
    }
}
