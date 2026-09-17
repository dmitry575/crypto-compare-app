package com.cryptocompare.auth.viewmodel.splashviewmodel

data class SplashUiState(
    val isPreparing: Boolean = true,
    /**
     * Онбординг ещё не показывали. Вход на запуске больше не проверяется:
     * каталог открыт и гостю, так что единственная развилка старта — показывать
     * ли рассказ о продукте.
     */
    val shouldShowOnboarding: Boolean = false,
)
