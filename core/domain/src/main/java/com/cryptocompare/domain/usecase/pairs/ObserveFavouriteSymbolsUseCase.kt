package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.FavouriteSymbolRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFavouriteSymbolsUseCase
    @Inject
    constructor(
        private val favouriteSymbolRepository: FavouriteSymbolRepository,
    ) {
        operator fun invoke(): Flow<Set<Long>> = favouriteSymbolRepository.observeFavouriteSymbolIds()
    }
