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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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

                advanceTimeBy(35_000)
            }

            for (i in 1 until delays.size) {
                assertTrue(
                    "Delay at attempt $i (${delays[i]}ms) should be >= previous (${delays[i - 1]}ms)",
                    delays[i] >= delays[i - 1],
                )
            }
        }

    @Test
    fun `reconnect should stop after max attempts`() =
        testScope.runTest {
            client.connect("wss://api.example.com/ws")

            client.connectionState.test {
                awaitItem()

                val max = 10
                repeat(max + 1) {
                    simulateWebSocketFailure(Throwable("Network error"))
                    advanceUntilIdle()

                    while (true) {
                        val s = awaitItem()

                        if (s is ConnectionState.Error && s.errorMsg.contains("Max attempts")) {
                            cancelAndIgnoreRemainingEvents()
                            return@test
                        }

                        if (s is ConnectionState.Reconnecting) {
                            advanceTimeBy(s.timeDelay)
                            advanceUntilIdle()
                            break
                        }
                    }
                }

                throw AssertionError("Should stop after max attempts, but it didn't")
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

                advanceTimeBy(rec.timeDelay)
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

                advanceTimeBy(rec.timeDelay)
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
