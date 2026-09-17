package com.cryptocompare.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.cryptocompare.data.repository.MarketPreferencesRepositoryImpl
import com.cryptocompare.model.chart.ChartIndicator
import com.cryptocompare.model.chart.ChartTimeframe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MarketPreferencesRepositoryImplTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: MarketPreferencesRepositoryImpl

    @Before
    fun setUp() {
        dataStore = InMemoryPreferencesDataStore()
        repository = MarketPreferencesRepositoryImpl(dataStore)
    }

    @Test
    fun `empty storage means the first available exchange and the default timeframe`() =
        runTest {
            val preferences = repository.observeMarketPreferences().first()

            assertNull(preferences.defaultProviderId)
            assertEquals(ChartTimeframe.DEFAULT, preferences.timeframe)
        }

    @Test
    fun `saved exchange and timeframe are read back`() =
        runTest {
            repository.setDefaultProvider(19)
            repository.setDefaultTimeframe(ChartTimeframe.H4)

            val preferences = repository.observeMarketPreferences().first()

            assertEquals(19, preferences.defaultProviderId)
            assertEquals(ChartTimeframe.H4, preferences.timeframe)
        }

    @Test
    fun `clearing the exchange goes back to the first available one`() =
        runTest {
            repository.setDefaultProvider(19)
            repository.setDefaultProvider(null)

            assertNull(repository.observeMarketPreferences().first().defaultProviderId)
        }

    @Test
    fun `chosen indicators are read back and can be cleared`() =
        runTest {
            repository.setChartIndicators(setOf(ChartIndicator.SMA_20, ChartIndicator.EMA_50))

            assertEquals(
                setOf(ChartIndicator.SMA_20, ChartIndicator.EMA_50),
                repository.observeMarketPreferences().first().indicators,
            )

            repository.setChartIndicators(emptySet())

            assertEquals(emptySet<ChartIndicator>(), repository.observeMarketPreferences().first().indicators)
        }

    @Test
    fun `a renamed or broken indicator is dropped, the rest survive`() =
        runTest {
            repository.setChartIndicators(setOf(ChartIndicator.SMA_20))
            dataStore.updateData { preferences ->
                preferences.toMutablePreferences().apply {
                    set(
                        androidx.datastore.preferences.core
                            .stringSetPreferencesKey(
                                com.cryptocompare.data.util.DataConstants.Preferences.CHART_INDICATORS_KEY,
                            ),
                        setOf("SMA_20", "RSI_14"),
                    )
                }
            }

            assertEquals(
                setOf(ChartIndicator.SMA_20),
                repository.observeMarketPreferences().first().indicators,
            )
        }

    @Test
    fun `a renamed or broken timeframe falls back to the default`() =
        runTest {
            // enum могли переименовать между версиями: экран должен открыться,
            // а не упасть на разборе настройки
            repository.setDefaultTimeframe(ChartTimeframe.W1)
            dataStore.updateData { preferences ->
                preferences.toMutablePreferences().apply {
                    set(
                        androidx.datastore.preferences.core
                            .stringPreferencesKey(
                                com.cryptocompare.data.util.DataConstants.Preferences.DEFAULT_TIMEFRAME_KEY,
                            ),
                        "H8",
                    )
                }
            }

            assertEquals(ChartTimeframe.DEFAULT, repository.observeMarketPreferences().first().timeframe)
        }
}
