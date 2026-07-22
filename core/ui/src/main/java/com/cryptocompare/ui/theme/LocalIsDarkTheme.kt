package com.cryptocompare.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Тёмная ли сейчас тема. Значение выставляет [CryptoCompareTheme], а расширения
 * в `ThemeExtensions.kt` читают именно его, а не `isSystemInDarkTheme()`.
 *
 * Иначе ручной выбор темы перекрашивал бы только `MaterialTheme.colorScheme`,
 * а весь кастомный UI оставался бы на системной настройке.
 *
 * `static`, потому что смена темы — редкое событие, требующее перерисовки
 * всего дерева целиком.
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }
