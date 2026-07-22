package com.cryptocompare.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.cryptocompare.data.util.DataConstants
import com.cryptocompare.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OnboardingRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : OnboardingRepository {
        private val seenKey = booleanPreferencesKey(DataConstants.Preferences.ONBOARDING_SEEN_KEY)

        override suspend fun hasSeenOnboarding(): Boolean =
            dataStore.data.map { preferences -> preferences[seenKey] ?: false }.first()

        override suspend fun markOnboardingSeen() {
            dataStore.edit { preferences -> preferences[seenKey] = true }
        }
    }
