package com.cryptocompare.domain.usecase.auth

import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.model.auth.AuthUser
import javax.inject.Inject

class GetCurrentUserUseCase
    @Inject
    constructor(
        val authRepository: AuthRepository,
    ) {
        operator fun invoke(): AuthUser? = authRepository.currentUser
    }
