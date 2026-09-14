package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.helpers.validPriceOrNull
import com.cryptocompare.model.comparison.PairComparison
import com.cryptocompare.model.ticker.TickerBestPrice

// Правило выбора лучшей пары — одно на загрузку и на живые события сокета.
// Если они разойдутся, выжимка после первого же тика покажет не то, что при
// открытии экрана.

/**
 * У пары есть обе стороны и обе биржи. Каталог отбрасывает неполные строки тем
 * же правилом, в `NormalizeSymbols`.
 */
internal fun TickerBestPrice.isComplete(): Boolean =
    bestAskProviderId != null &&
        bestBidProviderId != null &&
        bestAskPrice.validPriceOrNull() != null &&
        bestBidPrice.validPriceOrNull() != null

/**
 * Самая широкая из лучших пар тикера. Символов у тикера бывает несколько, и
 * смысл экрана — показать, где разница между биржами больше всего.
 */
internal fun List<TickerBestPrice>.widest(): TickerBestPrice? =
    maxByOrNull { it.spreadPercent ?: Double.NEGATIVE_INFINITY }

/** Выжимка сравнения берётся из одной пары целиком, стороны не смешиваются. */
internal fun PairComparison.withBest(best: TickerBestPrice): PairComparison =
    copy(
        bestAskProviderId = best.bestAskProviderId,
        bestAskPrice = best.bestAskPrice,
        bestBidProviderId = best.bestBidProviderId,
        bestBidPrice = best.bestBidPrice,
        spreadPercent = best.spreadPercent,
    )
