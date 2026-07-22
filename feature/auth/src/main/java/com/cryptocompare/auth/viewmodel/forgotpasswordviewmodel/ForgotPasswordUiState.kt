package com.cryptocompare.auth.viewmodel.forgotpasswordviewmodel

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    /** Письмо ушло — форма прячется, остаётся подтверждение. */
    val isEmailSent: Boolean = false,
    val errorMessage: String? = null,
)
