package com.cryptocompare.pairs.util

import com.cryptocompare.model.ticker.TickerConnectionState
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamStatusTest {
    @Test
    fun `connected socket means live prices`() {
        assertEquals(StreamStatus.LIVE, StreamStatus.of(TickerConnectionState.Connected))
    }

    @Test
    fun `first connect and reconnect look the same to the user`() {
        // разница между «первый раз открываем» и «переоткрываем» — внутренняя;
        // на экране и там и там связь ещё не установлена
        assertEquals(StreamStatus.RECONNECTING, StreamStatus.of(TickerConnectionState.Connecting))
        assertEquals(
            StreamStatus.RECONNECTING,
            StreamStatus.of(TickerConnectionState.Reconnecting(attempts = 3, timeDelay = 8_000L)),
        )
    }

    @Test
    fun `no socket and a socket error both mean offline`() {
        assertEquals(StreamStatus.OFFLINE, StreamStatus.of(TickerConnectionState.Disconnected))
        assertEquals(StreamStatus.OFFLINE, StreamStatus.of(TickerConnectionState.Error("host unreachable")))
    }
}
