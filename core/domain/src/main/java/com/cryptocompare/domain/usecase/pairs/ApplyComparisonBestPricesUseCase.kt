package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.model.comparison.PairComparison
import com.cryptocompare.model.ticker.TickerBestPrice
import javax.inject.Inject

/**
 * Накладывает на открытое сравнение лучшие пары, пришедшие из сокета.
 *
 * Событие заменяет строку **своего символа**, а в выжимку идёт самая широкая из
 * всех — тем же правилом, что при загрузке. Раньше выжимку заменяло последнее
 * событие, и у тикера с несколькими символами она прыгала между ними на каждом
 * флаше: у `ethusdc` +0.078% сменялось −0.008% просто потому, что другой символ
 * тикнул позже.
 *
 * Неполное событие — без одной из сторон или без биржи — пропускается целиком.
 * Брать из него то, что есть, нельзя: отметка встала бы на биржу из нового
 * события, а цена в выжимке осталась бы от старого.
 */
class ApplyComparisonBestPricesUseCase
    @Inject
    constructor() {
        operator fun invoke(
            comparison: PairComparison,
            updates: List<TickerBestPrice>,
        ): PairComparison {
            val complete = updates.filter { it.isComplete() }
            if (complete.isEmpty()) return comparison

            val bestPrices =
                (comparison.bestPrices.associateBy { it.symbolId } + complete.associateBy { it.symbolId })
                    .values
                    .toList()
            val best = bestPrices.widest() ?: return comparison

            return comparison.withBest(best).copy(bestPrices = bestPrices)
        }
    }
