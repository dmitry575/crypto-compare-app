package com.cryptocompare.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cryptocompare.data.repository.ThemeRepositoryImpl
import com.cryptocompare.data.util.DataConstants
import com.cryptocompare.model.settings.ThemePreference
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
        dataStore = InMemoryPreferencesDataStore()
        repository = ThemeRepositoryImpl(dataStore)
    }

    @Test
    fun `empty storage yields the system theme`() =
        runTest {
            assertEquals(ThemePreference.SYSTEM, repository.observeThemePreference().first())
        }

    @Test
    fun `an unreadable preferences file falls back to the default instead of crashing`() =
        runTest {
            // DataStore бросает IOException прямо в коллектор, а тему собирают
            // в viewModelScope без своего catch: раньше это было падение на старте
            val repository = ThemeRepositoryImpl(BrokenPreferencesDataStore())

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
}
