package com.cryptocompare.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Облегчённая тема для `@Preview`: без обращения к Activity и системным барам,
 * поэтому работает в рендере Android Studio.
 */
@Composable
fun CryptoCompareThemePreview(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CryptoDarkColorScheme else CryptoLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CryptoTypography,
        content = content,
    )
}
