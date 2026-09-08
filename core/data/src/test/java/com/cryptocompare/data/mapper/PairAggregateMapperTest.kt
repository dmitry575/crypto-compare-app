package com.cryptocompare.data.mapper

import com.cryptocompare.model.symbol.PairAggregateRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairAggregateMapperTest {
    private fun row(
        buyPrice: Double = 100.0,
        sellPrice: Double = 99.0,
        spreadPercent: Double? = -1.0,
        quoteVolume24h: Double? = 98_750_000.0,
        change24h: Double? = 2.35,
    ) = PairAggregateRow(
        ticker = "BTCUSDT",
        buyPrice = buyPrice,
        sellPrice = sellPrice,
        spreadPercent = spreadPercent,
        quoteVolume24h = quoteVolume24h,
        change24h = change24h,
    )

    @Test
    fun `prices keep their sides`() {
        // перепутанные местами покупка и продажа переворачивают знак спреда,
        // а числа при этом выглядят правдоподобно — поймать можно только тестом
        val item = row(buyPrice = 100.0, sellPrice = 99.0).toPairUiItem()

        assertEquals(100.0, item.buyPrice, 0.0001)
        assertEquals(99.0, item.sellPrice, 0.0001)
    }

    @Test
    fun `spread comes through from the query with its sign`() {
        // считает бэкенд, слой представления его не пересчитывает
        assertEquals(-0.33, row(spreadPercent = -0.33).toPairUiItem().spreadPercent!!, 0.0001)
        assertEquals(0.24, row(spreadPercent = 0.24).toPairUiItem().spreadPercent!!, 0.0001)
    }

    @Test
    fun `missing spread stays null rather than becoming zero`() {
        assertNull(row(spreadPercent = null).toPairUiItem().spreadPercent)
    }

    @Test
    fun `24h stats pass through untouched`() {
        val item = row().toPairUiItem()

        assertEquals(98_750_000.0, item.quoteVolume24h!!, 0.0001)
        assertEquals(2.35, item.change24h!!, 0.0001)
    }

    @Test
    fun `missing 24h stats stay null`() {
        val item = row(quoteVolume24h = null, change24h = null).toPairUiItem()

        assertNull(item.quoteVolume24h)
        assertNull(item.change24h)
    }
}
