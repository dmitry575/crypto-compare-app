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

    /**
     * Экран деталей забирает единственную подписку себе: слотов у соединения мало.
     * Набор каталога сохраняется на время захвата и возвращается, когда уходит
     * последний захвативший экран. Захваты считаются — база снимается только у
     * первого и восстанавливается только у последнего, иначе наложившийся экран
     * (например, при пересоздании) затёр бы подписки каталога поверх активного
     * экрана деталей или вернул бы их слишком рано.
     */
    fun beginSingleTickerTakeover(ticker: String)

    /** Возвращает подписки каталога, если это был последний захват; иначе просто уменьшает счётчик. */
    fun endSingleTickerTakeover()
}
