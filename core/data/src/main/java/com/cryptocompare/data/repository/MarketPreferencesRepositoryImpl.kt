package com.cryptocompare.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.cryptocompare.data.local.preferencesOrEmpty
import com.cryptocompare.data.util.DataConstants
import com.cryptocompare.domain.repository.MarketPreferencesRepository
import com.cryptocompare.model.chart.ChartIndicator
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.model.settings.MarketPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MarketPreferencesRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : MarketPreferencesRepository {
        private val providerKey = intPreferencesKey(DataConstants.Preferences.DEFAULT_PROVIDER_KEY)
        private val timeframeKey = stringPreferencesKey(DataConstants.Preferences.DEFAULT_TIMEFRAME_KEY)
        private val indicatorsKey = stringSetPreferencesKey(DataConstants.Preferences.CHART_INDICATORS_KEY)

        override fun observeMarketPreferences(): Flow<MarketPreferences> =
            dataStore.preferencesOrEmpty().map { preferences ->
                MarketPreferences(
                    defaultProviderId = preferences[providerKey],
                    // хранится имя константы: испорченное значение или переименованный
                    // enum откатывают к масштабу по умолчанию, а не роняют экран
                    timeframe =
                        preferences[timeframeKey]
                            ?.let { stored -> runCatching { ChartTimeframe.valueOf(stored) }.getOrNull() }
                            ?: ChartTimeframe.DEFAULT,
                    indicators =
                        preferences[indicatorsKey]
                            .orEmpty()
                            .mapNotNull { stored -> runCatching { ChartIndicator.valueOf(stored) }.getOrNull() }
                            .toSet(),
                )
            }

        override suspend fun setDefaultProvider(providerId: Int?) {
            dataStore.edit { preferences ->
                if (providerId == null) preferences.remove(providerKey) else preferences[providerKey] = providerId
            }
        }

        override suspend fun setDefaultTimeframe(timeframe: ChartTimeframe) {
            dataStore.edit { preferences -> preferences[timeframeKey] = timeframe.name }
        }

        override suspend fun setChartIndicators(indicators: Set<ChartIndicator>) {
            dataStore.edit { preferences ->
                preferences[indicatorsKey] = indicators.mapTo(mutableSetOf()) { it.name }
            }
        }
    }
