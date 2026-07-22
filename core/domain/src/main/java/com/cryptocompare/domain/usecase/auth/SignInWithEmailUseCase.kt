package com.cryptocompare.domain.usecase.auth

import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.model.auth.AuthUser
import javax.inject.Inject

class SignInWithEmailUseCase
    @Inject
    constructor(
        val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(
            email: String,
            password: String,
        ): Result<AuthUser> = authRepository.signInWithEmail(email, password)
    }
