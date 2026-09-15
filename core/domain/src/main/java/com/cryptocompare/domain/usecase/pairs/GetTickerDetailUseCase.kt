package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.helpers.networkNames
import com.cryptocompare.helpers.validPriceOrNull
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.model.ticker.TickerDetail
import javax.inject.Inject

/**
 * Котировки пары по биржам.
 *
 * С [symbolId] — только биржи этого символа, то есть одного набора сетей. Разбивка
 * по тикеру отдаёт все символы разом, и без фильтра таблица смешивала бы USDC из
 * Ethereum с USDC из Solana. Без [symbolId] — все биржи тикера, как раньше.
 */
class GetTickerDetailUseCase
    @Inject
    constructor(
        private val cryptoCompareRepository: CryptoCompareRepository,
    ) {
        suspend operator fun invoke(
            ticker: String,
            symbolId: Long? = null,
        ): Result<TickerDetail> =
            runCatching {
                val symbols =
                    cryptoCompareRepository
                        .getSymbolsByTicker(ticker)
                        .getOrThrow()
                        .filter { symbolId == null || it.id == symbolId }
                val providers = cryptoCompareRepository.getProviders().getOrThrow().associateBy { it.id }

                val exchanges =
                    symbols
                        .mapNotNull { symbol ->
                            val provider = providers[symbol.providerId] ?: return@mapNotNull null

                            ProviderDetail(
                                provider = provider,
                                priceSell = symbol.priceSell.validPriceOrNull(),
                                priceBuy = symbol.priceBuy.validPriceOrNull(),
                                // 24ч-статистика необязательная: биржа может её не отдавать,
                                // а NaN/Infinity из JSON не должны доехать до форматтеров
                                volume24h = symbol.volume24h.sanitizeVolume(),
                                quoteVolume24h = symbol.quoteVolume24h.sanitizeVolume(),
                                change24h = symbol.change24h?.takeIf { it.isFinite() },
                                quotedAtMillis = symbol.quotedAtMillis,
                            )
                        }.sortedBy { it.provider.name?.lowercase() }

                // сеть у символа одна строка на все биржи, поэтому берётся с любой;
                // с нескольких символов (без фильтра) метку не собрать — её и нет
                val networks =
                    symbols
                        .takeIf { symbolId != null }
                        ?.firstOrNull()
                        ?.let { networkNames(it.network, baseAsset = it.symbol?.substringBefore(SYMBOL_DELIMITER)) }
                        .orEmpty()

                TickerDetail(ticker = ticker, exchanges = exchanges, symbolId = symbolId, networks = networks)
            }
    }

/**
 * Отрицательного объёма не бывает, а нулевой биржи отдают вместо «нет статистики»:
 * у `athbtc` при нуле объёма изменение за сутки +16.67%. Оба случая — прочерк.
 */
private fun Double?.sanitizeVolume(): Double? = this?.takeIf { it.isFinite() && it > 0.0 }

private const val SYMBOL_DELIMITER = '/'
