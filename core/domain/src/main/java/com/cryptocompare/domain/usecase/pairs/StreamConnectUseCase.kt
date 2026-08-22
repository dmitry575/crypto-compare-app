package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.TickerStreamRepository
import javax.inject.Inject

class StreamConnectUseCase
    @Inject
    constructor(
        private val tickerStreamRepository: TickerStreamRepository,
    ) {
        /** Повторный вызов безопасен: соединение не рвётся, если уже открыто. */
        operator fun invoke() {
            tickerStreamRepository.connect()
        }
    }
