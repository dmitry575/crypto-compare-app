package com.cryptocompare.domain.usecase.profile

import com.cryptocompare.domain.repository.AuthRepository
import javax.inject.Inject

class ChangePasswordUseCase
    @Inject
    constructor(
        val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(
            currentPassword: String,
            newPassword: String,
        ): Result<Unit> = authRepository.changePassword(currentPassword, newPassword)
    }
