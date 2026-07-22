package com.cryptocompare.data

import app.cash.turbine.test
import com.cryptocompare.data.repository.TickerStreamRepositoryImpl
import com.cryptocompare.model.ticker.TickerConnectionState
import com.cryptocompare.model.ticker.TickerStreamEvent
import com.cryptocompare.network.dto.webSocketDTO.SocketDtoMessage
import com.cryptocompare.network.dto.webSocketDTO.dataTypes.ErrorData
import com.cryptocompare.network.dto.webSocketDTO.dataTypes.SymbolPriceChangeData
import com.cryptocompare.network.dto.webSocketDTO.dataTypes.TickerData
import com.cryptocompare.network.dto.webSocketDTO.dataTypes.WelcomeData
import com.cryptocompare.network.websocket.ConnectionState
import com.cryptocompare.network.websocket.WebSocketClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TickerStreamRepositoryImplTest {
    @Test
    fun `connectionState maps network states to model states`() =
        runTest {
            val (repository, networkState, _) = createRepository()

            repository.connectionState.test {
                assertEquals(TickerConnectionState.Disconnected, awaitItem())

                networkState.value = ConnectionState.Connecting
                assertEquals(TickerConnectionState.Connecting, awaitItem())

                networkState.value = ConnectionState.Connected
                assertEquals(TickerConnectionState.Connected, awaitItem())

                networkState.value = ConnectionState.Reconnecting(attempts = 2, timeDelay = 1500)
                val rec = awaitItem()
                assertTrue(rec is TickerConnectionState.Reconnecting)
                rec as TickerConnectionState.Reconnecting
                assertEquals(2, rec.attempts)
                assertEquals(1500, rec.timeDelay)

                networkState.value = ConnectionState.Error("boom")
                val err = awaitItem()
                assertTrue(err is TickerConnectionState.Error)
                err as TickerConnectionState.Error
                assertEquals("boom", err.errorMsg)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `events map all dto message types and preserve order`() =
        runTest {
            val (repository, _, networkMessages) = createRepository()

            repository.event.test {
                networkMessages.emit(
                    SocketDtoMessage.Welcome(
                        id = "welcome-id",
                        data = WelcomeData(message = "Connected to Crypto Compare server"),
                    ),
                )
                networkMessages.emit(
                    SocketDtoMessage.Subscribe(
                        id = "subscribe-id",
                        data = TickerData(ticker = "ethusdt"),
                    ),
                )
                networkMessages.emit(
                    SocketDtoMessage.Unsubscribe(
                        id = "unsubscribe-id",
                        data = TickerData(ticker = "btcusdt"),
                    ),
                )
                networkMessages.emit(
                    SocketDtoMessage.SymbolPriceChange(
                        id = "price-id",
                        data =
                            SymbolPriceChangeData(
                                ticker = "ethusdt",
                                symbolId = 2,
                                providerId = 5,
                                priceSell = 3329.82,
                                priceBuy = 3329.81,
                            ),
                    ),
                )
                networkMessages.emit(
                    SocketDtoMessage.Error(
                        id = "error-id",
                        data = ErrorData(error = "Ticker not found", errorCode = 3),
                    ),
                )

                val welcome = awaitItem() as TickerStreamEvent.Welcome
                assertEquals("welcome-id", welcome.id)
                assertEquals("Connected to Crypto Compare server", welcome.message)

                val subscribed = awaitItem() as TickerStreamEvent.Subscribe
                assertEquals("subscribe-id", subscribed.id)
                assertEquals("ethusdt", subscribed.ticker)

                val unsubscribed = awaitItem() as TickerStreamEvent.Unsubscribe
                assertEquals("unsubscribe-id", unsubscribed.id)
                assertEquals("btcusdt", unsubscribed.ticker)

                val priceChanged = awaitItem() as TickerStreamEvent.TickerPriceChange
                assertEquals("price-id", priceChanged.id)
                assertEquals("ethusdt", priceChanged.data.ticker)
                assertEquals(3329.82, priceChanged.data.priceSell, 0.0)
                assertEquals(3329.81, priceChanged.data.priceBuy, 0.0)

                val error = awaitItem() as TickerStreamEvent.Error
                assertEquals("error-id", error.id)
                assertEquals("Ticker not found", error.error)
                assertEquals(3, error.errorCode)
            }
        }

    @Test
    fun `welcome dto maps to welcome domain event`() =
        runTest {
            val (repository, _, networkMessages) = createRepository()

            repository.event.test {
                networkMessages.emit(
                    SocketDtoMessage.Welcome(
                        id = "welcome-only-id",
                        data = WelcomeData(message = "Connected"),
                    ),
                )

                val item = awaitItem() as TickerStreamEvent.Welcome
                assertEquals("welcome-only-id", item.id)
                assertEquals("Connected", item.message)
            }
        }

    @Test
    fun `error dto maps to error domain event with exact payload`() =
        runTest {
            val (repository, _, networkMessages) = createRepository()

            repository.event.test {
                networkMessages.emit(
                    SocketDtoMessage.Error(
                        id = "error-only-id",
                        data = ErrorData(error = "Ticker not found", errorCode = 3),
                    ),
                )

                val item = awaitItem() as TickerStreamEvent.Error
                assertEquals("error-only-id", item.id)
                assertEquals("Ticker not found", item.error)
                assertEquals(3, item.errorCode)
            }
        }

    @Test
    fun `multiple price change dto events map one to one`() =
        runTest {
            val (repository, _, networkMessages) = createRepository()

            repository.event.test {
                networkMessages.emit(
                    SocketDtoMessage.SymbolPriceChange(
                        id = "price-1",
                        data =
                            SymbolPriceChangeData(
                                ticker = "ethusdt",
                                symbolId = 2,
                                providerId = 5,
                                priceSell = 3000.0,
                                priceBuy = 2999.0,
                            ),
                    ),
                )
                networkMessages.emit(
                    SocketDtoMessage.SymbolPriceChange(
                        id = "price-2",
                        data =
                            SymbolPriceChangeData(
                                ticker = "ethusdt",
                                symbolId = 2,
                                providerId = 5,
                                priceSell = 3100.0,
                                priceBuy = 3099.0,
                            ),
                    ),
                )

                val first = awaitItem() as TickerStreamEvent.TickerPriceChange
                assertEquals("price-1", first.id)
                assertEquals(3000.0, first.data.priceSell, 0.0)
                assertEquals(2999.0, first.data.priceBuy, 0.0)

                val second = awaitItem() as TickerStreamEvent.TickerPriceChange
                assertEquals("price-2", second.id)
                assertEquals(3100.0, second.data.priceSell, 0.0)
                assertEquals(3099.0, second.data.priceBuy, 0.0)
            }
        }

    @Test
    fun `connect disconnect subscribe unsubscribe delegate to websocket client`() {
        val (repository, _, _, webSocketClient) = createRepository()

        repository.connect()
        repository.subscribe("ethusdt")
        repository.unsubscribe("ethusdt")
        repository.disconnect()

        verifyOrder {
            webSocketClient.connect("ws://localhost:8081")
            webSocketClient.subscribe("ethusdt")
            webSocketClient.unsubscribe("ethusdt")
            webSocketClient.disconnect()
        }
    }

    private fun createRepository(wsUrl: String = "ws://localhost:8081"): RepositoryFixture {
        val networkState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        val networkMessages = MutableSharedFlow<SocketDtoMessage>()
        val webSocketClient =
            mockk<WebSocketClient>(relaxed = true) {
                every { connectionState } returns networkState
                every { messages } returns networkMessages
            }

        val repository = TickerStreamRepositoryImpl(webSocketClient, wsUrl)
        return RepositoryFixture(repository, networkState, networkMessages, webSocketClient)
    }

    private data class RepositoryFixture(
        val repository: TickerStreamRepositoryImpl,
        val networkState: MutableStateFlow<ConnectionState>,
        val networkMessages: MutableSharedFlow<SocketDtoMessage>,
        val webSocketClient: WebSocketClient,
    )
}
