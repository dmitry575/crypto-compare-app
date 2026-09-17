package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.FavouriteSymbolRepository
import javax.inject.Inject

class SyncFavouriteSymbolsUseCase
    @Inject
    constructor(
        private val favouriteSymbolRepository: FavouriteSymbolRepository,
    ) {
        suspend operator fun invoke(): Result<Unit> = favouriteSymbolRepository.syncFavouriteSymbols()
    }
