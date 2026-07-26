package com.cryptocompare.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cryptocompare.data.util.DataConstants
import com.cryptocompare.domain.repository.LanguageRepository
import com.cryptocompare.model.settings.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LanguageRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : LanguageRepository {
        private val languageKey = stringPreferencesKey(DataConstants.Preferences.LANGUAGE_KEY)

        override fun observeLanguage(): Flow<AppLanguage> =
            dataStore.data.map { preferences ->
                // хранится имя константы; порча значения или переименование enum
                // не должны ронять приложение — откатываемся к системному языку
                preferences[languageKey]
                    ?.let { stored -> runCatching { AppLanguage.valueOf(stored) }.getOrNull() }
                    ?: AppLanguage.DEFAULT
            }

        override suspend fun setLanguage(language: AppLanguage) {
            dataStore.edit { preferences -> preferences[languageKey] = language.name }
        }
    }
