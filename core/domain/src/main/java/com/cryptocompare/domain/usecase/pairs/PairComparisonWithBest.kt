package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.model.comparison.PairComparison
import com.cryptocompare.model.ticker.TickerBestPrice

/**
 * Выжимка сравнения берётся из одной пары целиком, стороны не смешиваются.
 * Какую пару взять, решают `isComplete` и `widest` из `core:helpers`.
 */
internal fun PairComparison.withBest(best: TickerBestPrice): PairComparison =
    copy(
        bestAskProviderId = best.bestAskProviderId,
        bestAskPrice = best.bestAskPrice,
        bestBidProviderId = best.bestBidProviderId,
        bestBidPrice = best.bestBidPrice,
        spreadPercent = best.spreadPercent,
    )
