package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.FavouriteSymbolRepository
import javax.inject.Inject

class ToggleFavouriteSymbolUseCase
    @Inject
    constructor(
        private val favouriteSymbolRepository: FavouriteSymbolRepository,
    ) {
        suspend operator fun invoke(
            symbolId: Long,
            ticker: String,
        ): Result<Boolean> = favouriteSymbolRepository.toggleFavouriteSymbol(symbolId, ticker)
    }
