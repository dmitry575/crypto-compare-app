package com.cryptocompare.domain.usecase.portfolio

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.domain.repository.PortfolioRepository
import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.model.symbol.SymbolSellQuote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Цены продажи позиций портфеля, по `symbolId`.
 *
 * Позиция без биржи берёт цену из каталога: строки каталога держит свежими тот
 * же сокет, и позиция по паре, которую пользователь смотрит, переоценивается
 * вместе с её строкой. Позиция с биржей — только цену этой биржи, даже если
 * лучший bid сейчас на другой: продать монету можно только там, где она лежит.
 */
class ObservePortfolioPricesUseCase
    @Inject
    constructor(
        private val cryptoCompareRepository: CryptoCompareRepository,
        private val portfolioRepository: PortfolioRepository,
    ) {
        operator fun invoke(positions: List<PortfolioPosition>): Flow<Map<Long, SymbolSellQuote>> {
            val pinned = positions.filter { it.providerId != null }.map { it.symbolId }.toSet()
            val best = positions.map { it.symbolId }.toSet() - pinned
            val catalogQuotes = cryptoCompareRepository.observeSellQuotes(best)
            if (pinned.isEmpty()) return catalogQuotes

            return combine(catalogQuotes, portfolioRepository.observePinnedQuotes()) { catalog, own ->
                catalog + own.filterKeys { it in pinned }
            }
        }
    }
