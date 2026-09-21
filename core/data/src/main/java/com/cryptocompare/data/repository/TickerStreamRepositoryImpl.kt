package com.cryptocompare.data.repository

import com.cryptocompare.domain.repository.CrashReporter
import com.cryptocompare.domain.repository.TickerStreamRepository
import com.cryptocompare.helpers.util.WebSocketConstants
import com.cryptocompare.model.ticker.TickerBestPrice
import com.cryptocompare.model.ticker.TickerConnectionState
import com.cryptocompare.model.ticker.TickerPrice
import com.cryptocompare.model.ticker.TickerStreamEvent
import com.cryptocompare.network.dto.webSocketDTO.SocketDtoMessage
import com.cryptocompare.network.websocket.ConnectionState
import com.cryptocompare.network.websocket.WebSocketClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Named

class TickerStreamRepositoryImpl
    @Inject
    constructor(
        private val webSocketClient: WebSocketClient,
        @Named("wsUrl") private val wsUrl: String,
        private val crashReporter: CrashReporter,
        @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher,
    ) : TickerStreamRepository {
        // Репозиторий — синглтон на всё приложение, и разбор сообщений живёт
        // столько же. Supervisor: падение разбора не должно уносить область,
        // в которой поток поднимется снова.
        private val streamScope = CoroutineScope(SupervisorJob() + ioDispatcher)

        override val connectionState: Flow<TickerConnectionState> =
            webSocketClient.connectionState
                // сбой сокета сейчас превращается в строку и теряется; логируем
                // его non-fatal здесь — в core:network Firebase тащить нельзя,
                // сеть не должна зависеть от домена
                .onEach { state ->
                    if (state is ConnectionState.Error) {
                        state.cause?.let(crashReporter::recordException)
                    }
                }.map { state ->
                    when (state) {
                        is ConnectionState.Connecting -> TickerConnectionState.Connecting
                        is ConnectionState.Connected -> TickerConnectionState.Connected
                        is ConnectionState.Disconnected -> TickerConnectionState.Disconnected
                        is ConnectionState.Error ->
                            TickerConnectionState.Error(
                                errorMsg = state.errorMsg,
                            )

                        is ConnectionState.Reconnecting ->
                            TickerConnectionState.Reconnecting(
                                attempts = state.attempts,
                                timeDelay = state.timeDelay,
                            )
                    }
                }

        /**
         * Разбор сообщений общий на всех подписчиков.
         *
         * Раньше это был холодный `map`: каждый экран разбирал **свою** копию
         * каждого сообщения, и при открытом портфеле поверх живого каталога
         * десятки тиков в секунду превращались в домен дважды.
         *
         * `WhileSubscribed` без replay: подписчиков нет — не разбираем ничего,
         * опоздавший подписчик получает события с момента подписки, как и
         * раньше (снимка цены в потоке всё равно нет, свежее берётся REST).
         */
        override val event: Flow<TickerStreamEvent> =
            webSocketClient.messages
                .map { message ->
                    when (message) {
                        is SocketDtoMessage.Welcome ->
                            TickerStreamEvent.Welcome(
                                id = message.id,
                                message = message.data.message,
                            )

                        is SocketDtoMessage.Subscribe ->
                            TickerStreamEvent.Subscribe(
                                id = message.id,
                                ticker = message.data.ticker,
                            )

                        is SocketDtoMessage.Unsubscribe ->
                            TickerStreamEvent.Unsubscribe(
                                id = message.id,
                                ticker = message.data.ticker,
                            )

                        is SocketDtoMessage.SymbolPriceChange ->
                            TickerStreamEvent.TickerPriceChange(
                                id = message.id,
                                data =
                                    TickerPrice(
                                        ticker = message.data.ticker,
                                        symbolId = message.data.symbolId,
                                        providerId = message.data.providerId,
                                        priceSell = message.data.priceSell,
                                        priceBuy = message.data.priceBuy,
                                    ),
                            )

                        is SocketDtoMessage.SymbolBestPriceChange ->
                            TickerStreamEvent.TickerBestPriceChange(
                                id = message.id,
                                data =
                                    TickerBestPrice(
                                        ticker = message.data.ticker,
                                        symbolId = message.data.symbolId,
                                        bestAskProviderId = message.data.bestAskProviderId,
                                        bestAskPrice = message.data.bestAskPrice,
                                        bestBidProviderId = message.data.bestBidProviderId,
                                        bestBidPrice = message.data.bestBidPrice,
                                        spreadPercent = message.data.spreadPercent,
                                    ),
                            )

                        is SocketDtoMessage.Error ->
                            TickerStreamEvent.Error(
                                id = message.id,
                                error = message.data.error,
                                errorCode = message.data.errorCode,
                            )
                    }
                }.retryWhen { cause, _ ->
                    // одно сообщение, которое не разобралось, не должно навсегда
                    // оставить экраны без цен: сообщаем о нём и слушаем дальше
                    crashReporter.recordException(cause)
                    true
                }.buffer(WebSocketConstants.EVENT_BUFFER_CAPACITY, BufferOverflow.DROP_OLDEST)
                .shareIn(streamScope, SharingStarted.WhileSubscribed(), replay = 0)

        // база каталога и счётчик захватов живут вместе с синглтоном-репозиторием,
        // а не в отдельных ViewModel'ях: только так набор переживает наложение
        // экранов деталей друг на друга
        private val takeoverLock = Any()
        private var catalogBaseline: Set<String>? = null
        private var activeTakeovers = 0

        // drop(1): StateFlow отдаёт текущее число сразу при подписке, а это не
        // новое соединение, а то, что уже было
        override val reconnects: Flow<Unit> =
            webSocketClient.openedConnections
                .drop(1)
                .map { }

        override fun connect() {
            webSocketClient.connect(wsUrl)
        }

        override fun disconnect() {
            webSocketClient.disconnect()
        }

        override fun pause() {
            webSocketClient.pause()
        }

        override fun resume() {
            webSocketClient.resume()
        }

        override fun subscribe(ticker: String) {
            webSocketClient.subscribe(ticker)
        }

        override fun unsubscribe(ticker: String) {
            webSocketClient.unsubscribe(ticker)
        }

        override fun beginTickerTakeover(tickers: Set<String>) {
            val normalizedTickers = tickers.mapNotNull { it.lowercase().takeIf(String::isNotBlank) }.toSet()

            synchronized(takeoverLock) {
                // база снимается только у первого захвата: вложенный экран не должен
                // запомнить как «каталог» уже урезанный набор
                if (activeTakeovers == 0) {
                    catalogBaseline = webSocketClient.activeSubscriptions
                }
                activeTakeovers++
            }

            // оставляем в соединении ровно тикеры экрана
            val current = webSocketClient.activeSubscriptions
            (current - normalizedTickers).forEach(webSocketClient::unsubscribe)
            (normalizedTickers - current).forEach(webSocketClient::subscribe)
        }

        override fun endTickerTakeover() {
            val baseline =
                synchronized(takeoverLock) {
                    if (activeTakeovers > 0) activeTakeovers--
                    // каталог возвращаем только когда ушёл последний захвативший экран
                    if (activeTakeovers == 0) catalogBaseline.also { catalogBaseline = null } else null
                } ?: return

            // приводим подписки к сохранённому набору: лишние снимаем, недостающие досылаем
            val current = webSocketClient.activeSubscriptions
            (current - baseline).forEach(webSocketClient::unsubscribe)
            (baseline - current).forEach(webSocketClient::subscribe)
        }
    }
