package com.cryptocompare.domain.repository

import com.cryptocompare.model.settings.ThemePreference
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun observeThemePreference(): Flow<ThemePreference>

    suspend fun setThemePreference(preference: ThemePreference)
}
