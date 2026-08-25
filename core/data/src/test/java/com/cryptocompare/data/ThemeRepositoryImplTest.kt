package com.cryptocompare.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cryptocompare.data.repository.ThemeRepositoryImpl
import com.cryptocompare.data.util.DataConstants
import com.cryptocompare.model.settings.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ThemeRepositoryImplTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: ThemeRepositoryImpl

    @Before
    fun setUp() {
        // DataStore в памяти, а не поверх файла: проверять надо логику репозитория
        // (имя enum ↔ строка, откат на дефолт), а не файловую сериализацию Google.
        // Файловый DataStore на Windows к тому же флейкал на переименовании .tmp.
        dataStore = InMemoryPreferencesDataStore()
        repository = ThemeRepositoryImpl(dataStore)
    }

    @Test
    fun `empty storage yields the system theme`() =
        runTest {
            assertEquals(ThemePreference.SYSTEM, repository.observeThemePreference().first())
        }

    @Test
    fun `saved preference is read back`() =
        runTest {
            repository.setThemePreference(ThemePreference.DARK)

            assertEquals(ThemePreference.DARK, repository.observeThemePreference().first())
        }

    @Test
    fun `preference can be changed more than once`() =
        runTest {
            repository.setThemePreference(ThemePreference.DARK)
            repository.setThemePreference(ThemePreference.LIGHT)

            assertEquals(ThemePreference.LIGHT, repository.observeThemePreference().first())
        }

    @Test
    fun `every value survives a round trip`() =
        runTest {
            ThemePreference.entries.forEach { preference ->
                repository.setThemePreference(preference)
                assertEquals(preference, repository.observeThemePreference().first())
            }
        }

    @Test
    fun `unknown stored value falls back to the system theme`() =
        runTest {
            // так выглядит база после переименования константы в enum:
            // упасть здесь нельзя, иначе приложение не запустится
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey(DataConstants.Preferences.THEME_KEY)] = "NEON"
            }

            assertEquals(ThemePreference.SYSTEM, repository.observeThemePreference().first())
        }

    /** Честный `DataStore<Preferences>` в памяти: те же `data`/`updateData`, что и у настоящего, но без файла. */
    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())

        override val data: Flow<Preferences> = state.asStateFlow()

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }
}
