package com.cryptocompare.helpers

import org.junit.Assert.assertEquals
import org.junit.Test

class ToSubscriptionSetTest {
    @Test
    fun `tickers come to one shape`() {
        assertEquals(setOf("btcusdt", "ethusdt"), listOf("BTCUSDT", "EthUsdt").toSubscriptionSet(LIMIT))
    }

    @Test
    fun `duplicates and blanks do not take a slot`() {
        // слотов мало, и пустая строка занимала бы один из них впустую
        assertEquals(setOf("btcusdt"), listOf("BTCUSDT", "btcusdt", "", "   ").toSubscriptionSet(LIMIT))
    }

    @Test
    fun `everything past the limit is cut off`() {
        val tickers = (1..10).map { "ticker$it" }

        val subscriptions = tickers.toSubscriptionSet(LIMIT)

        assertEquals(LIMIT, subscriptions.size)
        assertEquals(tickers.take(LIMIT), subscriptions.toList())
    }

    @Test
    fun `the first ones stay - the screen decides who matters`() {
        assertEquals(
            listOf("btcusdt", "ethusdt"),
            listOf("BTCUSDT", "ETHUSDT", "ADAUSDT").toSubscriptionSet(2).toList(),
        )
    }

    private companion object {
        const val LIMIT = 8
    }
}
