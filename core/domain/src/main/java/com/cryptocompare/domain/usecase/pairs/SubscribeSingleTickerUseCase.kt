package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.TickerStreamRepository
import javax.inject.Inject

/**
 * Соединение держит ограниченное число подписок, поэтому экран деталей
 * забирает их себе целиком: снимает все чужие и остаётся с одним тикером.
 * Возвращает прежний набор — по нему подписки восстанавливаются при уходе.
 */
class SubscribeSingleTickerUseCase
    @Inject
    constructor(
        private val tickerStreamRepository: TickerStreamRepository,
    ) {
        operator fun invoke(ticker: String): Set<String> {
            val previousSubscriptions = tickerStreamRepository.activeSubscriptions
            val normalizedTicker = ticker.lowercase()

            (previousSubscriptions - normalizedTicker).forEach(tickerStreamRepository::unsubscribe)
            if (normalizedTicker !in previousSubscriptions) {
                tickerStreamRepository.subscribe(normalizedTicker)
            }

            return previousSubscriptions
        }
    }
