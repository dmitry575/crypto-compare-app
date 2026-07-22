package com.cryptocompare.domain.usecase.auth

import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.model.auth.AuthUser
import javax.inject.Inject

class SignInWithGoogleUseCase
    @Inject
    constructor(
        val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(idToken: String): Result<AuthUser> = authRepository.signInWithGoogle(idToken)
    }
