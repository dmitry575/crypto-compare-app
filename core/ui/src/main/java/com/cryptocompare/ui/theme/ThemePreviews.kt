package com.cryptocompare.ui.theme

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Светлая и тёмная тема одной аннотацией.
 *
 * Работает в паре с `CryptoCompareThemePreview(darkTheme = isSystemInDarkTheme())`:
 * тему переключает `uiMode`, а не параметр в каждой превью-функции. Иначе на
 * каждый компонент пришлось бы писать две почти одинаковые функции, и вторую
 * регулярно забывали бы.
 */
@Preview(name = "Светлая", group = "themes")
@Preview(name = "Тёмная", group = "themes", uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ThemePreviews
