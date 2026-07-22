package com.cryptocompare.domain.usecase.pairs

import androidx.paging.PagingData
import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.domain.repository.TickerStreamRepository
import com.cryptocompare.model.symbol.PairUiItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoadPairsUseCase
    @Inject
    constructor(
        private val cryptoCompareRepository: CryptoCompareRepository,
        private val tickerStreamRepository: TickerStreamRepository,
    ) {
        operator fun invoke(
            query: String,
            onlyFavourite: Boolean,
            favouriteTickers: Set<String>,
        ): Flow<PagingData<PairUiItem>> {
            tickerStreamRepository.connect()

            return cryptoCompareRepository.getPairsPaged(
                query = query,
                onlyFavourite = onlyFavourite,
                favouriteTickers = favouriteTickers,
            )
        }
    }
