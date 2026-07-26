package com.cryptocompare.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.settings.ObserveLanguageUseCase
import com.cryptocompare.model.settings.AppLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Язык нужен всему приложению, поэтому живёт на уровне Activity рядом с темой.
 * Eagerly — чтобы сохранённый язык приехал из DataStore до первой отрисовки.
 */
@HiltViewModel
class LanguageViewModel
    @Inject
    constructor(
        observeLanguageUseCase: ObserveLanguageUseCase,
    ) : ViewModel() {
        val language: StateFlow<AppLanguage> =
            observeLanguageUseCase().stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppLanguage.DEFAULT,
            )
    }
