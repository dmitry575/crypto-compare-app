package com.cryptocompare.domain.usecase.profile

import com.cryptocompare.domain.repository.AuthRepository
import javax.inject.Inject

class DeleteAccountUseCase
    @Inject
    constructor(
        val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(): Result<Unit> = authRepository.deleteAccount()
    }
