package com.cryptocompare.auth.viewmodel.splashviewmodel

data class SplashUiState(
    val isCheckAuth: Boolean = true,
    val isAuthenticated: Boolean? = null,
    val errorMessage: String? = null,
    /**
     * Онбординг ещё не показывали. Проверяется независимо от [isAuthenticated]:
     * он рассказывает про продукт, а не про вход, поэтому идёт первым в любом
     * случае — и вошедшему, и незнакомому.
     */
    val shouldShowOnboarding: Boolean = false,
)
