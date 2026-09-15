package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.TickerStreamRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveStreamReconnectsUseCase
    @Inject
    constructor(
        private val tickerStreamRepository: TickerStreamRepository,
    ) {
        /**
         * Соединение открылось заново. Цены на экране с этого момента могут быть
         * старыми: тики за время разрыва не досылаются, а после возврата из фона
         * разрыв бывает и в час.
         */
        operator fun invoke(): Flow<Unit> = tickerStreamRepository.reconnects
    }
