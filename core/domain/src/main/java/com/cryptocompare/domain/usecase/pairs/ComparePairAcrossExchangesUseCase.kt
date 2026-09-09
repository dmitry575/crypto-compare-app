package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.helpers.spreadPercent
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
                val best =
                    cryptoCompareRepository
                        .getBestPricesByTicker(ticker)
                        .getOrNull()
                        .orEmpty()
                        .maxByOrNull { it.spreadPercent ?: Double.NEGATIVE_INFINITY }

                val quotes = detail.exchanges.sortedByAsk()

                // при единственной бирже выбирать не из чего: её ask и есть лучший.
                // Это не подмена решения бэкенда, а вырожденный случай — но нужен,
                // потому что по таким парам best-выдача иногда молчит вовсе
                val single = quotes.singleOrNull()

                PairComparison(
                    ticker = ticker,
                    quotes = quotes,
                    bestAskProviderId =
                        best?.bestAskProviderId
                            ?: single?.provider?.id?.takeIf { single.priceSell != null },
                    bestAskPrice = best?.bestAskPrice?.takeIf { it > 0 } ?: single?.priceSell,
                    bestBidProviderId =
                        best?.bestBidProviderId
                            ?: single?.provider?.id?.takeIf { single.priceBuy != null },
                    bestBidPrice = best?.bestBidPrice?.takeIf { it > 0 } ?: single?.priceBuy,
                    spreadPercent =
                        best?.spreadPercent
                            ?: single?.let { spreadPercent(buyPrice = it.priceSell, sellPrice = it.priceBuy) },
                )
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
