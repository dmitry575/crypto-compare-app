package com.cryptocompare.domain.usecase.portfolio

import com.cryptocompare.domain.repository.PortfolioRepository
import com.cryptocompare.helpers.validPriceOrNull
import com.cryptocompare.model.portfolio.PortfolioQuote
import com.cryptocompare.model.ticker.TickerPrice
import javax.inject.Inject

/**
 * Пишет котировки бирж из сокета (событие типа 4) как последние цены позиций.
 *
 * Какие тики относятся к позициям, решает экран: он знает, за какой биржей
 * закреплён каждый символ. Здесь только перевод в цену продажи — у события
 * имена от лица биржи, и её bid это `priceBuy` (решение 6 в `CLAUDE.md`).
 */
class ApplyPinnedPriceTicksUseCase
    @Inject
    constructor(
        private val portfolioRepository: PortfolioRepository,
    ) {
        suspend operator fun invoke(ticks: List<TickerPrice>): Result<Unit> {
            val now = System.currentTimeMillis()
            val quotes =
                ticks.mapNotNull { tick ->
                    tick.priceBuy.validPriceOrNull()?.let { price ->
                        PortfolioQuote(
                            symbolId = tick.symbolId.toLong(),
                            providerId = tick.providerId,
                            price = price,
                            quotedAtMillis = now,
                        )
                    }
                }
            if (quotes.isEmpty()) return Result.success(Unit)

            return portfolioRepository.savePinnedQuotes(quotes)
        }
    }
