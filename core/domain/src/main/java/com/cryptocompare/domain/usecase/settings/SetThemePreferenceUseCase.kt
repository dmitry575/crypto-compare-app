package com.cryptocompare.domain.usecase.settings

import com.cryptocompare.domain.repository.ThemeRepository
import com.cryptocompare.model.settings.ThemePreference
import javax.inject.Inject

class SetThemePreferenceUseCase
    @Inject
    constructor(
        val themeRepository: ThemeRepository,
    ) {
        suspend operator fun invoke(preference: ThemePreference) = themeRepository.setThemePreference(preference)
    }
