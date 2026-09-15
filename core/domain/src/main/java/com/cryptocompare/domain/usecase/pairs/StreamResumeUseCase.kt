package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.TickerStreamRepository
import javax.inject.Inject

class StreamResumeUseCase
    @Inject
    constructor(
        private val tickerStreamRepository: TickerStreamRepository,
    ) {
        /** Приложение вернулось: поток открывается, если его закрыл уход в фон. */
        operator fun invoke() {
            tickerStreamRepository.resume()
        }
    }
