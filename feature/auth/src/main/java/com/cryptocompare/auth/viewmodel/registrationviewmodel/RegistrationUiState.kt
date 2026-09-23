package com.cryptocompare.auth.viewmodel.registrationviewmodel

import com.cryptocompare.model.error.AppError

data class RegistrationUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: AppError? = null,
    val passwordLengthMet: Boolean = false,
    val passwordLetterMet: Boolean = false,
    val passwordNumberMet: Boolean = false,
)
