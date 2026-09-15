package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.model.provider.Provider
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.model.provider.ProviderStatus
import com.cryptocompare.model.ticker.TickerBestPrice
import com.cryptocompare.model.ticker.TickerDetail
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparePairAcrossExchangesUseCaseTest {
    private val getTickerDetailUseCase: GetTickerDetailUseCase = mockk()
    private val repository: CryptoCompareRepository = mockk()
    private val useCase = ComparePairAcrossExchangesUseCase(getTickerDetailUseCase, repository)

    @Test
    fun `quotes are sorted from the cheapest ask`() =
        runTest {
            givenExchanges(
                quote(id = 1, name = "bybit", ask = 103.0, bid = 102.9),
                quote(id = 2, name = "okx", ask = 101.0, bid = 100.9),
                quote(id = 3, name = "binance", ask = 102.0, bid = 101.9),
            )
            givenBest()

            val result = useCase(TICKER).getOrThrow()

            assertEquals(listOf("okx", "binance", "bybit"), result.quotes.map { it.provider.name })
        }

    @Test
    fun `an exchange without an ask goes last instead of winning`() =
        runTest {
            // раньше выбор жил в композабле и при отсутствующем аске подставлял
            // бид той же биржи: бид всегда ниже, поэтому такая биржа выигрывала
            // «где купить» и показывала цену, по которой купить нельзя
            givenExchanges(
                quote(id = 1, name = "okx", ask = 101.0, bid = 100.9),
                quote(id = 2, name = "noask", ask = null, bid = 90.0),
            )
            givenBest()

            val result = useCase(TICKER).getOrThrow()

            assertEquals(listOf("okx", "noask"), result.quotes.map { it.provider.name })
        }

    @Test
    fun `an exchange without a bid keeps its place by ask`() =
        runTest {
            givenExchanges(
                quote(id = 1, name = "okx", ask = 101.0, bid = 100.9),
                quote(id = 2, name = "nobid", ask = 100.5, bid = null),
            )
            givenBest()

            val result = useCase(TICKER).getOrThrow()

            assertEquals(listOf("nobid", "okx"), result.quotes.map { it.provider.name })
            assertNull(result.quotes.first().priceBuy)
        }

    @Test
    fun `equal asks are broken by name so the list does not jitter on ticks`() =
        runTest {
            givenExchanges(
                quote(id = 1, name = "zebra", ask = 100.0, bid = 99.0),
                quote(id = 2, name = "alpha", ask = 100.0, bid = 99.0),
            )
            givenBest()

            val result = useCase(TICKER).getOrThrow()

            assertEquals(listOf("alpha", "zebra"), result.quotes.map { it.provider.name })
        }

    @Test
    fun `best exchanges come from the backend, not from the list`() =
        runTest {
            // у биржи 9 самый высокий бид, но котировка протухла, и бэкенд её
            // в лучшую пару не пустил — экран обязан повторить это решение
            givenExchanges(
                quote(id = 1, name = "okx", ask = 101.0, bid = 100.9),
                quote(id = 9, name = "stale", ask = 140.0, bid = 139.0),
            )
            givenBest(bestAskProviderId = 1, bestAskPrice = 101.0, bestBidProviderId = 1, bestBidPrice = 100.9)

            val result = useCase(TICKER).getOrThrow()

            assertEquals(1, result.bestAskProviderId)
            assertEquals(1, result.bestBidProviderId)
        }

    @Test
    fun `the row with the widest spread wins when a pair trades in several networks`() =
        runTest {
            givenExchanges(quote(id = 1, name = "okx", ask = 101.0, bid = 100.9))
            coEvery { repository.getBestPricesByTicker(TICKER) } returns
                Result.success(
                    listOf(
                        best(bestAskProviderId = 1, bestBidProviderId = 1, spreadPercent = -0.1),
                        best(bestAskProviderId = 2, bestBidProviderId = 3, spreadPercent = 0.4),
                    ),
                )

            val result = useCase(TICKER).getOrThrow()

            assertEquals(0.4, result.spreadPercent!!, 0.0001)
            assertEquals(2, result.bestAskProviderId)
            // все строки остаются в сравнении: по ним сокет потом выбирает заново
            assertEquals(2, result.bestPrices.size)
        }

    @Test
    fun `an incomplete row does not win even when it is the widest`() =
        runTest {
            givenExchanges(
                quote(id = 1, name = "okx", ask = 101.0, bid = 100.9),
                quote(id = 2, name = "bybit", ask = 102.0, bid = 101.9),
            )
            coEvery { repository.getBestPricesByTicker(TICKER) } returns
                Result.success(
                    listOf(
                        best(bestAskProviderId = 1, bestBidProviderId = 2, spreadPercent = -0.1),
                        best(bestAskProviderId = null, bestBidProviderId = 2, spreadPercent = 5.0),
                    ),
                )

            val result = useCase(TICKER).getOrThrow()

            // раньше биржа бралась из одной строки, а цена — откуда придётся
            assertEquals(-0.1, result.spreadPercent!!, 0.0001)
            assertEquals(1, result.bestAskProviderId)
            assertEquals(1, result.bestPrices.size)
        }

    @Test
    fun `a non finite best price makes the row incomplete`() =
        runTest {
            givenExchanges(
                quote(id = 1, name = "okx", ask = 101.0, bid = 100.9),
                quote(id = 2, name = "bybit", ask = 102.0, bid = 101.9),
            )
            givenBest(bestAskPrice = Double.POSITIVE_INFINITY)

            val result = useCase(TICKER).getOrThrow()

            assertNull(result.bestAskPrice)
            assertNull(result.bestAskProviderId)
        }

    @Test
    fun `same prices everywhere give a zero spread, not a missing one`() =
        runTest {
            givenExchanges(
                quote(id = 1, name = "okx", ask = 100.0, bid = 100.0),
                quote(id = 2, name = "bybit", ask = 100.0, bid = 100.0),
            )
            coEvery { repository.getBestPricesByTicker(TICKER) } returns
                Result.success(
                    listOf(
                        best(bestAskPrice = 100.0, bestBidPrice = 100.0, spreadPercent = 0.0),
                    ),
                )

            val result = useCase(TICKER).getOrThrow()

            assertEquals(0.0, result.spreadPercent!!, 0.0)
            assertEquals(0.0, result.difference!!, 0.0)
        }

    @Test
    fun `the table survives when best prices are unavailable`() =
        runTest {
            givenExchanges(
                quote(id = 1, name = "okx", ask = 101.0, bid = 100.9),
                quote(id = 2, name = "bybit", ask = 102.0, bid = 101.9),
            )
            coEvery { repository.getBestPricesByTicker(TICKER) } returns
                Result.failure(IllegalStateException("500"))

            val result = useCase(TICKER).getOrThrow()

            assertEquals(2, result.quotes.size)
            assertNull(result.spreadPercent)
            assertNull(result.bestAskProviderId)
            assertNull(result.difference)
        }

    @Test
    fun `zero prices from the backend are treated as missing`() =
        runTest {
            givenExchanges(
                quote(id = 1, name = "okx", ask = 101.0, bid = 100.9),
                quote(id = 2, name = "bybit", ask = 102.0, bid = 101.9),
            )
            givenBest(bestAskPrice = 0.0, bestBidPrice = 0.0)

            val result = useCase(TICKER).getOrThrow()

            assertNull(result.bestAskPrice)
            assertNull(result.bestBidPrice)
        }

    @Test
    fun `a single exchange fills the summary itself when the backend stays silent`() =
        runTest {
            // по таким парам best-выдача иногда не отвечает вовсе, а ответ
            // очевиден: выбирать не из чего, её ask и есть лучший
            givenExchanges(quote(id = 7, name = "blofin", ask = 0.03156, bid = 0.03154))
            coEvery { repository.getBestPricesByTicker(TICKER) } returns Result.success(emptyList())

            val result = useCase(TICKER).getOrThrow()

            assertEquals(7, result.bestAskProviderId)
            assertEquals(7, result.bestBidProviderId)
            assertEquals(0.03156, result.bestAskPrice!!, 1e-9)
            assertTrue(result.spreadPercent!! < 0)
        }

    @Test
    fun `a single exchange without an ask gets no buy highlight`() =
        runTest {
            givenExchanges(quote(id = 7, name = "blofin", ask = null, bid = 0.03154))
            coEvery { repository.getBestPricesByTicker(TICKER) } returns Result.success(emptyList())

            val result = useCase(TICKER).getOrThrow()

            assertNull(result.bestAskProviderId)
            assertEquals(7, result.bestBidProviderId)
        }

    @Test
    fun `several exchanges without a backend answer keep the summary empty`() =
        runTest {
            // здесь подставлять нечего: выбор между биржами — работа бэкенда,
            // он же отсеивает протухшие котировки
            givenExchanges(
                quote(id = 1, name = "okx", ask = 101.0, bid = 100.9),
                quote(id = 2, name = "bybit", ask = 102.0, bid = 101.9),
            )
            coEvery { repository.getBestPricesByTicker(TICKER) } returns Result.success(emptyList())

            val result = useCase(TICKER).getOrThrow()

            assertNull(result.bestAskProviderId)
            assertNull(result.spreadPercent)
        }

    @Test
    fun `a single exchange is reported as such`() =
        runTest {
            givenExchanges(quote(id = 1, name = "okx", ask = 101.0, bid = 100.9))
            givenBest()

            assertTrue(useCase(TICKER).getOrThrow().isSingleExchange)
        }

    @Test
    fun `several exchanges are not a single one`() =
        runTest {
            givenExchanges(
                quote(id = 1, name = "okx", ask = 101.0, bid = 100.9),
                quote(id = 2, name = "bybit", ask = 102.0, bid = 101.9),
            )
            givenBest()

            assertFalse(useCase(TICKER).getOrThrow().isSingleExchange)
        }

    @Test
    fun `difference is the gap between the two best sides`() =
        runTest {
            givenExchanges(quote(id = 1, name = "okx", ask = 100.0, bid = 99.0))
            givenBest(bestAskPrice = 100.0, bestBidPrice = 101.5)

            assertEquals(1.5, useCase(TICKER).getOrThrow().difference!!, 0.0001)
        }

    @Test
    fun `empty exchange list gives an empty comparison`() =
        runTest {
            givenExchanges()
            givenBest()

            val result = useCase(TICKER).getOrThrow()

            assertTrue(result.quotes.isEmpty())
            assertFalse(result.isSingleExchange)
        }

    @Test
    fun `a symbol id compares one network set and takes its own best pair, not the widest`() =
        runTest {
            coEvery { getTickerDetailUseCase(TICKER, 14487) } returns
                Result.success(
                    TickerDetail(
                        ticker = TICKER,
                        exchanges = listOf(quote(id = 17, name = "okx", ask = 2487.32, bid = 2486.93)),
                        symbolId = 14487,
                        networks = listOf("Ethereum", "Base"),
                    ),
                )
            coEvery { repository.getBestPricesByTicker(TICKER) } returns
                Result.success(
                    listOf(
                        best(bestAskProviderId = 18, bestBidProviderId = 3, spreadPercent = 0.16).copy(symbolId = 143),
                        best(
                            bestAskProviderId = 17,
                            bestBidProviderId = 17,
                            spreadPercent = -0.02,
                        ).copy(symbolId = 14487),
                    ),
                )

            val result = useCase(TICKER, symbolId = 14487).getOrThrow()

            // самая широкая пара тикера — у символа 143, но это другие сети
            assertEquals(17, result.bestAskProviderId)
            assertEquals(-0.02, result.spreadPercent!!, 1e-9)
            assertEquals(listOf(14487L), result.bestPrices.map { it.symbolId })
            assertEquals(14487L, result.symbolId)
            assertEquals(listOf("Ethereum", "Base"), result.networks)
        }

    @Test
    fun `a failing detail request fails the whole comparison`() =
        runTest {
            coEvery { getTickerDetailUseCase(TICKER) } returns Result.failure(IllegalStateException("no network"))

            assertTrue(useCase(TICKER).isFailure)
        }

    private fun givenExchanges(vararg quotes: ProviderDetail) {
        coEvery { getTickerDetailUseCase(TICKER) } returns
            Result.success(TickerDetail(ticker = TICKER, exchanges = quotes.toList()))
    }

    private fun givenBest(
        bestAskProviderId: Int? = 1,
        bestAskPrice: Double = 101.0,
        bestBidProviderId: Int? = 1,
        bestBidPrice: Double = 100.9,
    ) {
        coEvery { repository.getBestPricesByTicker(TICKER) } returns
            Result.success(
                listOf(
                    best(
                        bestAskProviderId = bestAskProviderId,
                        bestAskPrice = bestAskPrice,
                        bestBidProviderId = bestBidProviderId,
                        bestBidPrice = bestBidPrice,
                    ),
                ),
            )
    }

    private fun best(
        bestAskProviderId: Int? = 1,
        bestAskPrice: Double = 101.0,
        bestBidProviderId: Int? = 1,
        bestBidPrice: Double = 100.9,
        spreadPercent: Double? = -0.1,
    ) = TickerBestPrice(
        ticker = TICKER,
        symbolId = 1L,
        bestAskProviderId = bestAskProviderId,
        bestAskPrice = bestAskPrice,
        bestBidProviderId = bestBidProviderId,
        bestBidPrice = bestBidPrice,
        spreadPercent = spreadPercent,
    )

    private fun quote(
        id: Int,
        name: String,
        ask: Double?,
        bid: Double?,
    ) = ProviderDetail(
        provider =
            Provider(
                id = id,
                name = name,
                referralUrl = null,
                status = ProviderStatus.Enabled,
            ),
        priceSell = ask,
        priceBuy = bid,
    )

    private companion object {
        const val TICKER = "btcusdt"
    }
}
