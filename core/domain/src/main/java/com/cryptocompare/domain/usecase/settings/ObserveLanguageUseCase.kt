package com.cryptocompare.domain.usecase.settings

import com.cryptocompare.domain.repository.LanguageRepository
import com.cryptocompare.model.settings.AppLanguage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLanguageUseCase
    @Inject
    constructor(
        private val repository: LanguageRepository,
    ) {
        operator fun invoke(): Flow<AppLanguage> = repository.observeLanguage()
    }
