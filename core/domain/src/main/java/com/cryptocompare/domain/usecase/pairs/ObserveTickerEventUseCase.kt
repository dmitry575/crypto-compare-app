package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.TickerStreamRepository
import com.cryptocompare.model.ticker.TickerStreamEvent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTickerEventUseCase
    @Inject
    constructor(
        val tickerStreamRepository: TickerStreamRepository,
    ) {
        operator fun invoke(): Flow<TickerStreamEvent> = tickerStreamRepository.event
    }
