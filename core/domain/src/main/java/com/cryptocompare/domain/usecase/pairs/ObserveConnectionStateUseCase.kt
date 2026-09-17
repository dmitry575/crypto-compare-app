package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.TickerStreamRepository
import com.cryptocompare.model.ticker.TickerConnectionState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConnectionStateUseCase
    @Inject
    constructor(
        private val tickerStreamRepository: TickerStreamRepository,
    ) {
        /**
         * Состояние сокета: по нему экран говорит, живые сейчас цены или нет.
         *
         * Отличается от [ObserveStreamReconnectsUseCase]: тот сообщает о факте
         * нового соединения, чтобы перезапросить цены, а этот — о текущем
         * состоянии, которое видит пользователь.
         */
        operator fun invoke(): Flow<TickerConnectionState> = tickerStreamRepository.connectionState
    }
