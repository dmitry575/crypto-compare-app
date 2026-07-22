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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Dark Color Scheme for Crypto Compare
 */
internal val CryptoDarkColorScheme =
    darkColorScheme(
        // Primary colors
        primary = ElectricCyan,
        onPrimary = Color.White,
        primaryContainer = ElectricCyanDark,
        onPrimaryContainer = Color.White,
        // Secondary colors
        secondary = CosmicPurple,
        onSecondary = Color.White,
        secondaryContainer = CosmicPurpleLight,
        onSecondaryContainer = Color.White,
        // Tertiary colors (for accent elements)
        tertiary = Success,
        onTertiary = Color.White,
        tertiaryContainer = SuccessDark,
        onTertiaryContainer = Color.White,
        // Background colors
        background = BgPrimaryDark,
        onBackground = TextPrimaryDark,
        // Surface colors
        surface = BgSecondaryDark,
        onSurface = TextPrimaryDark,
        surfaceVariant = BgTertiaryDark,
        onSurfaceVariant = TextSecondaryDark,
        // Container colors
        surfaceContainer = BgCardDark,
        surfaceContainerHigh = SurfaceDark,
        surfaceContainerHighest = BgTertiaryDark,
        surfaceContainerLow = BgSecondaryDark,
        surfaceContainerLowest = BgPrimaryDark,
        // Inverse colors
        inverseSurface = TextPrimaryDark,
        inverseOnSurface = BgPrimaryDark,
        inversePrimary = ElectricCyanLight,
        // Error colors
        error = Error,
        onError = Color.White,
        errorContainer = ErrorDark,
        onErrorContainer = Color.White,
        // Outline colors
        outline = BorderPrimaryDark,
        outlineVariant = BorderSecondaryDark,
        // Scrim
        scrim = ScrimDark,
        // Surface tint
        surfaceTint = ElectricCyan,
    )

/**
 * Light Color Scheme для Crypto Compare
 */
internal val CryptoLightColorScheme =
    lightColorScheme(
        // Primary colors
        primary = ElectricCyan,
        onPrimary = Color.White,
        primaryContainer = ElectricCyanLight,
        onPrimaryContainer = TextPrimaryLight,
        // Secondary colors
        secondary = CosmicPurple,
        onSecondary = Color.White,
        secondaryContainer = CosmicPurpleLight,
        onSecondaryContainer = TextPrimaryLight,
        // Tertiary colors
        tertiary = SuccessLight,
        onTertiary = Color.White,
        tertiaryContainer = Success,
        onTertiaryContainer = TextPrimaryLight,
        // Background colors
        background = BgPrimaryLight,
        onBackground = TextPrimaryLight,
        // Surface colors
        surface = BgSecondaryLight,
        onSurface = TextPrimaryLight,
        surfaceVariant = BgTertiaryLight,
        onSurfaceVariant = TextSecondaryLight,
        // Container colors
        surfaceContainer = BgCardLight,
        surfaceContainerHigh = SurfaceLight,
        surfaceContainerHighest = BgTertiaryLight,
        surfaceContainerLow = BgSecondaryLight,
        surfaceContainerLowest = BgPrimaryLight,
        // Inverse colors
        inverseSurface = TextPrimaryLight,
        inverseOnSurface = BgPrimaryLight,
        inversePrimary = ElectricCyan,
        // Error colors
        error = ErrorLight,
        onError = Color.White,
        errorContainer = Error,
        onErrorContainer = TextPrimaryLight,
        // Outline colors
        outline = BorderPrimaryLight,
        outlineVariant = BorderSecondaryLight,
        // Scrim
        scrim = ScrimLight,
        // Surface tint
        surfaceTint = ElectricCyan,
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

            window.statusBarColor = colorScheme.background.toArgb()

            window.navigationBarColor =
                if (darkTheme) {
                    BottomNavBackgroundDark.toArgb()
                } else {
                    BottomNavBackgroundLight.toArgb()
                }

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
            content = content,
        )
    }
}
