package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.TickerStreamRepository
import javax.inject.Inject

class StreamPauseUseCase
    @Inject
    constructor(
        private val tickerStreamRepository: TickerStreamRepository,
    ) {
        /** Приложение ушло в фон: поток котировок закрывается, подписки остаются. */
        operator fun invoke() {
            tickerStreamRepository.pause()
        }
    }
