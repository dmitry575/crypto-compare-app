package com.cryptocompare.profile.viewmodel.profileviewmodel

import com.cryptocompare.model.auth.AuthUser
import com.cryptocompare.model.provider.Provider
import com.cryptocompare.model.settings.AppLanguage
import com.cryptocompare.model.settings.MarketPreferences
import com.cryptocompare.model.settings.ThemePreference

data class ProfileUiState(
    /** `null` — гость: экран показывает приглашение войти вместо раздела аккаунта. */
    val user: AuthUser? = null,
    val isLoading: Boolean = false,
    val showSignOutConfirmation: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val errorMessage: String? = null,
    val themePreference: ThemePreference = ThemePreference.DEFAULT,
    val marketPreferences: MarketPreferences = MarketPreferences(),
    /** Справочник бирж для выбора площадки по умолчанию. */
    val providers: List<Provider> = emptyList(),
    val showExchangePicker: Boolean = false,
    val language: AppLanguage = AppLanguage.DEFAULT,
)
