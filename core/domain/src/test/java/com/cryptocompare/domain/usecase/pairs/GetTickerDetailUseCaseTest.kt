package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.model.provider.Provider
import com.cryptocompare.model.provider.ProviderStatus
import com.cryptocompare.model.symbol.Symbol
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetTickerDetailUseCaseTest {
    private val repository: CryptoCompareRepository = mockk()
    private val useCase = GetTickerDetailUseCase(repository)

    @Test
    fun `joins symbols with providers and sorts exchanges by name`() =
        runTest {
            coEvery { repository.getSymbolsByTicker(TICKER) } returns
                Result.success(
                    listOf(
                        symbol(id = 1, providerId = 10, priceSell = 101.0, priceBuy = 100.0),
                        symbol(id = 2, providerId = 20, priceSell = 99.0, priceBuy = 98.0),
                    ),
                )
            coEvery { repository.getProviders() } returns
                Result.success(listOf(provider(10, "Zonda"), provider(20, "Binance")))

            val detail = useCase(TICKER).getOrThrow()

            assertEquals(TICKER, detail.ticker)
            assertEquals(listOf("Binance", "Zonda"), detail.exchanges.map { it.provider.name })
            assertEquals(99.0, detail.exchanges.first().priceSell!!, 0.0)
        }

    @Test
    fun `sorting by name ignores case`() =
        runTest {
            coEvery { repository.getSymbolsByTicker(TICKER) } returns
                Result.success(
                    listOf(
                        symbol(id = 1, providerId = 10),
                        symbol(id = 2, providerId = 20),
                        symbol(id = 3, providerId = 30),
                    ),
                )
            coEvery { repository.getProviders() } returns
                Result.success(
                    listOf(provider(10, "beta"), provider(20, "Alpha"), provider(30, "gamma")),
                )

            val detail = useCase(TICKER).getOrThrow()

            assertEquals(listOf("Alpha", "beta", "gamma"), detail.exchanges.map { it.provider.name })
        }

    @Test
    fun `symbols without a matching provider are dropped`() =
        runTest {
            coEvery { repository.getSymbolsByTicker(TICKER) } returns
                Result.success(
                    listOf(
                        symbol(id = 1, providerId = 10),
                        symbol(id = 2, providerId = 999),
                    ),
                )
            coEvery { repository.getProviders() } returns Result.success(listOf(provider(10, "Binance")))

            val detail = useCase(TICKER).getOrThrow()

            assertEquals(1, detail.exchanges.size)
            assertEquals(
                "Binance",
                detail.exchanges
                    .single()
                    .provider.name,
            )
        }

    @Test
    fun `non positive prices become null instead of being shown as zero`() =
        runTest {
            coEvery { repository.getSymbolsByTicker(TICKER) } returns
                Result.success(listOf(symbol(id = 1, providerId = 10, priceSell = 0.0, priceBuy = -1.0)))
            coEvery { repository.getProviders() } returns Result.success(listOf(provider(10, "Binance")))

            val exchange = useCase(TICKER).getOrThrow().exchanges.single()

            assertNull(exchange.priceSell)
            assertNull(exchange.priceBuy)
        }

    @Test
    fun `failed symbols request produces a failure`() =
        runTest {
            coEvery { repository.getSymbolsByTicker(TICKER) } returns Result.failure(IllegalStateException("offline"))
            coEvery { repository.getProviders() } returns Result.success(emptyList())

            val result = useCase(TICKER)

            assertTrue(result.isFailure)
            assertEquals("offline", result.exceptionOrNull()?.message)
        }

    @Test
    fun `failed providers request produces a failure`() =
        runTest {
            coEvery { repository.getSymbolsByTicker(TICKER) } returns Result.success(listOf(symbol(1, 10)))
            coEvery { repository.getProviders() } returns Result.failure(IllegalStateException("offline"))

            assertTrue(useCase(TICKER).isFailure)
        }

    @Test
    fun `ticker present nowhere yields an empty exchange list rather than a failure`() =
        runTest {
            coEvery { repository.getSymbolsByTicker(TICKER) } returns Result.success(emptyList())
            coEvery { repository.getProviders() } returns Result.success(listOf(provider(10, "Binance")))

            val detail = useCase(TICKER).getOrThrow()

            assertTrue(detail.exchanges.isEmpty())
        }

    private fun symbol(
        id: Long,
        providerId: Int,
        priceSell: Double = 1.0,
        priceBuy: Double = 1.0,
    ) = Symbol(
        id = id,
        ticker = TICKER,
        symbol = TICKER,
        providerId = providerId,
        priceSell = priceSell,
        priceBuy = priceBuy,
    )

    private fun provider(
        id: Int,
        name: String?,
    ) = Provider(id = id, name = name, webSite = null, status = ProviderStatus.Enabled)

    private companion object {
        const val TICKER = "btcusdt"
    }
}
