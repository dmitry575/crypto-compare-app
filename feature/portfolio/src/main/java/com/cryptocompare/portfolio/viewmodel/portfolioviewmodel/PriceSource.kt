package com.cryptocompare.portfolio.viewmodel.portfolioviewmodel

import com.cryptocompare.model.portfolio.PortfolioPosition

/**
 * Откуда берётся цена позиции — по этому ключу портфель помнит, за чем уже
 * сходил по REST. Позиция без биржи делит источник со всеми символами своего
 * тикера: лучшие пары приходят одним запросом на тикер. Позиция с биржей —
 * свой источник, и смена биржи делает его новым: цену надо брать заново.
 */
internal sealed interface PriceSource {
    data class Best(
        val ticker: String,
    ) : PriceSource

    data class Pinned(
        val symbolId: Long,
        val providerId: Int,
    ) : PriceSource
}

internal fun PortfolioPosition.priceSource(): PriceSource =
    providerId?.let { PriceSource.Pinned(symbolId, it) } ?: PriceSource.Best(ticker.lowercase())
