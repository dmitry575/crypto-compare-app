package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.TickerStreamRepository
import javax.inject.Inject

/** Приводит подписки соединения к переданному набору: лишние снимает, недостающие досылает. */
class RestoreTickerSubscriptionsUseCase
    @Inject
    constructor(
        private val tickerStreamRepository: TickerStreamRepository,
    ) {
        operator fun invoke(tickers: Set<String>) {
            val normalizedTickers =
                tickers
                    .asSequence()
                    .map { it.lowercase() }
                    .filter { it.isNotBlank() }
                    .toSet()

            val currentSubscriptions = tickerStreamRepository.activeSubscriptions

            (currentSubscriptions - normalizedTickers).forEach(tickerStreamRepository::unsubscribe)
            (normalizedTickers - currentSubscriptions).forEach(tickerStreamRepository::subscribe)
        }
    }
