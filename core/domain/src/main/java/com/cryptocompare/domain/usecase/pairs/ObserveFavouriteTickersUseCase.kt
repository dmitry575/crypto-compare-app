package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.FavouriteTickerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFavouriteTickersUseCase
    @Inject
    constructor(
        private val favouriteTickerRepository: FavouriteTickerRepository,
    ) {
        operator fun invoke(): Flow<Set<String>> = favouriteTickerRepository.observeFavouriteTickers()
    }
