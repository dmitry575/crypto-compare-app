package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.model.ticker.TickerDetail
import javax.inject.Inject

class GetTickerDetailUseCase
    @Inject
    constructor(
        private val cryptoCompareRepository: CryptoCompareRepository,
    ) {
        suspend operator fun invoke(ticker: String): Result<TickerDetail> =
            runCatching {
                val symbols = cryptoCompareRepository.getSymbolsByTicker(ticker).getOrThrow()
                val providers = cryptoCompareRepository.getProviders().getOrThrow().associateBy { it.id }

                val exchanges =
                    symbols
                        .mapNotNull { symbol ->
                            val provider = providers[symbol.providerId] ?: return@mapNotNull null

                            ProviderDetail(
                                provider = provider,
                                priceSell = symbol.priceSell.takeIf { it > 0 },
                                priceBuy = symbol.priceBuy.takeIf { it > 0 },
                            )
                        }.sortedBy { it.provider.name?.lowercase() }

                TickerDetail(ticker = ticker, exchanges = exchanges)
            }
    }
