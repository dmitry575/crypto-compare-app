package com.cryptocompare.domain.usecase.auth

import com.cryptocompare.domain.repository.AuthRepository
import javax.inject.Inject

class SendPasswordResetEmailUseCase
    @Inject
    constructor(
        val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(email: String): Result<Unit> = authRepository.sendPasswordResetEmail(email)
    }
