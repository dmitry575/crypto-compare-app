package com.cryptocompare.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Облегчённая тема для `@Preview`: без обращения к Activity и системным барам,
 * поэтому работает в рендере Android Studio.
 *
 * [LocalIsDarkTheme] обязателен: расширения в `ThemeExtensions.kt` читают именно
 * его, а по умолчанию он `false`. Без этой строки превью с `darkTheme = true`
 * рисовало тёмную схему Material поверх светлых семантических цветов — то есть
 * показывало то, чего в приложении не бывает.
 *
 * [CryptoShapes] по той же причине: без них превью брало скругления Material,
 * а не проектные, и углы карточек в превью и на устройстве расходились.
 */
@Composable
fun CryptoCompareThemePreview(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CryptoDarkColorScheme else CryptoLightColorScheme

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CryptoTypography,
            shapes = CryptoShapes,
            content = content,
        )
    }
}
