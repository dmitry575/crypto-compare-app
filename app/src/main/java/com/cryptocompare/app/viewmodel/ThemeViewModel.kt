package com.cryptocompare.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.settings.ObserveThemePreferenceUseCase
import com.cryptocompare.model.settings.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Тема нужна всему приложению, поэтому живёт на уровне Activity, а не экрана.
 * UiState-обёртка тут не заводится: состояние — одно значение перечисления.
 */
@HiltViewModel
class ThemeViewModel
    @Inject
    constructor(
        observeThemePreferenceUseCase: ObserveThemePreferenceUseCase,
    ) : ViewModel() {
        // Eagerly, чтобы чтение из DataStore стартовало до первой отрисовки
        // и сохранённая тема успела приехать без заметного мигания
        val themePreference: StateFlow<ThemePreference> =
            observeThemePreferenceUseCase().stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = ThemePreference.DEFAULT,
            )
    }
