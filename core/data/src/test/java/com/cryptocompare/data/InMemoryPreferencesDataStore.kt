package com.cryptocompare.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Честный `DataStore<Preferences>` в памяти: те же `data`/`updateData`, что и у
 * настоящего, но без файла.
 *
 * Проверять в тестах настроек надо логику репозитория — имя enum ↔ строка, откат
 * на значение по умолчанию, — а не файловую сериализацию Google. Файловый
 * DataStore к тому же флейкал на Windows при переименовании `.tmp`.
 */
internal class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state.asStateFlow()

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
