package com.cryptocompare.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cryptocompare.data.util.DataConstants
import com.cryptocompare.domain.repository.ThemeRepository
import com.cryptocompare.model.settings.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ThemeRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ThemeRepository {
        private val themeKey = stringPreferencesKey(DataConstants.Preferences.THEME_KEY)

        override fun observeThemePreference(): Flow<ThemePreference> =
            dataStore.data.map { preferences ->
                // хранится имя константы: если значение испорчено или enum
                // переименовали, откатываемся к системной теме, а не падаем
                preferences[themeKey]
                    ?.let { stored -> runCatching { ThemePreference.valueOf(stored) }.getOrNull() }
                    ?: ThemePreference.DEFAULT
            }

        override suspend fun setThemePreference(preference: ThemePreference) {
            dataStore.edit { preferences -> preferences[themeKey] = preference.name }
        }
    }
