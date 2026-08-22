package com.cryptocompare.domain.usecase.pairs

import app.cash.turbine.test
import com.cryptocompare.domain.repository.TickerStreamRepository
import com.cryptocompare.model.ticker.TickerPrice
import com.cryptocompare.model.ticker.TickerStreamEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Use case'ы потока котировок — тонкие обёртки над [TickerStreamRepository]. */
class TickerStreamUseCasesTest {
    private val repository: TickerStreamRepository = mockk(relaxed = true)

    @Test
    fun `ObserveTickerEventUseCase forwards service messages and price ticks alike`() =
        runTest {
            val welcome = TickerStreamEvent.Welcome(id = "1", message = "Connected")
            val tick =
                TickerStreamEvent.TickerPriceChange(
                    id = "2",
                    data =
                        TickerPrice(
                            ticker = "btcusdt",
                            symbolId = 1,
                            providerId = 1,
                            priceSell = 100.0,
                            priceBuy = 99.0,
                        ),
                )
            every { repository.event } returns flowOf(welcome, tick)

            ObserveTickerEventUseCase(repository)().test {
                assertEquals(welcome, awaitItem())
                assertEquals(tick, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `ObserveTickerEventUseCase forwards errors instead of swallowing them`() =
        runTest {
            val error = TickerStreamEvent.Error(id = "3", errorCode = 2, error = "Subscribe failed")
            every { repository.event } returns flowOf(error)

            ObserveTickerEventUseCase(repository)().test {
                assertEquals(error, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `StreamDisconnectUseCase closes the socket`() {
        StreamDisconnectUseCase(repository)()

        verify(exactly = 1) { repository.disconnect() }
        // отключение не должно попутно дёргать подписки
        verify(exactly = 0) { repository.unsubscribe(any()) }
    }

    @Test
    fun `StreamConnectUseCase opens the connection`() {
        StreamConnectUseCase(repository)()

        verify(exactly = 1) { repository.connect() }
        verify(exactly = 0) { repository.unsubscribe(any()) }
    }

    @Test
    fun `SubscribeSingleTickerUseCase leaves only the requested ticker subscribed`() {
        every { repository.activeSubscriptions } returns setOf("btcusdt", "ethusdt")

        val previous = SubscribeSingleTickerUseCase(repository)("adausdt")

        assertEquals(setOf("btcusdt", "ethusdt"), previous)
        verify(exactly = 1) { repository.unsubscribe("btcusdt") }
        verify(exactly = 1) { repository.unsubscribe("ethusdt") }
        verify(exactly = 1) { repository.subscribe("adausdt") }
    }

    @Test
    fun `SubscribeSingleTickerUseCase does not resubscribe an already active ticker`() {
        every { repository.activeSubscriptions } returns setOf("btcusdt")

        SubscribeSingleTickerUseCase(repository)("BTCUSDT")

        verify(exactly = 0) { repository.subscribe(any()) }
        verify(exactly = 0) { repository.unsubscribe(any()) }
    }

    @Test
    fun `RestoreTickerSubscriptionsUseCase diffs against active subscriptions`() {
        every { repository.activeSubscriptions } returns setOf("btcusdt", "ethusdt")

        RestoreTickerSubscriptionsUseCase(repository)(setOf("ETHUSDT", "adausdt"))

        verify(exactly = 1) { repository.unsubscribe("btcusdt") }
        verify(exactly = 1) { repository.subscribe("adausdt") }
        // ethusdt остался активным — трогать его не надо
        verify(exactly = 0) { repository.subscribe("ethusdt") }
        verify(exactly = 0) { repository.unsubscribe("ethusdt") }
    }

    @Test
    fun `RestoreTickerSubscriptionsUseCase skips blank tickers`() {
        every { repository.activeSubscriptions } returns emptySet()

        RestoreTickerSubscriptionsUseCase(repository)(setOf("", "  ", "btcusdt"))

        verify(exactly = 1) { repository.subscribe("btcusdt") }
        verify(exactly = 1) { repository.subscribe(any()) }
    }
}
