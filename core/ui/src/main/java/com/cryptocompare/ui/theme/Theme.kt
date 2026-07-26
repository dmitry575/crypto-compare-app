package com.cryptocompare.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Акцент один и тот же в primary и secondary: раньше secondary был фиолетовым,
 * и два соседних ряда чипов на детальном экране красились в разные цвета просто
 * потому, что брали разные роли. Пока роли совпадают, такое не повторится.
 */
internal val CryptoDarkColorScheme =
    darkColorScheme(
        primary = AccentDark,
        onPrimary = OnAccentDark,
        primaryContainer = AccentSoftDark,
        onPrimaryContainer = AccentDark,
        secondary = AccentDark,
        onSecondary = OnAccentDark,
        secondaryContainer = AccentSoftDark,
        onSecondaryContainer = AccentDark,
        tertiary = AccentDark,
        onTertiary = OnAccentDark,
        background = GroundDark,
        onBackground = InkDark,
        surface = SurfaceDark,
        onSurface = InkDark,
        surfaceVariant = SunkDark,
        onSurfaceVariant = Ink2Dark,
        surfaceContainer = SurfaceDark,
        surfaceContainerHigh = SurfaceDark,
        surfaceContainerHighest = SunkDark,
        surfaceContainerLow = GroundDark,
        surfaceContainerLowest = SunkDark,
        inverseSurface = InkDark,
        inverseOnSurface = GroundDark,
        inversePrimary = AccentLight,
        error = DownDark,
        onError = OnAccentDark,
        errorContainer = DownSoftDark,
        onErrorContainer = DownDark,
        outline = LineDark,
        outlineVariant = LineSoftDark,
        scrim = ScrimDark,
        surfaceTint = AccentDark,
    )

internal val CryptoLightColorScheme =
    lightColorScheme(
        primary = AccentLight,
        onPrimary = OnAccentLight,
        primaryContainer = AccentSoftLight,
        onPrimaryContainer = AccentLight,
        secondary = AccentLight,
        onSecondary = OnAccentLight,
        secondaryContainer = AccentSoftLight,
        onSecondaryContainer = AccentLight,
        tertiary = AccentLight,
        onTertiary = OnAccentLight,
        background = GroundLight,
        onBackground = InkLight,
        surface = SurfaceLight,
        onSurface = InkLight,
        surfaceVariant = SunkLight,
        onSurfaceVariant = Ink2Light,
        surfaceContainer = SurfaceLight,
        surfaceContainerHigh = SurfaceLight,
        surfaceContainerHighest = SunkLight,
        surfaceContainerLow = GroundLight,
        surfaceContainerLowest = SunkLight,
        inverseSurface = InkLight,
        inverseOnSurface = GroundLight,
        inversePrimary = AccentDark,
        error = DownLight,
        onError = OnAccentLight,
        errorContainer = DownSoftLight,
        onErrorContainer = DownLight,
        outline = LineLight,
        outlineVariant = LineSoftLight,
        scrim = ScrimLight,
        surfaceTint = AccentLight,
    )

@Composable
fun CryptoCompareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> CryptoDarkColorScheme
            else -> CryptoLightColorScheme
        }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val background = colorScheme.background.toArgb()

            // Фон окна красим цветом текущей схемы, а не полагаемся на
            // windowBackground из темы: тот выбирается по системной ночной теме,
            // а у нас тема своя. Иначе при переключении темы внутри приложения
            // под Compose просвечивал старый фон окна, пока перерисовывался Scaffold.
            window.decorView.setBackgroundColor(background)

            // системные панели сливаются с фоном экрана: отдельного цвета
            // для них нет, иначе внизу появляется полоса чужого оттенка
            window.statusBarColor = background
            window.navigationBarColor = background

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    // LocalIsDarkTheme питает расширения-цвета из ThemeExtensions.kt: без него
    // они читали бы системную настройку в обход параметра darkTheme
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CryptoTypography,
            shapes = CryptoShapes,
            content = content,
        )
    }
}
