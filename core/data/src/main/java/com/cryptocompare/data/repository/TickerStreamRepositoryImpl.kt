package com.cryptocompare.data.repository

import com.cryptocompare.domain.repository.CrashReporter
import com.cryptocompare.domain.repository.TickerStreamRepository
import com.cryptocompare.model.ticker.TickerConnectionState
import com.cryptocompare.model.ticker.TickerPrice
import com.cryptocompare.model.ticker.TickerStreamEvent
import com.cryptocompare.network.dto.webSocketDTO.SocketDtoMessage
import com.cryptocompare.network.websocket.ConnectionState
import com.cryptocompare.network.websocket.WebSocketClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Named

class TickerStreamRepositoryImpl
    @Inject
    constructor(
        private val webSocketClient: WebSocketClient,
        @Named("wsUrl") private val wsUrl: String,
        private val crashReporter: CrashReporter,
    ) : TickerStreamRepository {
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

        override val event: Flow<TickerStreamEvent> =
            webSocketClient.messages.map { message ->
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

                    is SocketDtoMessage.Error ->
                        TickerStreamEvent.Error(
                            id = message.id,
                            error = message.data.error,
                            errorCode = message.data.errorCode,
                        )
                }
            }

        override fun connect() {
            webSocketClient.connect(wsUrl)
        }

        override fun disconnect() {
            webSocketClient.disconnect()
        }

        override fun subscribe(ticker: String) {
            webSocketClient.subscribe(ticker)
        }

        override fun unsubscribe(ticker: String) {
            webSocketClient.unsubscribe(ticker)
        }
    }
