package com.cryptocompare.auth.viewmodel.loginviewmodel

import com.cryptocompare.model.error.AppError

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: AppError? = null,
)
