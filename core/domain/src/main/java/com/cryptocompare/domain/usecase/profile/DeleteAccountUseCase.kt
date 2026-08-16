package com.cryptocompare.domain.usecase.profile

import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.domain.repository.FavouriteTickerRepository
import javax.inject.Inject

class DeleteAccountUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val favoriteRepository: FavouriteTickerRepository,
    ) {
        suspend operator fun invoke(): Result<Unit> =
            favoriteRepository.deleteAllFavorites().mapCatching {
                authRepository.deleteAccount().getOrThrow()
            }
    }
