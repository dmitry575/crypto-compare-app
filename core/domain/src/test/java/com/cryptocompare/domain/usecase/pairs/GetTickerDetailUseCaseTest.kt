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

    @Test
    fun `24h stats reach the exchange as they came`() =
        runTest {
            coEvery { repository.getSymbolsByTicker(TICKER) } returns
                Result.success(
                    listOf(
                        symbol(
                            id = 1,
                            providerId = 10,
                            volume24h = 1_250.5,
                            quoteVolume24h = 98_750_000.0,
                            change24h = -2.35,
                        ),
                    ),
                )
            coEvery { repository.getProviders() } returns Result.success(listOf(provider(10, "Binance")))

            val exchange = useCase(TICKER).getOrThrow().exchanges.single()

            assertEquals(1_250.5, exchange.volume24h!!, 0.0001)
            assertEquals(98_750_000.0, exchange.quoteVolume24h!!, 0.0001)
            assertEquals(-2.35, exchange.change24h!!, 0.0001)
        }

    @Test
    fun `broken 24h numbers are dropped instead of reaching the formatters`() =
        runTest {
            coEvery { repository.getSymbolsByTicker(TICKER) } returns
                Result.success(
                    listOf(
                        symbol(
                            id = 1,
                            providerId = 10,
                            // отрицательного объёма не бывает, а NaN приезжает из кривого JSON
                            volume24h = -1.0,
                            quoteVolume24h = Double.NaN,
                            change24h = Double.POSITIVE_INFINITY,
                        ),
                    ),
                )
            coEvery { repository.getProviders() } returns Result.success(listOf(provider(10, "Binance")))

            val exchange = useCase(TICKER).getOrThrow().exchanges.single()

            assertNull(exchange.volume24h)
            assertNull(exchange.quoteVolume24h)
            assertNull(exchange.change24h)
        }

    private fun symbol(
        id: Long,
        providerId: Int,
        priceSell: Double = 1.0,
        priceBuy: Double = 1.0,
        volume24h: Double? = null,
        quoteVolume24h: Double? = null,
        change24h: Double? = null,
    ) = Symbol(
        id = id,
        ticker = TICKER,
        symbol = TICKER,
        providerId = providerId,
        priceSell = priceSell,
        priceBuy = priceBuy,
        volume24h = volume24h,
        quoteVolume24h = quoteVolume24h,
        change24h = change24h,
    )

    private fun provider(
        id: Int,
        name: String?,
    ) = Provider(id = id, name = name, webSite = null, status = ProviderStatus.Enabled)

    private companion object {
        const val TICKER = "btcusdt"
    }
}
