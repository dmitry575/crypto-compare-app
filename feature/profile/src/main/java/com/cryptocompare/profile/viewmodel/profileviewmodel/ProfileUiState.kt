package com.cryptocompare.profile.viewmodel.profileviewmodel

import com.cryptocompare.model.auth.AuthUser
import com.cryptocompare.model.settings.ThemePreference

data class ProfileUiState(
    val user: AuthUser? = null,
    val isLoading: Boolean = false,
    val showSignOutConfirmation: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    /** Сессии больше нет — экран отдаёт навигацию обратно на авторизацию. */
    val isSignedOut: Boolean = false,
    val errorMessage: String? = null,
    val themePreference: ThemePreference = ThemePreference.DEFAULT,
)
