package com.cryptocompare.domain.usecase.profile

import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.domain.repository.FavouriteSymbolRepository
import javax.inject.Inject

class DeleteAccountUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val favoriteRepository: FavouriteSymbolRepository,
    ) {
        suspend operator fun invoke(): Result<Unit> =
            favoriteRepository.deleteAllFavourites().mapCatching {
                authRepository.deleteAccount().getOrThrow()
            }
    }
