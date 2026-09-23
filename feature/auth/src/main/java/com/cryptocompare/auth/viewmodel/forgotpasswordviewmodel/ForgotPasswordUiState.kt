package com.cryptocompare.auth.viewmodel.forgotpasswordviewmodel

import com.cryptocompare.model.error.AppError

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    /** Письмо ушло — форма прячется, остаётся подтверждение. */
    val isEmailSent: Boolean = false,
    val error: AppError? = null,
)
