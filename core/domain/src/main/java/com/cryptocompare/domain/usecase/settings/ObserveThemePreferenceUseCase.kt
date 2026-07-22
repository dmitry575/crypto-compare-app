package com.cryptocompare.domain.usecase.settings

import com.cryptocompare.domain.repository.ThemeRepository
import com.cryptocompare.model.settings.ThemePreference
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveThemePreferenceUseCase
    @Inject
    constructor(
        val themeRepository: ThemeRepository,
    ) {
        operator fun invoke(): Flow<ThemePreference> = themeRepository.observeThemePreference()
    }
