package com.cryptocompare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// ============================================
// SEMANTIC COLORS
// ============================================

val ColorScheme.cryptoSuccess: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) Success else SuccessLight

val ColorScheme.cryptoSuccessDark: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) SuccessDark else Success

val ColorScheme.cryptoError: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) Error else ErrorLight

val ColorScheme.cryptoErrorDark: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) ErrorDark else Error

val ColorScheme.cryptoWarning: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) Warning else WarningLight

val ColorScheme.cryptoWarningDark: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) WarningDark else Warning

val ColorScheme.cryptoInfo: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) Info else InfoLight

val ColorScheme.cryptoInfoDark: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) InfoDark else Info

// ============================================
// TEXT COLORS
// ============================================

val ColorScheme.textPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) TextPrimaryDark else TextPrimaryLight

val ColorScheme.textSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) TextSecondaryDark else TextSecondaryLight

val ColorScheme.textTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) TextTertiaryDark else TextTertiaryLight

val ColorScheme.textDisabled: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) TextDisabledDark else TextDisabledLight

val ColorScheme.textPlaceholder: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) TextPlaceholderDark else TextPlaceholderLight

// ============================================
// BACKGROUND COLORS
// ============================================

val ColorScheme.bgPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) BgPrimaryDark else BgPrimaryLight

val ColorScheme.bgSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) BgSecondaryDark else BgSecondaryLight

val ColorScheme.bgTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) BgTertiaryDark else BgTertiaryLight

val ColorScheme.bgCard: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) BgCardDark else BgCardLight

// ============================================
// INPUT COLORS
// ============================================

val ColorScheme.inputBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) InputBackgroundDark else InputBackgroundLight

val ColorScheme.inputBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) InputBorderDark else InputBorderLight

val ColorScheme.inputBorderFocused: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) InputBorderFocusedDark else InputBorderFocusedLight

// ============================================
// BORDER & DIVIDER COLORS
// ============================================

val ColorScheme.borderPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) BorderPrimaryDark else BorderPrimaryLight

val ColorScheme.borderSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) BorderSecondaryDark else BorderSecondaryLight

val ColorScheme.divider: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DividerDark else DividerLight

// ============================================
// CHART COLORS
// ============================================

val ColorScheme.chartPositive: Color
    get() = ChartPositive

val ColorScheme.chartNegative: Color
    get() = ChartNegative

val ColorScheme.chartGrid: Color
    get() = ChartGrid

// ============================================
// CARD COLORS
// ============================================

val ColorScheme.cardElevated: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) CardElevatedDark else CardElevatedLight

val ColorScheme.cardGlass: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) CardGlassDark else CardGlassLight

// ============================================
// BOTTOM NAVIGATION COLORS
// ============================================

val ColorScheme.bottomNavBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) BottomNavBackgroundDark else BottomNavBackgroundLight

val ColorScheme.bottomNavSelected: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) BottomNavSelectedDark else BottomNavSelectedLight

val ColorScheme.bottomNavUnselected: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) BottomNavUnselectedDark else BottomNavUnselectedLight

// ============================================
// TOP BAR COLORS
// ============================================

val ColorScheme.topBarBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) TopBarBackgroundDark else TopBarBackgroundLight

// ============================================
// ICON COLORS
// ============================================

val ColorScheme.iconPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) IconPrimaryDark else IconPrimaryLight

val ColorScheme.iconSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) IconSecondaryDark else IconSecondaryLight

// ============================================
// BADGE COLORS
// ============================================

val ColorScheme.badgeSuccessBg: Color
    get() = BadgeSuccessBg

val ColorScheme.badgeSuccessText: Color
    get() = BadgeSuccessText

val ColorScheme.badgeErrorBg: Color
    get() = BadgeErrorBg

val ColorScheme.badgeErrorText: Color
    get() = BadgeErrorText

val ColorScheme.badgeWarningBg: Color
    get() = BadgeWarningBg

val ColorScheme.badgeWarningText: Color
    get() = BadgeWarningText

val ColorScheme.badgeInfoBg: Color
    get() = BadgeInfoBg

val ColorScheme.badgeInfoText: Color
    get() = BadgeInfoText

// ============================================
// STATUS COLORS
// ============================================

val ColorScheme.statusActive: Color
    get() = StatusActive

val ColorScheme.statusInactive: Color
    get() = StatusInactive

val ColorScheme.statusPending: Color
    get() = StatusPending

// ============================================
// SHIMMER/LOADING COLORS
// ============================================

val ColorScheme.shimmerBase: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) ShimmerBaseColorDark else ShimmerBaseColorLight

val ColorScheme.shimmerHighlight: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) ShimmerHighlightColorDark else ShimmerHighlightColorLight

// ============================================
// OVERLAY & SCRIM COLORS
// ============================================

val ColorScheme.overlay: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) OverlayDark else OverlayLight

val ColorScheme.scrimColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) ScrimDark else ScrimLight

// ============================================
// SPECIAL EFFECT COLORS
// ============================================

val ColorScheme.glassMorphismOverlay: Color
    get() = GlassMorphismOverlay

val ColorScheme.shadowColor: Color
    get() = ShadowColor

// ============================================
// PRIMARY COLORS EXTENSIONS
// ============================================

val ColorScheme.electricCyan: Color
    get() = ElectricCyan

val ColorScheme.electricCyanDark: Color
    get() = ElectricCyanDark

val ColorScheme.electricCyanLight: Color
    get() = ElectricCyanLight

val ColorScheme.cosmicPurple: Color
    get() = CosmicPurple

val ColorScheme.cosmicPurpleLight: Color
    get() = CosmicPurpleLight

// ============================================
// GRADIENT COLORS
// ============================================

object CryptoGradients {
    val primary: List<Color>
        get() = listOf(GradientPrimaryStart, GradientPrimaryEnd)

    val backgroundDark: List<Color>
        get() =
            listOf(
                GradientBackgroundStartDark,
                GradientBackgroundMiddleDark,
                GradientBackgroundEndDark,
            )

    val backgroundLight: List<Color>
        get() =
            listOf(
                GradientBackgroundStartLight,
                GradientBackgroundMiddleLight,
                GradientBackgroundEndLight,
            )

    @Composable
    @ReadOnlyComposable
    fun background(): List<Color> =
        if (isSystemInDarkTheme()) {
            backgroundDark
        } else {
            backgroundLight
        }
}

// ============================================
// HELPER EXTENSIONS
// ============================================

fun ColorScheme.priceChangeColor(isPositive: Boolean): Color = if (isPositive) chartPositive else chartNegative

fun ColorScheme.badgeBackground(type: BadgeType): Color =
    when (type) {
        BadgeType.Success -> badgeSuccessBg
        BadgeType.Error -> badgeErrorBg
        BadgeType.Warning -> badgeWarningBg
        BadgeType.Info -> badgeInfoBg
    }

fun ColorScheme.badgeText(type: BadgeType): Color =
    when (type) {
        BadgeType.Success -> badgeSuccessText
        BadgeType.Error -> badgeErrorText
        BadgeType.Warning -> badgeWarningText
        BadgeType.Info -> badgeInfoText
    }

enum class BadgeType {
    Success,
    Error,
    Warning,
    Info,
}
