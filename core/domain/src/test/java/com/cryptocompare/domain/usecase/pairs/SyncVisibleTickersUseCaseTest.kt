package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.TickerStreamRepository
import com.cryptocompare.helpers.util.WebSocketConstants
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncVisibleTickersUseCaseTest {
    private val repository: TickerStreamRepository = mockk(relaxed = true)
    private val useCase = SyncVisibleTickersUseCase(repository)

    @Test
    fun `subscribes to every visible ticker while under the limit`() {
        val visible = listOf("BTCUSDT", "ethusdt", "adausdt")

        val result = useCase(visible, mutableSetOf())

        assertEquals(setOf("btcusdt", "ethusdt", "adausdt"), result)
        verify(exactly = 1) { repository.subscribe("btcusdt") }
        verify(exactly = 1) { repository.subscribe("ethusdt") }
        verify(exactly = 1) { repository.subscribe("adausdt") }
    }

    @Test
    fun `never subscribes to more than the backend accepts`() {
        // бэкенд отвечает Subscribe failed начиная с девятой подписки в соединении
        val visible = (1..20).map { index -> "ticker${index}usdt" }

        val result = useCase(visible, mutableSetOf())

        assertEquals(WebSocketConstants.MAX_SUBSCRIPTIONS, result.size)
        verify(exactly = WebSocketConstants.MAX_SUBSCRIPTIONS) { repository.subscribe(any()) }
    }

    @Test
    fun `keeps the topmost rows when the screen shows more than the limit`() {
        val visible = (1..12).map { index -> "ticker${index}usdt" }

        val result = useCase(visible, mutableSetOf())

        assertEquals(visible.take(WebSocketConstants.MAX_SUBSCRIPTIONS).toSet(), result)
        verify(exactly = 0) { repository.subscribe("ticker9usdt") }
    }

    @Test
    fun `unsubscribes from tickers that left the screen`() {
        val alreadySubscribed = mutableSetOf("btcusdt", "ethusdt")

        val result = useCase(listOf("ethusdt", "adausdt"), alreadySubscribed)

        assertEquals(setOf("ethusdt", "adausdt"), result)
        verify(exactly = 1) { repository.unsubscribe("btcusdt") }
        verify(exactly = 1) { repository.subscribe("adausdt") }
        // ethusdt остался на экране — трогать его не надо
        verify(exactly = 0) { repository.subscribe("ethusdt") }
        verify(exactly = 0) { repository.unsubscribe("ethusdt") }
    }

    @Test
    fun `blank tickers do not eat subscription slots`() {
        val visible = listOf("", "  ", "btcusdt", "ethusdt")

        val result = useCase(visible, mutableSetOf())

        assertEquals(setOf("btcusdt", "ethusdt"), result)
        verify(exactly = 2) { repository.subscribe(any()) }
    }

    @Test
    fun `duplicates do not eat subscription slots`() {
        val visible = listOf("BTCUSDT", "btcusdt", "ethusdt")

        val result = useCase(visible, mutableSetOf())

        assertEquals(setOf("btcusdt", "ethusdt"), result)
        verify(exactly = 1) { repository.subscribe("btcusdt") }
    }
}
