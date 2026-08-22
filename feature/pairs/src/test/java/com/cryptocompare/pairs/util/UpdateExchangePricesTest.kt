package com.cryptocompare.pairs.util

import com.cryptocompare.model.provider.Provider
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.model.provider.ProviderStatus
import com.cryptocompare.model.ticker.TickerPrice
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateExchangePricesTest {
    private fun providerDetail(
        id: Int,
        priceSell: Double?,
        priceBuy: Double?,
    ) = ProviderDetail(
        provider = Provider(id = id, name = "provider$id", webSite = null, status = ProviderStatus.Enabled),
        priceSell = priceSell,
        priceBuy = priceBuy,
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
        val updated = exchanges.withLivePrices(tick(providerId = 2, priceSell = 105.0, priceBuy = 104.0))

        assertEquals(exchanges.first(), updated.first())
        assertEquals(105.0, updated.last().priceSell!!, 0.0)
        assertEquals(104.0, updated.last().priceBuy!!, 0.0)
    }

    @Test
    fun `tick from unknown provider leaves everything untouched`() {
        val updated = exchanges.withLivePrices(tick(providerId = 42, priceSell = 105.0, priceBuy = 104.0))

        assertEquals(exchanges, updated)
    }

    @Test
    fun `null prices of other providers survive the update`() {
        val withNulls = listOf(providerDetail(id = 3, priceSell = null, priceBuy = null))
        val updated = withNulls.withLivePrices(tick(providerId = 1, priceSell = 105.0, priceBuy = 104.0))

        assertEquals(withNulls, updated)
    }
}
