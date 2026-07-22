package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.model.ticker.TickerPrice
import javax.inject.Inject

class ApplyTickerPriceChangesUseCase
    @Inject
    constructor(
        private val cryptoCompareRepository: CryptoCompareRepository,
    ) {
        suspend operator fun invoke(updates: List<TickerPrice>): Result<Unit> {
            if (updates.isEmpty()) return Result.success(Unit)

            return cryptoCompareRepository.applyPriceUpdates(updates)
        }
    }
