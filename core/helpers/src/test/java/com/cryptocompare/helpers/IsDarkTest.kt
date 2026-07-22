package com.cryptocompare.helpers

import com.cryptocompare.model.settings.ThemePreference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsDarkTest {
    @Test
    fun `system preference follows the system`() {
        assertTrue(ThemePreference.SYSTEM.isDark(systemInDarkTheme = true))
        assertFalse(ThemePreference.SYSTEM.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `explicit choice overrides the system`() {
        assertFalse(ThemePreference.LIGHT.isDark(systemInDarkTheme = true))
        assertTrue(ThemePreference.DARK.isDark(systemInDarkTheme = false))
    }
}
