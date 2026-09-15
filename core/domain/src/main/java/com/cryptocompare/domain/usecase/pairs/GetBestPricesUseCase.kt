package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.helpers.isComplete
import com.cryptocompare.model.ticker.TickerBestPrice
import javax.inject.Inject

/**
 * Лучшие пары тикера с бэкенда, по одной на символ, — только полные.
 *
 * Отсюда берёт разницу между биржами экран деталей. Раньше он считал её сам по
 * разбивке, которая приходит без фильтра свежести, и на `ethusdc` 2026-09-14
 * рисовал «купить на bingx, продать на kraken, +0.87%»: цена kraken стояла, а
 * бэкенд, каталог и экран сравнения показывали +0.078%.
 */
class GetBestPricesUseCase
    @Inject
    constructor(
        private val cryptoCompareRepository: CryptoCompareRepository,
    ) {
        /** С [symbolId] — только лучшая пара этого символа: сети не смешиваются. */
        suspend operator fun invoke(
            ticker: String,
            symbolId: Long? = null,
        ): Result<List<TickerBestPrice>> =
            cryptoCompareRepository.getBestPricesByTicker(ticker).map { rows ->
                rows.filter { it.isComplete() && (symbolId == null || it.symbolId == symbolId) }
            }
    }
