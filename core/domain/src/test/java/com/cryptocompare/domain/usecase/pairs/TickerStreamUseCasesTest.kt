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
    fun `StreamPauseUseCase pauses instead of disconnecting`() {
        StreamPauseUseCase(repository)()

        verify(exactly = 1) { repository.pause() }
        // disconnect сбросил бы намерение держать соединение, и resume ничего бы не вернул
        verify(exactly = 0) { repository.disconnect() }
    }

    @Test
    fun `StreamResumeUseCase resumes the connection`() {
        StreamResumeUseCase(repository)()

        verify(exactly = 1) { repository.resume() }
    }

    @Test
    fun `ObserveStreamReconnectsUseCase forwards reconnects`() =
        runTest {
            every { repository.reconnects } returns flowOf(Unit, Unit)

            ObserveStreamReconnectsUseCase(repository)().test {
                awaitItem()
                awaitItem()
                awaitComplete()
            }
        }

    @Test
    fun `StreamConnectUseCase opens the connection`() {
        StreamConnectUseCase(repository)()

        verify(exactly = 1) { repository.connect() }
        verify(exactly = 0) { repository.unsubscribe(any()) }
    }

    @Test
    fun `TakeOverTickerSubscriptionsUseCase begins a takeover with the screen's tickers`() {
        // дифф подписок и сохранение базы каталога теперь на репозитории — здесь
        // проверяем только делегацию
        TakeOverTickerSubscriptionsUseCase(repository)(setOf("ADAUSDT"))

        verify(exactly = 1) { repository.beginTickerTakeover(setOf("adausdt")) }
    }

    @Test
    fun `RestoreTickerSubscriptionsUseCase ends the takeover`() {
        RestoreTickerSubscriptionsUseCase(repository)()

        verify(exactly = 1) { repository.endTickerTakeover() }
    }
}
