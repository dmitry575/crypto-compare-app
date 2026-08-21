package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.TickerStreamRepository
import com.cryptocompare.helpers.util.WebSocketConstants
import javax.inject.Inject

class SyncVisibleTickersUseCase
    @Inject
    constructor(
        private val tickerStreamRepository: TickerStreamRepository,
    ) {
        operator fun invoke(
            visibleTickers: List<String>,
            subscribedTickers: Set<String>,
        ): Set<String> {
            // Экран отдаёт всё, что реально видно, а сколько из этого потянет
            // соединение — знает только этот слой. Берём верхние строки:
            // подписок получается min(видимые, MAX_SUBSCRIPTIONS).
            val normalizedVisibleTickers =
                visibleTickers
                    .asSequence()
                    .map { it.lowercase() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(WebSocketConstants.MAX_SUBSCRIPTIONS)
                    .toSet()

            val toUnsubscribe = subscribedTickers - normalizedVisibleTickers
            val toSubscribe = normalizedVisibleTickers - subscribedTickers

            toUnsubscribe.forEach(tickerStreamRepository::unsubscribe)
            toSubscribe.forEach(tickerStreamRepository::subscribe)

            return normalizedVisibleTickers
        }
    }
