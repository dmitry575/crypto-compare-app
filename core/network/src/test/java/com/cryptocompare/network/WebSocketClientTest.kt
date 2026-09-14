package com.cryptocompare.network

import app.cash.turbine.test
import com.cryptocompare.helpers.util.WebSocketConstants
import com.cryptocompare.network.dto.webSocketDTO.SocketDtoMessage
import com.cryptocompare.network.websocket.ConnectionState
import com.cryptocompare.network.websocket.MessageType
import com.cryptocompare.network.websocket.WebSocketClient
import com.google.gson.Gson
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketClientTest {
    // Mocks
    private lateinit var mockOkHttpClient: OkHttpClient
    private lateinit var mockWebSocket: WebSocket
    private lateinit var gson: Gson
    private lateinit var client: WebSocketClient

    // Test dispatcher
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // Captured listener
    private val listenerSlot = slot<WebSocketListener>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        gson = Gson()
        mockOkHttpClient = mockk(relaxed = true)
        mockWebSocket = mockk(relaxed = true)

        every {
            mockOkHttpClient.newWebSocket(any(), capture(listenerSlot))
        } returns mockWebSocket

        client = WebSocketClient(gson, mockOkHttpClient, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `connect with valid wss URL should succeed`() =
        testScope.runTest {
            val url = "wss://api.example.com/ws"

            client.connect(url)

            verify { mockOkHttpClient.newWebSocket(any(), any()) }

            client.connectionState.test {
                assertEquals(ConnectionState.Connecting, awaitItem())
            }
        }

    @Test
    fun `connect with valid ws URL should succeed`() =
        testScope.runTest {
            val url = "ws://localhost:8080/ws"

            client.connect(url)

            verify { mockOkHttpClient.newWebSocket(any(), any()) }
        }

    @Test(expected = IllegalArgumentException::class)
    fun `connect with invalid URL should throw exception`() {
        val url = "https://api.example.com/ws"

        client.connect(url)
    }

    @Test
    fun `connect when already connected should be ignored`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()

            client.connect("wss://api.example.com/ws")

            verify(exactly = 1) { mockOkHttpClient.newWebSocket(any(), any()) }
        }

    @Test
    fun `onOpen should update state to Connected`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")

            client.connectionState.test {
                assertEquals(ConnectionState.Connecting, awaitItem())

                simulateWebSocketOpen()

                assertEquals(ConnectionState.Connected, awaitItem())
            }
        }

    @Test
    fun `disconnect should close WebSocket`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()

            client.disconnect()

            verify {
                mockWebSocket.close(
                    WebSocketConstants.NORMAL_CLOSURE_STATUS,
                    "Client disconnected",
                )
            }

            client.connectionState.test {
                assertEquals(ConnectionState.Disconnected, awaitItem())
            }
        }

    @Test
    fun `subscribe when connected should send subscribe message`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()
            every { mockWebSocket.send(any<String>()) } returns true

            client.subscribe("BTCUSDT")

            verify(exactly = 1) {
                mockWebSocket.send(
                    match<String> { msg ->
                        msg.contains("\"type\":${MessageType.SUBSCRIBE.type}") &&
                            msg.contains("\"ticker\":\"btcusdt\"")
                    },
                )
            }
        }

    @Test
    fun `subscribe same ticker twice should send only one message`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()
            every { mockWebSocket.send(any<String>()) } returns true

            client.subscribe("BTCUSDT")
            client.subscribe("BTCUSDT")

            verify(exactly = 1) { mockWebSocket.send(any<String>()) }
        }

    @Test
    fun `subscribe should normalize ticker to lowercase`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()
            every { mockWebSocket.send(any<String>()) } returns true

            client.subscribe("BTCUSDT")

            verify { mockWebSocket.send(match<String> { it.contains("btcusdt") }) }
        }

    @Test
    fun `unsubscribe existing ticker should send unsubscribe message`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()
            every { mockWebSocket.send(any<String>()) } returns true
            client.subscribe("BTCUSDT")
            clearMocks(mockWebSocket, answers = false)

            client.unsubscribe("BTCUSDT")

            verify(exactly = 1) {
                mockWebSocket.send(
                    match<String> { msg ->
                        msg.contains("\"type\":${MessageType.UNSUBSCRIBE.type}") &&
                            msg.contains("\"ticker\":\"btcusdt\"")
                    },
                )
            }
        }

    @Test
    fun `unsubscribe non-existing ticker should not send message`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()

            client.unsubscribe("BTCUSDT")

            verify(exactly = 0) { mockWebSocket.send(any<String>()) }
        }

    @Test
    fun `onFailure should trigger reconnect`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")

            client.connectionState.test {
                skipItems(1)

                simulateWebSocketFailure(Throwable("Network error"))

                val errorState = awaitItem()
                assertTrue(errorState is ConnectionState.Error)
                assertEquals("Network error", (errorState as ConnectionState.Error).errorMsg)

                val reconnectingState = awaitItem()
                assertTrue(reconnectingState is ConnectionState.Reconnecting)
                assertEquals(0, (reconnectingState as ConnectionState.Reconnecting).attempts)
                assertTrue(reconnectingState.timeDelay > 0)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `reconnect should use exponential backoff`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")

            val delays = mutableListOf<Long>()

            repeat(5) {
                simulateWebSocketFailure(Throwable("Network error"))
                advanceUntilIdle()

                val currentState = client.connectionState.value
                if (currentState is ConnectionState.Reconnecting) {
                    delays.add(currentState.timeDelay)
                }

                advanceTimeBy(35_000.milliseconds)
            }

            for (i in 1 until delays.size) {
                assertTrue(
                    "Delay at attempt $i (${delays[i]}ms) should be >= previous (${delays[i - 1]}ms)",
                    delays[i] >= delays[i - 1],
                )
            }
        }

    @Test
    fun `reconnect should keep retrying past the old attempt limit`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")

            // Раньше клиент сдавался после 10 попыток; жёсткого предела больше нет —
            // прогоняем заведомо больше и убеждаемся, что он продолжает пытаться
            // восстановиться, а не переходит в терминальное «Max attempts».
            repeat(15) { attempt ->
                simulateWebSocketFailure(Throwable("Network error"))

                // onFailure синхронно ставит Error, затем reconnect() — Reconnecting;
                // если бы предел вернули, на 11-й попытке тут был бы Error, а не Reconnecting
                val state = client.connectionState.value
                assertTrue(
                    "attempt $attempt: должен продолжать реконнект, а не сдаваться (был $state)",
                    state is ConnectionState.Reconnecting,
                )

                // проматываем бэкофф, чтобы сработал следующий connect()
                advanceTimeBy(((state as ConnectionState.Reconnecting).timeDelay + 1_000).milliseconds)
                advanceUntilIdle()
            }
        }

    @Test
    fun `manual disconnect should prevent auto-reconnect`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()

            client.disconnect()
            simulateWebSocketClosed(1000, "Normal closure")

            advanceUntilIdle()

            client.connectionState.test {
                assertEquals(ConnectionState.Disconnected, awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `onClosed without manual disconnect should trigger reconnect`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")

            client.connectionState.test {
                while (awaitItem() != ConnectionState.Connecting) { // skip
                }

                simulateWebSocketOpen()
                advanceUntilIdle()

                while (awaitItem() != ConnectionState.Connected) { // skip
                }

                simulateWebSocketClosed(1001, "Going away")
                advanceUntilIdle()

                assertEquals(ConnectionState.Disconnected, awaitItem())
                assertTrue("Should attempt to reconnect", awaitItem() is ConnectionState.Reconnecting)

                cancelAndIgnoreRemainingEvents()
            }

            simulateWebSocketOpen()
            advanceUntilIdle()
        }

    @Test
    fun `reconnect should restore subscriptions`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()

            every { mockWebSocket.send(any<String>()) } returns true

            client.subscribe("BTCUSDT")
            client.subscribe("ETHUSDT")
            clearMocks(mockWebSocket, answers = false)

            client.connectionState.test {
                simulateWebSocketClosed(1006, "Abnormal closure")
                advanceUntilIdle()

                while (awaitItem() != ConnectionState.Disconnected) {
                }

                val rec = awaitItem()
                require(rec is ConnectionState.Reconnecting)

                advanceTimeBy(rec.timeDelay.milliseconds)
                advanceUntilIdle()

                simulateWebSocketOpen()
                advanceUntilIdle()

                verify(exactly = 2) {
                    mockWebSocket.send(
                        match<String> { msg ->
                            msg.contains("\"type\":${MessageType.SUBSCRIBE.type}")
                        },
                    )
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `restored subscriptions should be normalized to lowercase`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()

            every { mockWebSocket.send(any<String>()) } returns true

            client.subscribe("BTCUSDT")
            clearMocks(mockWebSocket, answers = false)

            client.connectionState.test {
                simulateWebSocketClosed(1006, "Abnormal closure")
                advanceUntilIdle()

                while (awaitItem() != ConnectionState.Disconnected) {
                }

                val rec = awaitItem()
                require(rec is ConnectionState.Reconnecting)

                advanceTimeBy(rec.timeDelay.milliseconds)
                advanceUntilIdle()

                simulateWebSocketOpen()
                advanceUntilIdle()

                verify {
                    mockWebSocket.send(
                        match<String> { msg ->
                            msg.contains("\"type\":${MessageType.SUBSCRIBE.type}") &&
                                msg.contains("\"ticker\":\"btcusdt\"")
                        },
                    )
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onMessage with PRICE_CHANGE should emit SymbolPriceChange`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()

            val jsonMessage =
                """
                {
                    "id": "123",
                    "type": ${MessageType.PRICE_CHANGE.type},
                    "data": {
                        "ticker": "BTCUSDT",
                        "symbolId": 1,
                        "providerId": 2,
                        "priceBuy": 50000.0,
                        "priceSell": 50000.0
                    }
                }
                """.trimIndent()

            client.messages.test {
                simulateWebSocketMessage(jsonMessage)
                advanceUntilIdle()

                val message = awaitItem()
                assertTrue(message is SocketDtoMessage.SymbolPriceChange)
                assertEquals("BTCUSDT", (message as SocketDtoMessage.SymbolPriceChange).data.ticker)
                assertTrue(50000.0 == message.data.priceBuy)
            }
        }

    @Test
    fun `onMessage with binary data should be parsed`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()

            val jsonMessage = """
            {"id": "123", "type": ${MessageType.WELCOME.type}, "data": {"message": "Connected to Crypto Compare server"}}
        """
            val bytes = jsonMessage.encodeUtf8()

            client.messages.test {
                simulateWebSocketMessage(bytes)
                advanceUntilIdle()

                val message = awaitItem()
                assertTrue(message is SocketDtoMessage.Welcome)
            }
        }

    @Test
    fun `onMessage with invalid JSON should be ignored`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()

            client.messages.test {
                simulateWebSocketMessage("invalid json {{{")
                advanceUntilIdle()

                expectNoEvents()
            }
        }

    @Test
    fun `connectionState should transition correctly`() =
        testScope.runTest {
            client.connectionState.test {
                assertEquals(ConnectionState.Disconnected, awaitItem())

                client.connect("wss://api.example.com/ws")
                assertEquals(ConnectionState.Connecting, awaitItem())

                simulateWebSocketOpen()
                assertEquals(ConnectionState.Connected, awaitItem())

                client.disconnect()
                assertEquals(ConnectionState.Disconnected, awaitItem())
            }
        }

    @Test
    fun `close should cancel scope`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()

            client.close()

            verify { mockWebSocket.close(any(), any()) }
        }

    // ---------- аудит #43: брошенные сокеты, бэкофф, фон ----------

    /** Каждый connect() получает свой сокет и свой листенер — иначе брошенный не отличить от живого. */
    private fun distinctSockets(): Pair<MutableList<WebSocket>, MutableList<WebSocketListener>> {
        val sockets = mutableListOf<WebSocket>()
        val listeners = mutableListOf<WebSocketListener>()
        every { mockOkHttpClient.newWebSocket(any(), any()) } answers {
            listeners += secondArg<WebSocketListener>()
            mockk<WebSocket>(relaxed = true).also { sockets += it }
        }
        return sockets to listeners
    }

    @Test
    fun `a closed socket reporting late does not touch the new connection`() =
        testScope.runTest {
            val (sockets, listeners) = distinctSockets()
            client.connect("wss://api.example.com/ws")
            listeners[0].onOpen(sockets[0], mockk(relaxed = true))

            // уход в фон и возврат быстрее, чем сервер ответил на close
            client.disconnect()
            client.connect("wss://api.example.com/ws")
            listeners[1].onOpen(sockets[1], mockk(relaxed = true))

            listeners[0].onClosed(sockets[0], 1000, "Client disconnected")
            advanceUntilIdle()

            // раньше: Disconnected поверх живого соединения, ссылка на новый сокет
            // обнулялась, и реконнект открывал третий, пока второй висел без хозяина
            assertEquals(ConnectionState.Connected, client.connectionState.value)
            verify(exactly = 2) { mockOkHttpClient.newWebSocket(any(), any()) }
            verify(exactly = 0) { sockets[1].cancel() }
        }

    @Test
    fun `a failure of an abandoned socket does not start a reconnect`() =
        testScope.runTest {
            val (sockets, listeners) = distinctSockets()
            client.connect("wss://api.example.com/ws")
            client.disconnect()
            client.connect("wss://api.example.com/ws")

            listeners[0].onFailure(sockets[0], Throwable("Canceled"), null)
            advanceUntilIdle()

            assertEquals(ConnectionState.Connecting, client.connectionState.value)
            verify(exactly = 2) { mockOkHttpClient.newWebSocket(any(), any()) }
        }

    @Test
    fun `messages from an abandoned socket are dropped`() =
        testScope.runTest {
            val (sockets, listeners) = distinctSockets()
            client.connect("wss://api.example.com/ws")
            listeners[0].onOpen(sockets[0], mockk(relaxed = true))
            client.disconnect()
            client.connect("wss://api.example.com/ws")
            listeners[1].onOpen(sockets[1], mockk(relaxed = true))

            val welcome = """{"id": "1", "type": ${MessageType.WELCOME.type}, "data": {"message": "hi"}}"""
            client.messages.test {
                // иначе каждая цена приходила бы дважды — от старого сокета и от нового
                listeners[0].onMessage(sockets[0], welcome)
                expectNoEvents()

                listeners[1].onMessage(sockets[1], welcome)
                assertTrue(awaitItem() is SocketDtoMessage.Welcome)
            }
        }

    @Test
    fun `an abandoned socket that opens late is closed and not counted`() =
        testScope.runTest {
            val (sockets, listeners) = distinctSockets()
            client.connect("wss://api.example.com/ws")
            client.disconnect()

            listeners[0].onOpen(sockets[0], mockk(relaxed = true))

            verify(exactly = 1) { sockets[0].cancel() }
            assertEquals(ConnectionState.Disconnected, client.connectionState.value)
            assertEquals(0, client.openedConnections.value)
        }

    @Test
    fun `every open is counted, including reconnects`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()
            simulateWebSocketClosed(1006, "Abnormal closure")
            advanceTimeBy(35_000.milliseconds)
            simulateWebSocketOpen()

            assertEquals(2, client.openedConnections.value)
        }

    @Test
    fun `a connection dropped right after opening keeps backing off`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")

            // сервер принимает соединение и тут же рвёт его: раньше onOpen обнулял
            // счётчик, и переподключение шло раз в секунду без конца
            val attempts =
                (0 until 4).map {
                    simulateWebSocketOpen()
                    simulateWebSocketClosed(1011, "Server error")
                    val state = client.connectionState.value as ConnectionState.Reconnecting
                    advanceTimeBy((state.timeDelay + 1).milliseconds)
                    state.attempts
                }

            assertEquals(listOf(0, 1, 2, 3), attempts)
        }

    @Test
    fun `a connection that held long enough starts the backoff over`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketFailure(Throwable("Network error"))
            advanceTimeBy(5_000.milliseconds)
            simulateWebSocketFailure(Throwable("Network error"))
            advanceTimeBy(5_000.milliseconds)

            simulateWebSocketOpen()
            advanceTimeBy((WebSocketConstants.STABLE_CONNECTION_MS + 1).milliseconds)
            simulateWebSocketClosed(1006, "Abnormal closure")

            assertEquals(0, (client.connectionState.value as ConnectionState.Reconnecting).attempts)
        }

    @Test
    fun `pause closes the socket and resume reopens it with the same subscriptions`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()
            every { mockWebSocket.send(any<String>()) } returns true
            client.subscribe("BTCUSDT")

            client.pause()
            advanceUntilIdle()

            assertEquals(ConnectionState.Disconnected, client.connectionState.value)
            verify { mockWebSocket.close(WebSocketConstants.NORMAL_CLOSURE_STATUS, any()) }
            // в фоне не переподключаемся сами
            verify(exactly = 1) { mockOkHttpClient.newWebSocket(any(), any()) }

            clearMocks(mockWebSocket, answers = false)
            client.resume()
            simulateWebSocketOpen()

            verify(exactly = 2) { mockOkHttpClient.newWebSocket(any(), any()) }
            verify(exactly = 1) { mockWebSocket.send(match<String> { it.contains("btcusdt") }) }
        }

    @Test
    fun `a screen asking to connect while in background waits for resume`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()
            client.pause()

            client.connect("wss://api.example.com/ws")

            verify(exactly = 1) { mockOkHttpClient.newWebSocket(any(), any()) }

            client.resume()

            verify(exactly = 2) { mockOkHttpClient.newWebSocket(any(), any()) }
        }

    @Test
    fun `resume does not reopen a connection that was closed on purpose`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()
            client.disconnect()

            client.pause()
            client.resume()

            verify(exactly = 1) { mockOkHttpClient.newWebSocket(any(), any()) }
            assertEquals(ConnectionState.Disconnected, client.connectionState.value)
        }

    @Test
    fun `leaving the catalog while in background keeps the connection closed`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")
            simulateWebSocketOpen()
            client.pause()

            // MainViewModel.onCleared приходит, пока приложение свёрнуто
            client.disconnect()
            client.resume()

            verify(exactly = 1) { mockOkHttpClient.newWebSocket(any(), any()) }
        }

    @Test
    fun `pause without an open connection does nothing`() =
        testScope.runTest {
            client.pause()
            client.resume()

            verify(exactly = 0) { mockOkHttpClient.newWebSocket(any(), any()) }
            assertFalse(client.connectionState.value is ConnectionState.Reconnecting)
        }

    private fun simulateWebSocketOpen() {
        val mockResponse = mockk<Response>(relaxed = true)
        listenerSlot.captured.onOpen(mockWebSocket, mockResponse)
    }

    private fun simulateWebSocketMessage(text: String) {
        listenerSlot.captured.onMessage(mockWebSocket, text)
    }

    private fun simulateWebSocketMessage(bytes: ByteString) {
        listenerSlot.captured.onMessage(mockWebSocket, bytes)
    }

    private fun simulateWebSocketFailure(t: Throwable) {
        listenerSlot.captured.onFailure(mockWebSocket, t, null)
    }

    private fun simulateWebSocketClosed(
        code: Int,
        reason: String,
    ) {
        listenerSlot.captured.onClosed(mockWebSocket, code, reason)
    }
}
