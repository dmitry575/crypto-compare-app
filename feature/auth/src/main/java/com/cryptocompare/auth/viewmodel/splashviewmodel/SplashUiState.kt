package com.cryptocompare.auth.viewmodel.splashviewmodel

data class SplashUiState(
    val isCheckAuth: Boolean = true,
    val isAuthenticated: Boolean? = null,
    val errorMessage: String? = null,
)
