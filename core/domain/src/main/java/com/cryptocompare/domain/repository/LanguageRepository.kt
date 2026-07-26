package com.cryptocompare.domain.repository

import com.cryptocompare.model.settings.AppLanguage
import kotlinx.coroutines.flow.Flow

interface LanguageRepository {
    fun observeLanguage(): Flow<AppLanguage>

    suspend fun setLanguage(language: AppLanguage)
}
