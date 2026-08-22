package com.cryptocompare.domain.repository

import com.cryptocompare.model.ticker.TickerConnectionState
import com.cryptocompare.model.ticker.TickerStreamEvent
import kotlinx.coroutines.flow.Flow

interface TickerStreamRepository {
    val connectionState: Flow<TickerConnectionState>
    val event: Flow<TickerStreamEvent>

    /** Тикеры, подписка на которые сейчас активна в соединении. */
    val activeSubscriptions: Set<String>

    fun connect()

    fun disconnect()

    fun subscribe(ticker: String)

    fun unsubscribe(ticker: String)
}
