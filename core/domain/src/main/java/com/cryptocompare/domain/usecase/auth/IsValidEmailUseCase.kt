package com.cryptocompare.domain.usecase.auth

import com.cryptocompare.helpers.util.UseCaseConstants
import javax.inject.Inject

class IsValidEmailUseCase
    @Inject
    constructor() {
        operator fun invoke(email: String): Boolean = email.isNotBlank() && UseCaseConstants.EMAIL_REGEX.matches(email)
    }
