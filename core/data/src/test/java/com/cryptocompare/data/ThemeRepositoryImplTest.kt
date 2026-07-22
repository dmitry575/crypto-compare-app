package com.cryptocompare.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cryptocompare.data.repository.ThemeRepositoryImpl
import com.cryptocompare.data.util.DataConstants
import com.cryptocompare.model.settings.ThemePreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ThemeRepositoryImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    /**
     * Именно настоящий scope, а не TestScope: у DataStore и runTest были бы
     * разные виртуальные часы, и тест вставал бы намертво.
     */
    private val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: ThemeRepositoryImpl

    @Before
    fun setUp() {
        // настоящий DataStore поверх временного файла: подменять его моком
        // бессмысленно — проверять надо как раз сериализацию значения
        dataStore =
            PreferenceDataStoreFactory.create(scope = dataStoreScope) {
                temporaryFolder.newFile("theme.preferences_pb")
            }
        repository = ThemeRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
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
}
