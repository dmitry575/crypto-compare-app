package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Дотягивает лучшие пары видимых тикеров через REST и пишет их в каталог.
 *
 * Нужен после реконнекта: сокет не досылает пропущенное, а у малоликвидной пары
 * следующее событие может не прийти минутами — всё это время строка каталога
 * показывала бы цену часовой давности. Тикеров здесь не больше лимита подписок,
 * поэтому запросов немного.
 *
 * Тикер, по которому запрос не прошёл, пропускается: остальные обновятся, а он
 * дождётся сокета, как и раньше. Поэтому наружу идёт **число** обновлённых
 * котировок, а не `Unit`: без сети не проходит ни один запрос, и «успех» с нулём
 * — это не «цены свежие», а «не удалось». Экран по этому числу и решает, двигать
 * ли время последнего обновления.
 */
class RefreshBestPricesUseCase
    @Inject
    constructor(
        private val cryptoCompareRepository: CryptoCompareRepository,
    ) {
        suspend operator fun invoke(tickers: Set<String>): Result<Int> {
            if (tickers.isEmpty()) return Result.success(0)

            val updates =
                coroutineScope {
                    tickers
                        .map { ticker -> async { cryptoCompareRepository.getBestPricesByTicker(ticker).getOrNull() } }
                        .awaitAll()
                        .filterNotNull()
                        .flatten()
                }
            if (updates.isEmpty()) return Result.success(0)

            return cryptoCompareRepository.applyBestPriceUpdates(updates).map { updates.size }
        }
    }
