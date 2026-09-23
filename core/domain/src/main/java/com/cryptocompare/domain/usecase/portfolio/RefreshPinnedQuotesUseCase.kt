package com.cryptocompare.domain.usecase.portfolio

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.domain.repository.PortfolioRepository
import com.cryptocompare.helpers.validPriceOrNull
import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.model.portfolio.PortfolioQuote
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Дотягивает по REST цены бирж, за которыми закреплены позиции, и сохраняет их.
 *
 * Нужен по той же причине, что и догонка каталога: сокет прошлое не досылает,
 * а котировок отдельных бирж в каталоге нет вовсе. Берётся разбивка тикера по
 * биржам, и из неё — строка ровно этого символа на ровно этой бирже: у тикера
 * бывает несколько сетей, и цена чужой сети была бы ценой другого актива.
 *
 * Цена продажи — `priceBuy`: в разбивке имена от лица биржи, и её bid — это
 * `priceBuy` (решение 6 в `CLAUDE.md`).
 *
 * Как и [com.cryptocompare.domain.usecase.pairs.RefreshBestPricesUseCase],
 * отдаёт **число** сохранённых цен: ноль значит «ни один запрос не прошёл»,
 * а не «цены свежие».
 */
class RefreshPinnedQuotesUseCase
    @Inject
    constructor(
        private val cryptoCompareRepository: CryptoCompareRepository,
        private val portfolioRepository: PortfolioRepository,
    ) {
        suspend operator fun invoke(positions: List<PortfolioPosition>): Result<Int> {
            val pinned = positions.filter { it.providerId != null }
            if (pinned.isEmpty()) return Result.success(0)

            val quotes =
                coroutineScope {
                    pinned
                        .groupBy { it.ticker.lowercase() }
                        .map { (ticker, tickerPositions) ->
                            async {
                                val rows = cryptoCompareRepository.getSymbolsByTicker(ticker).getOrNull().orEmpty()
                                val now = System.currentTimeMillis()

                                tickerPositions.mapNotNull { position ->
                                    val row =
                                        rows.firstOrNull {
                                            it.id == position.symbolId && it.providerId == position.providerId
                                        } ?: return@mapNotNull null
                                    val price = row.priceBuy.validPriceOrNull() ?: return@mapNotNull null

                                    PortfolioQuote(
                                        symbolId = position.symbolId,
                                        providerId = row.providerId,
                                        price = price,
                                        quotedAtMillis = row.quotedAtMillis ?: now,
                                    )
                                }
                            }
                        }.awaitAll()
                        .flatten()
                }
            if (quotes.isEmpty()) return Result.success(0)

            return portfolioRepository.savePinnedQuotes(quotes).map { quotes.size }
        }
    }
