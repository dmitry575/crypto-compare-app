package com.cryptocompare.pairs.util

import com.cryptocompare.model.provider.Provider
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.model.provider.ProviderStatus
import com.cryptocompare.model.ticker.TickerPrice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateExchangePricesTest {
    private fun providerDetail(
        id: Int,
        priceSell: Double?,
        priceBuy: Double?,
        quotedAtMillis: Long? = LOADED_AT,
    ) = ProviderDetail(
        provider = Provider(id = id, name = "provider$id", referralUrl = null, status = ProviderStatus.Enabled),
        priceSell = priceSell,
        priceBuy = priceBuy,
        quotedAtMillis = quotedAtMillis,
    )

    private val exchanges =
        listOf(
            providerDetail(id = 1, priceSell = 100.0, priceBuy = 99.0),
            providerDetail(id = 2, priceSell = 101.0, priceBuy = 100.5),
        )

    private fun tick(
        providerId: Int,
        priceSell: Double,
        priceBuy: Double,
    ) = TickerPrice(
        ticker = "btcusdt",
        symbolId = 1,
        providerId = providerId,
        priceSell = priceSell,
        priceBuy = priceBuy,
    )

    @Test
    fun `tick replaces both prices of the matching provider only`() {
        val updated = exchanges.withLivePrices(tick(providerId = 2, priceSell = 105.0, priceBuy = 104.0), RECEIVED_AT)

        assertEquals(exchanges.first(), updated.first())
        assertEquals(105.0, updated.last().priceSell!!, 0.0)
        assertEquals(104.0, updated.last().priceBuy!!, 0.0)
    }

    @Test
    fun `tick from unknown provider leaves everything untouched`() {
        val updated = exchanges.withLivePrices(tick(providerId = 42, priceSell = 105.0, priceBuy = 104.0), RECEIVED_AT)

        assertEquals(exchanges, updated)
    }

    @Test
    fun `a zero side of the tick becomes a dash, not a zero`() {
        val updated = exchanges.withLivePrices(tick(providerId = 2, priceSell = 0.0, priceBuy = 104.0), RECEIVED_AT)

        assertNull(updated.last().priceSell)
        assertEquals(104.0, updated.last().priceBuy!!, 0.0)
    }

    @Test
    fun `non finite prices of the tick are dropped`() {
        val updated =
            exchanges.withLivePrices(
                tick(providerId = 1, priceSell = Double.POSITIVE_INFINITY, priceBuy = Double.NaN),
                RECEIVED_AT,
            )

        assertNull(updated.first().priceSell)
        assertNull(updated.first().priceBuy)
    }

    @Test
    fun `null prices of other providers survive the update`() {
        val withNulls = listOf(providerDetail(id = 3, priceSell = null, priceBuy = null))
        val updated = withNulls.withLivePrices(tick(providerId = 1, priceSell = 105.0, priceBuy = 104.0), RECEIVED_AT)

        assertEquals(withNulls, updated)
    }

    @Test
    fun `a tick refreshes the quote time of its exchange`() {
        // раньше время приходило только из REST при открытии экрана: через пять
        // минут сравнение гасило даже тикавшие биржи, белели они только после
        // перезахода
        val updated = exchanges.withLivePrices(tick(providerId = 2, priceSell = 105.0, priceBuy = 104.0), RECEIVED_AT)

        assertEquals(RECEIVED_AT, updated.last().quotedAtMillis)
    }

    @Test
    fun `other exchanges keep their own quote time`() {
        // биржа без тиков стареет честно — её время тик соседа не трогает
        val updated = exchanges.withLivePrices(tick(providerId = 2, priceSell = 105.0, priceBuy = 104.0), RECEIVED_AT)

        assertEquals(LOADED_AT, updated.first().quotedAtMillis)
    }

    private companion object {
        const val LOADED_AT = 1_700_000_000_000L
        const val RECEIVED_AT = LOADED_AT + 600_000L
    }
}
