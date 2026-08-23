package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.TickerStreamRepository
import javax.inject.Inject

/**
 * Завершает захват подписок экраном деталей. Когда уходит последний захвативший
 * экран, репозиторий возвращает соединению сохранённый набор каталога.
 */
class RestoreTickerSubscriptionsUseCase
    @Inject
    constructor(
        private val tickerStreamRepository: TickerStreamRepository,
    ) {
        operator fun invoke() = tickerStreamRepository.endSingleTickerTakeover()
    }
