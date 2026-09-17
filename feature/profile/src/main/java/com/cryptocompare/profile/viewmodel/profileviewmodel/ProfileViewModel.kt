package com.cryptocompare.profile.viewmodel.profileviewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.auth.ObserveAuthStateUseCase
import com.cryptocompare.domain.usecase.profile.DeleteAccountUseCase
import com.cryptocompare.domain.usecase.profile.SignOutUseCase
import com.cryptocompare.domain.usecase.settings.ObserveLanguageUseCase
import com.cryptocompare.domain.usecase.settings.ObserveThemePreferenceUseCase
import com.cryptocompare.domain.usecase.settings.SetLanguageUseCase
import com.cryptocompare.domain.usecase.settings.SetThemePreferenceUseCase
import com.cryptocompare.helpers.toUserMessage
import com.cryptocompare.model.settings.AppLanguage
import com.cryptocompare.model.settings.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val signOutUseCase: SignOutUseCase,
        private val deleteAccountUseCase: DeleteAccountUseCase,
        private val setThemePreferenceUseCase: SetThemePreferenceUseCase,
        private val setLanguageUseCase: SetLanguageUseCase,
        observeAuthStateUseCase: ObserveAuthStateUseCase,
        observeThemePreferenceUseCase: ObserveThemePreferenceUseCase,
        observeLanguageUseCase: ObserveLanguageUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ProfileUiState())
        val uiState = _uiState.asStateFlow()

        init {
            observeUser(observeAuthStateUseCase)
            observeTheme(observeThemePreferenceUseCase)
            observeLanguage(observeLanguageUseCase)
        }

        fun onThemePreferenceChange(preference: ThemePreference) {
            viewModelScope.launch { setThemePreferenceUseCase(preference) }
        }

        fun onLanguageChange(language: AppLanguage) {
            viewModelScope.launch { setLanguageUseCase(language) }
        }

        fun onSignOutClick() {
            _uiState.update { uiState -> uiState.copy(showSignOutConfirmation = true) }
        }

        fun onSignOutDismissed() {
            _uiState.update { uiState -> uiState.copy(showSignOutConfirmation = false) }
        }

        fun onSignOutConfirmed() {
            viewModelScope.launch {
                _uiState.update { uiState ->
                    uiState.copy(showSignOutConfirmation = false, isLoading = true, errorMessage = null)
                }
                runCatching { signOutUseCase() }
                    .onSuccess { sessionEnded() }
                    .onFailure(::showError)
            }
        }

        fun onDeleteAccountClick() {
            _uiState.update { uiState -> uiState.copy(showDeleteConfirmation = true) }
        }

        fun onDeleteAccountDismissed() {
            _uiState.update { uiState -> uiState.copy(showDeleteConfirmation = false) }
        }

        fun onDeleteAccountConfirmed() {
            viewModelScope.launch {
                _uiState.update { uiState ->
                    uiState.copy(showDeleteConfirmation = false, isLoading = true, errorMessage = null)
                }
                deleteAccountUseCase()
                    .onSuccess { sessionEnded() }
                    .onFailure(::showError)
            }
        }

        fun onErrorShown() {
            _uiState.update { uiState -> uiState.copy(errorMessage = null) }
        }

        private fun observeTheme(observeThemePreferenceUseCase: ObserveThemePreferenceUseCase) {
            viewModelScope.launch {
                observeThemePreferenceUseCase().collect { preference ->
                    _uiState.update { uiState -> uiState.copy(themePreference = preference) }
                }
            }
        }

        private fun observeLanguage(observeLanguageUseCase: ObserveLanguageUseCase) {
            viewModelScope.launch {
                observeLanguageUseCase().collect { language ->
                    _uiState.update { uiState -> uiState.copy(language = language) }
                }
            }
        }

        /**
         * Поток, а не разовое чтение: после выхода и удаления аккаунта экран
         * остаётся открытым и должен сам превратиться в приглашение войти.
         */
        private fun observeUser(observeAuthStateUseCase: ObserveAuthStateUseCase) {
            viewModelScope.launch {
                observeAuthStateUseCase().collect { user ->
                    _uiState.update { uiState -> uiState.copy(user = user) }
                }
            }
        }

        private fun sessionEnded() {
            _uiState.update { uiState -> uiState.copy(isLoading = false) }
        }

        private fun showError(error: Throwable) {
            _uiState.update { uiState ->
                uiState.copy(isLoading = false, errorMessage = error.toUserMessage())
            }
        }
    }
