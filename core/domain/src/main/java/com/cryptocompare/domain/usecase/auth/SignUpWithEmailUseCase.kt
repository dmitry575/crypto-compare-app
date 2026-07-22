package com.cryptocompare.domain.usecase.auth

import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.model.auth.AuthUser
import javax.inject.Inject

class SignUpWithEmailUseCase
    @Inject
    constructor(
        val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(
            email: String,
            password: String,
        ): Result<AuthUser> = authRepository.signUpWithEmail(email, password)
    }
