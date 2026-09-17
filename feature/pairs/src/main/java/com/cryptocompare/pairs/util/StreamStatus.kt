package com.cryptocompare.pairs.util

import com.cryptocompare.model.ticker.TickerConnectionState

/**
 * Что показать про поток котировок: цены живые, связь восстанавливается или её нет.
 *
 * Пять состояний сокета пользователю ни о чём не говорят — ему важно ровно одно:
 * верить ли числам на экране. Попытки и задержка бэкоффа остаются внутри
 * [TickerConnectionState], наружу идёт только смысл.
 */
enum class StreamStatus {
    LIVE,
    RECONNECTING,
    OFFLINE,
    ;

    companion object {
        fun of(state: TickerConnectionState): StreamStatus =
            when (state) {
                is TickerConnectionState.Connected -> LIVE
                is TickerConnectionState.Connecting -> RECONNECTING
                is TickerConnectionState.Reconnecting -> RECONNECTING
                is TickerConnectionState.Disconnected -> OFFLINE
                is TickerConnectionState.Error -> OFFLINE
            }
    }
}
