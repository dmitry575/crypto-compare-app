package com.cryptocompare.auth.viewmodel.onboardingviewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.onboarding.MarkOnboardingSeenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val markOnboardingSeenUseCase: MarkOnboardingSeenUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(OnboardingUiState())
        val uiState = _uiState.asStateFlow()

        /**
         * «Пропустить» и «Начать» ведут сюда одинаково: онбординг показан, и
         * повторять его не нужно — пропуск это тоже осознанный выбор.
         *
         * Экран закрывается только после успешной записи флага, иначе при
         * падении DataStore онбординг всплыл бы снова на следующем запуске
         * без объяснений.
         */
        fun onFinish() {
            viewModelScope.launch {
                runCatching { markOnboardingSeenUseCase() }
                _uiState.update { state -> state.copy(isFinished = true) }
            }
        }
    }
