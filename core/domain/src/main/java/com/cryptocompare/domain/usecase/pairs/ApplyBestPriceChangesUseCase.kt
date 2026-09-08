package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.model.ticker.TickerBestPrice
import javax.inject.Inject

/**
 * Пишет в каталог лучшие пары цен, накопленные из сокета.
 *
 * Принимает только событие типа 5. Тик отдельной биржи сюда не отдаётся: его
 * `symbolId` общий на весь тикер, поэтому строка каталога получала бы bid/ask
 * той биржи, которая тикнула последней, вместо разницы между биржами.
 */
class ApplyBestPriceChangesUseCase
    @Inject
    constructor(
        private val cryptoCompareRepository: CryptoCompareRepository,
    ) {
        suspend operator fun invoke(updates: List<TickerBestPrice>): Result<Unit> {
            if (updates.isEmpty()) return Result.success(Unit)

            return cryptoCompareRepository.applyBestPriceUpdates(updates)
        }
    }
