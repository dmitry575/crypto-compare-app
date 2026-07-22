package com.cryptocompare.auth.viewmodel.splashviewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.auth.GetCurrentUserUseCase
import com.cryptocompare.domain.usecase.onboarding.HasSeenOnboardingUseCase
import com.cryptocompare.helpers.toUserMessage
import com.cryptocompare.helpers.util.AppConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel
    @Inject
    constructor(
        private val getCurrentUserUseCase: GetCurrentUserUseCase,
        private val hasSeenOnboardingUseCase: HasSeenOnboardingUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SplashUiState())
        val uiState = _uiState.asStateFlow()

        init {
            checkAuthentification()
        }

        fun checkAuthentification() {
            viewModelScope.launch {
                _uiState.update { uiState -> uiState.copy(isCheckAuth = true, errorMessage = null) }
                delay(AppConstants.SPLASH_DURATION_MS)

                // сбой чтения флага не должен ронять запуск: показать онбординг
                // второй раз не страшно, а застрять на сплеше — страшно
                val showOnboarding = runCatching { !hasSeenOnboardingUseCase() }.getOrDefault(false)

                runCatching { getCurrentUserUseCase() }
                    .onSuccess { user ->
                        _uiState.update { uiState ->
                            uiState.copy(
                                isCheckAuth = false,
                                isAuthenticated = user != null,
                                shouldShowOnboarding = showOnboarding,
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update { uiState ->
                            uiState.copy(
                                isCheckAuth = false,
                                isAuthenticated = false,
                                errorMessage = error.toUserMessage(),
                                shouldShowOnboarding = showOnboarding,
                            )
                        }
                    }
            }
        }
    }
