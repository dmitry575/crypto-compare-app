package com.cryptocompare.profile.viewmodel.profileviewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.auth.ObserveAuthStateUseCase
import com.cryptocompare.domain.usecase.pairs.GetProvidersUseCase
import com.cryptocompare.domain.usecase.profile.DeleteAccountUseCase
import com.cryptocompare.domain.usecase.profile.SignOutUseCase
import com.cryptocompare.domain.usecase.settings.ObserveLanguageUseCase
import com.cryptocompare.domain.usecase.settings.ObserveMarketPreferencesUseCase
import com.cryptocompare.domain.usecase.settings.ObserveThemePreferenceUseCase
import com.cryptocompare.domain.usecase.settings.SetDefaultProviderUseCase
import com.cryptocompare.domain.usecase.settings.SetDefaultTimeframeUseCase
import com.cryptocompare.domain.usecase.settings.SetLanguageUseCase
import com.cryptocompare.domain.usecase.settings.SetThemePreferenceUseCase
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.model.error.asAppError
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
        private val setDefaultProviderUseCase: SetDefaultProviderUseCase,
        private val setDefaultTimeframeUseCase: SetDefaultTimeframeUseCase,
        private val getProvidersUseCase: GetProvidersUseCase,
        observeAuthStateUseCase: ObserveAuthStateUseCase,
        observeThemePreferenceUseCase: ObserveThemePreferenceUseCase,
        observeLanguageUseCase: ObserveLanguageUseCase,
        observeMarketPreferencesUseCase: ObserveMarketPreferencesUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ProfileUiState())
        val uiState = _uiState.asStateFlow()

        init {
            observeUser(observeAuthStateUseCase)
            observeTheme(observeThemePreferenceUseCase)
            observeLanguage(observeLanguageUseCase)
            observeMarketPreferences(observeMarketPreferencesUseCase)
            loadProviders()
        }

        fun onThemePreferenceChange(preference: ThemePreference) {
            viewModelScope.launch { setThemePreferenceUseCase(preference) }
        }

        fun onDefaultTimeframeChange(timeframe: ChartTimeframe) {
            viewModelScope.launch { setDefaultTimeframeUseCase(timeframe) }
        }

        fun onDefaultExchangeClick() {
            // справочник бирж мог не приехать при открытии экрана — пробуем ещё раз,
            // раз он понадобился прямо сейчас
            if (_uiState.value.providers.isEmpty()) loadProviders()

            _uiState.update { uiState -> uiState.copy(showExchangePicker = true) }
        }

        fun onDefaultExchangeDismissed() {
            _uiState.update { uiState -> uiState.copy(showExchangePicker = false) }
        }

        fun onDefaultExchangeSelected(providerId: Int?) {
            _uiState.update { uiState -> uiState.copy(showExchangePicker = false) }
            viewModelScope.launch { setDefaultProviderUseCase(providerId) }
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
                    uiState.copy(showSignOutConfirmation = false, isLoading = true, error = null)
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
                    uiState.copy(showDeleteConfirmation = false, isLoading = true, error = null)
                }
                deleteAccountUseCase()
                    .onSuccess { sessionEnded() }
                    .onFailure(::showError)
            }
        }

        fun onErrorShown() {
            _uiState.update { uiState -> uiState.copy(error = null) }
        }

        private fun observeTheme(observeThemePreferenceUseCase: ObserveThemePreferenceUseCase) {
            viewModelScope.launch {
                observeThemePreferenceUseCase().collect { preference ->
                    _uiState.update { uiState -> uiState.copy(themePreference = preference) }
                }
            }
        }

        private fun observeMarketPreferences(observeMarketPreferencesUseCase: ObserveMarketPreferencesUseCase) {
            viewModelScope.launch {
                observeMarketPreferencesUseCase().collect { preferences ->
                    _uiState.update { uiState -> uiState.copy(marketPreferences = preferences) }
                }
            }
        }

        /** Список бирж нужен только для выбора площадки: ошибку не показываем, шторка просто останется с «Первой доступной». */
        private fun loadProviders() {
            viewModelScope.launch {
                getProvidersUseCase().onSuccess { providers ->
                    _uiState.update { uiState -> uiState.copy(providers = providers) }
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
                uiState.copy(isLoading = false, error = error.asAppError())
            }
        }
    }
