package com.cryptocompare.data

import com.cryptocompare.data.mapper.toCandles
import com.cryptocompare.network.dto.apiDTO.historyDTO.GetHistoryResponse
import com.cryptocompare.network.dto.apiDTO.historyDTO.HistoryCandleDto
import com.cryptocompare.network.dto.apiDTO.historyDTO.HistoryDataDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CandleMapperTest {
    private fun response(vararg candles: HistoryCandleDto) =
        GetHistoryResponse(
            response = "Success",
            message = null,
            error = null,
            data = HistoryDataDto(candles.toList()),
        )

    @Test
    fun `valid candle is mapped as is`() {
        val candles =
            response(HistoryCandleDto(1_751_328_000L, open = 10.0, high = 12.0, low = 9.0, close = 11.0))
                .toCandles()

        val candle = candles.single()
        assertEquals(10.0, candle.open, 0.0)
        assertEquals(12.0, candle.high, 0.0)
        assertEquals(9.0, candle.low, 0.0)
        assertEquals(11.0, candle.close, 0.0)
    }

    @Test
    fun `fully empty candle is dropped`() {
        val candles = response(HistoryCandleDto(1L, 0.0, 0.0, 0.0, 0.0)).toCandles()

        assertTrue(candles.isEmpty())
    }

    @Test
    fun `candle with only high zeroed is dropped`() {
        // именно такие свечи роняли график: low оказывался больше high
        val candles =
            response(HistoryCandleDto(1_751_328_000L, open = 0.5, high = 0.0, low = 0.5, close = 0.5))
                .toCandles()

        assertTrue(candles.isEmpty())
    }

    @Test
    fun `negative price is dropped`() {
        val candles =
            response(HistoryCandleDto(1_751_328_000L, open = 1.0, high = 2.0, low = -1.0, close = 1.5))
                .toCandles()

        assertTrue(candles.isEmpty())
    }

    @Test
    fun `inconsistent bounds are normalised so low is never above the rest`() {
        // high/low перепутаны местами — приводим к корректному виду, а не роняем экран
        val candle =
            response(HistoryCandleDto(1_751_328_000L, open = 10.0, high = 9.0, low = 12.0, close = 11.0))
                .toCandles()
                .single()

        assertEquals(12.0, candle.high, 0.0)
        assertEquals(9.0, candle.low, 0.0)
        assertTrue(candle.low <= candle.open)
        assertTrue(candle.low <= candle.close)
        assertTrue(candle.high >= candle.open)
        assertTrue(candle.high >= candle.close)
    }

    @Test
    fun `seconds timestamp is expanded to millis`() {
        val candle =
            response(HistoryCandleDto(1_751_328_000L, 1.0, 2.0, 0.5, 1.5)).toCandles().single()

        assertEquals(1_751_328_000_000L, candle.timeMillis)
    }
}
