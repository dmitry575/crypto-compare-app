package com.cryptocompare.helpers

import com.cryptocompare.model.settings.ThemePreference

/**
 * Тёмная ли тема при текущем выборе пользователя. [ThemePreference.SYSTEM]
 * отдаёт решение системе, поэтому её состояние приходит аргументом: функция
 * остаётся чистой и проверяется без Compose.
 */
fun ThemePreference.isDark(systemInDarkTheme: Boolean): Boolean =
    when (this) {
        ThemePreference.SYSTEM -> systemInDarkTheme
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
