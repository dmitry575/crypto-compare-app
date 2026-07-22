package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.FavouriteTickerRepository
import javax.inject.Inject

class ToggleFavouriteTickerUseCase
    @Inject
    constructor(
        private val favouriteTickerRepository: FavouriteTickerRepository,
    ) {
        suspend operator fun invoke(ticker: String): Result<Boolean> =
            favouriteTickerRepository.toggleFavouriteTicker(ticker)
    }
