package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.FavouriteTickerRepository
import javax.inject.Inject

class SyncFavouriteTickersUseCase
    @Inject
    constructor(
        private val favouriteTickerRepository: FavouriteTickerRepository,
    ) {
        suspend operator fun invoke(): Result<Unit> = favouriteTickerRepository.syncFavouriteTickers()
    }
