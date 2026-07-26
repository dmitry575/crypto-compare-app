package com.cryptocompare.domain.usecase.settings

import com.cryptocompare.domain.repository.LanguageRepository
import com.cryptocompare.model.settings.AppLanguage
import javax.inject.Inject

class SetLanguageUseCase
    @Inject
    constructor(
        private val repository: LanguageRepository,
    ) {
        suspend operator fun invoke(language: AppLanguage) = repository.setLanguage(language)
    }
