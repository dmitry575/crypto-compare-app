package com.cryptocompare.domain.repository

import com.cryptocompare.model.ticker.TickerConnectionState
import com.cryptocompare.model.ticker.TickerStreamEvent
import kotlinx.coroutines.flow.Flow

interface TickerStreamRepository {
    val connectionState: Flow<TickerConnectionState>
    val event: Flow<TickerStreamEvent>

    fun connect()

    fun disconnect()

    fun subscribe(ticker: String)

    fun unsubscribe(ticker: String)
}
