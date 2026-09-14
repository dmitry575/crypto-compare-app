package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.helpers.isComplete
import com.cryptocompare.helpers.spreadPercent
import com.cryptocompare.helpers.widest
import com.cryptocompare.model.comparison.PairComparison
import com.cryptocompare.model.provider.ProviderDetail
import javax.inject.Inject

/**
 * Собирает сравнение котировок пары по биржам.
 *
 * Список бирж и выбор лучших цен приходят из **разных** источников, и это
 * намеренно. Разбивка по биржам показывается как есть — пользователь должен
 * видеть весь рынок. А кто из них лучший, решает бэкенд: разбивка приходит без
 * фильтра свежести, и на btcusdt 2026-09-08 биржа с самым высоким бидом стояла
 * два часа. Любой `maxByOrNull` здесь вручил бы победу именно ей.
 *
 * Раньше этот выбор жил прямо в композабле `SpreadBar` и при отсутствующем аске
 * подставлял бид той же биржи. Бид всегда ниже, поэтому биржа без аска
 * систематически выигрывала «где купить» и показывала цену, по которой купить
 * нельзя.
 */
class ComparePairAcrossExchangesUseCase
    @Inject
    constructor(
        private val getTickerDetailUseCase: GetTickerDetailUseCase,
        private val cryptoCompareRepository: CryptoCompareRepository,
    ) {
        suspend operator fun invoke(ticker: String): Result<PairComparison> =
            runCatching {
                val detail = getTickerDetailUseCase(ticker).getOrThrow()

                // если лучшие цены не пришли, экран всё равно покажет таблицу:
                // выжимка сверху сообщит, что данных нет, а не соврёт числом
                val bestPrices =
                    cryptoCompareRepository
                        .getBestPricesByTicker(ticker)
                        .getOrNull()
                        .orEmpty()
                        .filter { it.isComplete() }

                val quotes = detail.exchanges.sortedByAsk()

                // при единственной бирже выбирать не из чего: её ask и есть лучший.
                // Это не подмена решения бэкенда, а вырожденный случай — но нужен,
                // потому что по таким парам best-выдача иногда молчит вовсе
                val single = quotes.singleOrNull()

                val comparison =
                    PairComparison(
                        ticker = ticker,
                        quotes = quotes,
                        bestAskProviderId = single?.provider?.id?.takeIf { single.priceSell != null },
                        bestAskPrice = single?.priceSell,
                        bestBidProviderId = single?.provider?.id?.takeIf { single.priceBuy != null },
                        bestBidPrice = single?.priceBuy,
                        spreadPercent = single?.let { spreadPercent(buyPrice = it.priceSell, sellPrice = it.priceBuy) },
                        bestPrices = bestPrices,
                    )

                bestPrices.widest()?.let(comparison::withBest) ?: comparison
            }
    }

/**
 * От самой дешёвой покупки к дорогой; биржи без аска уходят в конец.
 *
 * Добор по имени обязателен: при равных ценах порядок должен быть устойчивым,
 * иначе список переставляется на каждом тике.
 */
private fun List<ProviderDetail>.sortedByAsk(): List<ProviderDetail> =
    sortedWith(
        compareBy(
            { it.priceSell == null },
            { it.priceSell ?: Double.MAX_VALUE },
            { it.provider.name?.lowercase() ?: "" },
        ),
    )
