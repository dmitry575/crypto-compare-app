package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.TickerStreamRepository
import javax.inject.Inject

/**
 * Соединение держит ограниченное число подписок, поэтому экран деталей забирает
 * их себе целиком: снимает все чужие и остаётся с одним тикером. Набор каталога
 * репозиторий сохраняет сам и вернёт при уходе последнего экрана — см.
 * [RestoreTickerSubscriptionsUseCase].
 */
class SubscribeSingleTickerUseCase
    @Inject
    constructor(
        private val tickerStreamRepository: TickerStreamRepository,
    ) {
        operator fun invoke(ticker: String) = tickerStreamRepository.beginSingleTickerTakeover(ticker)
    }
