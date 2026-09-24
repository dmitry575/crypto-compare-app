package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.domain.repository.TickerStreamRepository
import com.cryptocompare.helpers.util.WebSocketConstants
import com.cryptocompare.model.ticker.TickerBestPrice
import com.cryptocompare.model.ticker.TickerPrice
import com.cryptocompare.model.ticker.TickerStreamEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncLiveBestPricesUseCaseTest {
    private val events = MutableSharedFlow<TickerStreamEvent>(extraBufferCapacity = 16)
    private val stream: TickerStreamRepository = mockk { every { event } returns events }
    private val repository: CryptoCompareRepository = mockk()
    private val useCase = SyncLiveBestPricesUseCase(stream, repository)

    @Test
    fun `ticks are written in one batch per interval, the latest per symbol`() =
        runTest {
            coEvery { repository.applyBestPriceUpdates(any()) } returns Result.success(Unit)
            startWriter()

            events.emit(best(symbolId = 1, bid = 100.0))
            events.emit(best(symbolId = 1, bid = 110.0))
            events.emit(best(symbolId = 2, bid = 10.0))
            runCurrent()
            coVerify(exactly = 0) { repository.applyBestPriceUpdates(any()) }

            advanceTimeBy(WebSocketConstants.PRICE_FLUSH_INTERVAL_MS + 1)

            coVerify(exactly = 1) {
                repository.applyBestPriceUpdates(
                    match { batch ->
                        batch.map { it.symbolId to it.bestBidPrice }.toSet() ==
                            setOf(1L to 110.0, 2L to 10.0)
                    },
                )
            }
        }

    @Test
    fun `a quote of one exchange never reaches the catalog`() =
        runTest {
            // у события типа 4 symbolId общий на биржи тикера: запиши его — и каталог
            // показывал бы цены последней тикнувшей площадки вместо разницы между биржами
            startWriter()

            events.emit(TickerStreamEvent.TickerPriceChange("evt", TickerPrice("btcusdt", 1, 5, 101.0, 100.0)))
            advanceTimeBy(WebSocketConstants.PRICE_FLUSH_INTERVAL_MS * 3)

            coVerify(exactly = 0) { repository.applyBestPriceUpdates(any()) }
        }

    @Test
    fun `nothing is written while no ticks come`() =
        runTest {
            startWriter()

            advanceTimeBy(WebSocketConstants.PRICE_FLUSH_INTERVAL_MS * 10)

            coVerify(exactly = 0) { repository.applyBestPriceUpdates(any()) }
        }

    @Test
    fun `a failed write does not stop the next batches`() =
        runTest {
            coEvery { repository.applyBestPriceUpdates(any()) } returnsMany
                listOf(Result.failure(IllegalStateException("db locked")), Result.success(Unit))
            startWriter()

            events.emit(best(symbolId = 1, bid = 100.0))
            advanceTimeBy(WebSocketConstants.PRICE_FLUSH_INTERVAL_MS + 1)
            events.emit(best(symbolId = 1, bid = 101.0))
            advanceTimeBy(WebSocketConstants.PRICE_FLUSH_INTERVAL_MS * 2 + 1)

            coVerify(exactly = 2) { repository.applyBestPriceUpdates(any()) }
        }

    @Test
    fun `a tick after a quiet interval starts a new batch`() =
        runTest {
            // таймер выходит после пустого интервала; следующий тик не должен повиснуть
            coEvery { repository.applyBestPriceUpdates(any()) } returns Result.success(Unit)
            startWriter()

            events.emit(best(symbolId = 1, bid = 100.0))
            advanceTimeBy(WebSocketConstants.PRICE_FLUSH_INTERVAL_MS * 4)
            events.emit(best(symbolId = 2, bid = 10.0))
            advanceTimeBy(WebSocketConstants.PRICE_FLUSH_INTERVAL_MS + 1)

            coVerify(exactly = 2) { repository.applyBestPriceUpdates(any()) }
        }

    /** Писатель не возвращается сам: запускаем его в фоне теста, runTest отменит его в конце. */
    private fun TestScope.startWriter() {
        backgroundScope.launch { useCase() }
        runCurrent()
    }

    private fun best(
        symbolId: Long,
        bid: Double,
    ) = TickerStreamEvent.TickerBestPriceChange(
        id = "evt",
        data =
            TickerBestPrice(
                ticker = "btcusdt",
                symbolId = symbolId,
                bestAskProviderId = 18,
                bestAskPrice = bid + 1,
                bestBidProviderId = 3,
                bestBidPrice = bid,
                spreadPercent = -1.0,
            ),
    )
}
