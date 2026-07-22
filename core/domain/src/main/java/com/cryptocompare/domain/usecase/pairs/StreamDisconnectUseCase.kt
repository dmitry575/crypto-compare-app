package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.TickerStreamRepository
import javax.inject.Inject

class StreamDisconnectUseCase
    @Inject
    constructor(
        private val tickerStreamRepository: TickerStreamRepository,
    ) {
        operator fun invoke() {
            tickerStreamRepository.disconnect()
        }
    }
