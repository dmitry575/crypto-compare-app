package com.cryptocompare.network.websocket

import android.util.Log
import com.cryptocompare.helpers.util.WebSocketConstants
import com.cryptocompare.network.dto.webSocketDTO.SocketDtoMessage
import com.cryptocompare.network.dto.webSocketDTO.SocketInboundRawMessage
import com.cryptocompare.network.dto.webSocketDTO.SocketOutboundMessage
import com.cryptocompare.network.dto.webSocketDTO.dataTypes.ErrorData
import com.cryptocompare.network.dto.webSocketDTO.dataTypes.SymbolPriceChangeData
import com.cryptocompare.network.dto.webSocketDTO.dataTypes.TickerData
import com.cryptocompare.network.dto.webSocketDTO.dataTypes.WelcomeData
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class WebSocketClient
    @Inject
    constructor(
        private val gson: Gson,
        private val okHttpClient: OkHttpClient,
        @Named("ioDispatcher") private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + dispatcher)

        private val subscribedTickers = mutableSetOf<String>()
        private val lock = Any()

        // Эти поля читаются и пишутся из потоков OkHttp-колбэков (onOpen/onClosed/
        // onFailure) и из korutin реконнекта — @Volatile гарантирует их видимость
        // между потоками. Без него disconnect() мог выставить isManuallyDisconnect,
        // а onClosed на потоке OkHttp прочитать устаревшее false и переподключиться.
        @Volatile private var reconnectJob: Job? = null

        @Volatile private var isManuallyDisconnect = false

        @Volatile private var reconnectAttempts = 0

        @Volatile private var webSocket: WebSocket? = null

        @Volatile private var currentUrl: String? = null

        private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        val connectionState = _connectionState.asStateFlow()

        private val _messages =
            MutableSharedFlow<SocketDtoMessage>(
                extraBufferCapacity = 256,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        val messages = _messages.asSharedFlow()

        fun connect(url: String) {
            require(url.startsWith("ws://") || url.startsWith("wss://")) {
                "WebSocket URL must start with ws:// or wss://"
            }

            currentUrl = url
            if (_connectionState.value == ConnectionState.Connecting ||
                _connectionState.value == ConnectionState.Connected
            ) {
                Log.d(TAG, "connect($url) skipped, state=${_connectionState.value}")
                return
            }

            Log.i(TAG, "connecting to $url")
            _connectionState.value = ConnectionState.Connecting
            isManuallyDisconnect = false

            webSocket?.cancel()
            val request = Request.Builder().url(url).build()
            webSocket = okHttpClient.newWebSocket(request, webSocketListener())
        }

        fun disconnect() {
            Log.i(TAG, "disconnect requested")
            isManuallyDisconnect = true
            reconnectJob?.cancel()
            reconnectAttempts = 0
            _connectionState.value = ConnectionState.Disconnected
            webSocket?.close(WebSocketConstants.NORMAL_CLOSURE_STATUS, "Client disconnected")
            webSocket = null
        }

        fun reconnect() {
            if (isManuallyDisconnect || reconnectJob?.isActive == true) {
                return
            }

            val url = currentUrl ?: return

            // Жёсткого предела попыток нет намеренно: после разрыва сеть может
            // лежать сколько угодно, и поток котировок должен восстановиться, когда
            // она вернётся. Частоту держит экспоненциальный бэкофф с потолком
            // MAX_RECONNECT_DELAY_MS (30с) — офлайн это всего один пинг раз в 30с.
            val delayMs = reconnectDelayMs(reconnectAttempts)
            Log.w(TAG, "reconnect #${reconnectAttempts + 1} in ${delayMs}ms")
            _connectionState.value =
                ConnectionState.Reconnecting(
                    attempts = reconnectAttempts,
                    timeDelay = delayMs,
                )

            reconnectAttempts++
            reconnectJob =
                scope.launch {
                    delay(delayMs.milliseconds)
                    connect(url)
                }
        }

        fun subscribe(ticker: String) {
            val tickerLower = ticker.lowercase()

            val wasAdded =
                synchronized(lock) {
                    subscribedTickers.add(tickerLower)
                }

            if (wasAdded && _connectionState.value == ConnectionState.Connected) {
                sendMessage(MessageType.SUBSCRIBE, tickerLower)
            } else if (wasAdded) {
                // подписка не потеряется: restoreSubscriptions() дошлёт её после onOpen
                Log.d(TAG, "subscribe($tickerLower) deferred, state=${_connectionState.value}")
            }
        }

        fun unsubscribe(ticker: String) {
            val tickerLower = ticker.lowercase()

            val wasRemoved =
                synchronized(lock) {
                    subscribedTickers.remove(tickerLower)
                }

            if (wasRemoved && _connectionState.value == ConnectionState.Connected) {
                sendMessage(MessageType.UNSUBSCRIBE, tickerLower)
            }
        }

        /** Снимок активных подписок: снаружи по нему считают дифф, не рассылая лишних сообщений. */
        val activeSubscriptions: Set<String>
            get() = synchronized(lock) { subscribedTickers.toSet() }

        private companion object {
            const val TAG = WebSocketConstants.LOG_TAG
        }

        private fun webSocketListener(): WebSocketListener =
            object : WebSocketListener() {
                override fun onOpen(
                    webSocket: WebSocket,
                    response: Response,
                ) {
                    Log.i(TAG, "connected, http=${response.code}")
                    _connectionState.value = ConnectionState.Connected
                    reconnectAttempts = 0
                    reconnectJob?.cancel()
                    restoreSubscriptions()
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    bytes: ByteString,
                ) {
                    parseAndEmitRaw(bytes.utf8())
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    text: String,
                ) {
                    parseAndEmitRaw(text)
                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?,
                ) {
                    // response == null означает, что до HTTP-ответа дело не дошло:
                    // обычно неверный хост/порт или сервер не отвечает на upgrade
                    Log.e(TAG, "failure on $currentUrl, http=${response?.code}: ${t.message}", t)
                    _connectionState.value = ConnectionState.Error(t.message ?: "Error in websocket", t)
                    reconnect()
                }

                override fun onClosing(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String,
                ) {
                    webSocket.close(code, reason)
                }

                override fun onClosed(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String,
                ) {
                    Log.i(TAG, "closed code=$code reason=$reason")
                    _connectionState.value = ConnectionState.Disconnected
                    this@WebSocketClient.webSocket = null
                    if (!isManuallyDisconnect) {
                        reconnect()
                    }
                }
            }

        private fun sendMessage(
            type: MessageType,
            ticker: String,
        ): Boolean {
            val payload =
                SocketOutboundMessage(
                    type = type.type,
                    data = TickerData(ticker = ticker),
                )
            val sent = webSocket?.send(gson.toJson(payload)) ?: false
            if (sent) {
                Log.d(TAG, "-> ${type.name} $ticker")
            } else {
                // сокет закрыт или очередь переполнена — подписка не ушла
                Log.w(TAG, "-> ${type.name} $ticker NOT SENT, state=${_connectionState.value}")
            }
            return sent
        }

        private fun restoreSubscriptions() {
            val tickers = synchronized(lock) { subscribedTickers.toSet() }
            Log.i(TAG, "restoring ${tickers.size} subscription(s): $tickers")
            tickers.forEach { ticker ->
                sendMessage(MessageType.SUBSCRIBE, ticker)
            }
        }

        private fun parseAndEmitRaw(rawMessage: String) {
            val parsedMessage =
                runCatching { gson.fromJson(rawMessage, SocketInboundRawMessage::class.java) }.getOrNull()
                    ?: run {
                        Log.w(TAG, "<- unparsed: ${rawMessage.take(WebSocketConstants.RAW_LOG_LIMIT)}")
                        return
                    }
            val type = MessageType.fromType(parsedMessage.type)
            val message: SocketDtoMessage =
                when (type) {
                    MessageType.WELCOME -> {
                        val data =
                            runCatching { gson.fromJson(parsedMessage.data, WelcomeData::class.java) }.getOrNull()
                                ?: WelcomeData("")
                        SocketDtoMessage.Welcome(parsedMessage.id, data = data)
                    }

                    MessageType.SUBSCRIBE -> {
                        val data =
                            runCatching { gson.fromJson(parsedMessage.data, TickerData::class.java) }.getOrNull()
                                ?: TickerData("")
                        SocketDtoMessage.Subscribe(parsedMessage.id, data = data)
                    }

                    MessageType.UNSUBSCRIBE -> {
                        val data =
                            runCatching { gson.fromJson(parsedMessage.data, TickerData::class.java) }.getOrNull()
                                ?: TickerData("")
                        SocketDtoMessage.Unsubscribe(parsedMessage.id, data = data)
                    }

                    MessageType.PRICE_CHANGE -> {
                        val data =
                            runCatching {
                                gson.fromJson(
                                    parsedMessage.data,
                                    SymbolPriceChangeData::class.java,
                                )
                            }.getOrNull()
                                ?: return
                        SocketDtoMessage.SymbolPriceChange(parsedMessage.id, data = data)
                    }

                    MessageType.ERROR -> {
                        val data =
                            runCatching { gson.fromJson(parsedMessage.data, ErrorData::class.java) }.getOrNull()
                                ?: ErrorData(error = "", errorCode = WebSocketConstants.UNKNOWN_ERROR_CODE)
                        SocketDtoMessage.Error(parsedMessage.id, data = data)
                    }
                }

            // тиков много (десятки в секунду на тикер), поэтому они на verbose,
            // а редкие служебные сообщения — на debug
            if (type == MessageType.PRICE_CHANGE) {
                Log.v(TAG, "<- PRICE_CHANGE ${parsedMessage.data}")
            } else {
                Log.d(TAG, "<- ${type.name} ${parsedMessage.data}")
            }

            _messages.tryEmit(message)
        }

        private fun reconnectDelayMs(attempt: Int): Long {
            val exponent = min(attempt, WebSocketConstants.MAX_EXPONENT)
            val expMultiplier = 1L shl exponent
            val baseDelay =
                min(
                    WebSocketConstants.MAX_RECONNECT_DELAY_MS,
                    WebSocketConstants.BASE_RECONNECT_DELAY_MS * expMultiplier,
                )
            val jitter = Random.nextLong(WebSocketConstants.RECONNECT_JITTER_MS + 1)
            return baseDelay + jitter
        }

        fun close() {
            disconnect()
            scope.cancel()
        }
    }
