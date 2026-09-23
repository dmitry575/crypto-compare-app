package com.cryptocompare.domain.usecase.portfolio

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.model.symbol.SymbolSellQuote
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Цены продажи символов портфеля из каталога.
 *
 * Своего источника цен у портфеля нет и не нужно: строки каталога держит
 * свежими тот же сокет, и позиция по паре, которую пользователь смотрит,
 * переоценивается вместе с её строкой.
 */
class ObservePortfolioPricesUseCase
    @Inject
    constructor(
        private val cryptoCompareRepository: CryptoCompareRepository,
    ) {
        operator fun invoke(symbolIds: Set<Long>): Flow<Map<Long, SymbolSellQuote>> =
            cryptoCompareRepository.observeSellQuotes(symbolIds)
    }
