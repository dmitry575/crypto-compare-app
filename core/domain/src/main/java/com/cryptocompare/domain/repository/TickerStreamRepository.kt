package com.cryptocompare.domain.repository

import com.cryptocompare.model.ticker.TickerConnectionState
import com.cryptocompare.model.ticker.TickerStreamEvent
import kotlinx.coroutines.flow.Flow

interface TickerStreamRepository {
    val connectionState: Flow<TickerConnectionState>
    val event: Flow<TickerStreamEvent>

    /**
     * Соединение открылось заново — после разрыва или возврата из фона. Тики за
     * время разрыва потеряны: сервер их не досылает и снимка при подписке не
     * присылает, поэтому экран по этому сигналу берёт свежие цены через REST.
     * Первое открытие, случившееся до подписки на поток, не приходит.
     */
    val reconnects: Flow<Unit>

    fun connect()

    fun disconnect()

    /** Приложение ушло в фон: закрыть соединение, сохранив подписки. */
    fun pause()

    /** Приложение вернулось: открыть соединение, если его закрыл [pause]. */
    fun resume()

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
