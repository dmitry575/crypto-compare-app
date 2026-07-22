package com.cryptocompare.ui.theme

import androidx.compose.ui.graphics.Color

val ElectricCyan = Color(0xFF00D4FF)
val ElectricCyanDark = Color(0xFF00B8E6)
val ElectricCyanLight = Color(0xFF33DDFF)
val CosmicPurple = Color(0xFF6E3AFA)
val CosmicPurpleLight = Color(0xFF8B5CF6)

// Background Colors - Dark Theme
val BgPrimaryDark = Color(0xFF0A0E27)
val BgSecondaryDark = Color(0xFF161B33)
val BgTertiaryDark = Color(0xFF1E2440)
val BgCardDark = Color(0xFF1E2440)

// Background Colors - Light Theme
val BgPrimaryLight = Color(0xFFF5F7FA)
val BgSecondaryLight = Color(0xFFFFFFFF)
val BgTertiaryLight = Color(0xFFE8EBF0)
val BgCardLight = Color(0xFFFFFFFF)

// Surface Colors
val SurfaceDark = Color(0xFF1E2440)
val SurfaceLight = Color(0xFFFFFFFF)

// Input Colors - Dark
val InputBackgroundDark = Color(0xFF1E2440)
val InputBorderDark = Color(0xFF2D3548)
val InputBorderFocusedDark = Color(0xFF00D4FF)

// Input Colors - Light
val InputBackgroundLight = Color(0xFFFFFFFF)
val InputBorderLight = Color(0xFFE0E0E0)
val InputBorderFocusedLight = Color(0xFF00D4FF)

// Text Colors - Dark Theme
val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0xFFE5E7EB)
val TextTertiaryDark = Color(0xFF9CA3AF)
val TextDisabledDark = Color(0xFF6B7280)
val TextPlaceholderDark = Color(0xFF9CA3AF)

// Text Colors - Light Theme
val TextPrimaryLight = Color(0xFF1A1A1A)
val TextSecondaryLight = Color(0xFF4A4A4A)
val TextTertiaryLight = Color(0xFF6B7280)
val TextDisabledLight = Color(0xFF9CA3AF)
val TextPlaceholderLight = Color(0xFF9CA3AF)

// Semantic Colors
val Success = Color(0xFF00FF88)
val SuccessDark = Color(0xFF00CC6E)
val SuccessLight = Color(0xFF10B981)

val Error = Color(0xFFFF3B69)
val ErrorDark = Color(0xFFE63462)
val ErrorLight = Color(0xFFEF4444)

val Warning = Color(0xFFFFB800)
val WarningDark = Color(0xFFE6A800)
val WarningLight = Color(0xFFF59E0B)

val Info = Color(0xFF4D9FFF)
val InfoDark = Color(0xFF3B8FE6)
val InfoLight = Color(0xFF3B82F6)

// Border & Divider Colors
val BorderPrimaryDark = Color(0xFF2D3548)
val BorderSecondaryDark = Color(0xFF1E2440)
val DividerDark = Color(0x1AFFFFFF) // 10% white

val BorderPrimaryLight = Color(0xFFE0E0E0)
val BorderSecondaryLight = Color(0xFFF0F0F0)
val DividerLight = Color(0x1A000000) // 10% black

// Chart Colors
val ChartPositive = Success
val ChartNegative = Error
val ChartGrid = Color(0xFF2D3548)

// Gradient Colors (for composables)
// These will be used in Brush.linearGradient
val GradientPrimaryStart = ElectricCyan
val GradientPrimaryEnd = CosmicPurple

val GradientBackgroundStartDark = Color(0xFF1a1a2e)
val GradientBackgroundMiddleDark = Color(0xFF16213e)
val GradientBackgroundEndDark = Color(0xFF0f3460)

val GradientBackgroundStartLight = Color(0xFFE8F4FF)
val GradientBackgroundMiddleLight = Color(0xFFF5F7FA)
val GradientBackgroundEndLight = Color(0xFFFFFFFF)

// Special Effect Colors
val GlassMorphismOverlay = Color(0x80161B33) // 50% opacity
val ShadowColor = Color(0x4D000000) // 30% black for shadows

// Status Colors
val StatusActive = Success
val StatusInactive = Color(0xFF6B7280)
val StatusPending = Warning

// Icon Tint Colors
val IconPrimaryDark = Color(0xFFFFFFFF)
val IconSecondaryDark = Color(0xFF9CA3AF)

val IconPrimaryLight = Color(0xFF1A1A1A)
val IconSecondaryLight = Color(0xFF6B7280)

// Badge Colors
val BadgeSuccessBg = Color(0x3300FF88) // 20% Success
val BadgeSuccessText = Success

val BadgeErrorBg = Color(0x33FF3B69) // 20% Error
val BadgeErrorText = Error

val BadgeWarningBg = Color(0x33FFB800) // 20% Warning
val BadgeWarningText = Warning

val BadgeInfoBg = Color(0x334D9FFF) // 20% Info
val BadgeInfoText = Info

// Bottom Navigation
val BottomNavBackgroundDark = Color(0xF2161B33) // 95% opacity
val BottomNavBackgroundLight = Color(0xF2FFFFFF) // 95% opacity

val BottomNavSelectedDark = ElectricCyan
val BottomNavUnselectedDark = Color(0x99FFFFFF) // 60% white

val BottomNavSelectedLight = ElectricCyan
val BottomNavUnselectedLight = Color(0x99000000) // 60% black

// Top Bar Colors
val TopBarBackgroundDark = Color(0xF2161B33) // 95% opacity
val TopBarBackgroundLight = Color(0xF2FFFFFF) // 95% opacity

// Card Colors (with variants)
val CardElevatedDark = Color(0xFF1E2440)
val CardElevatedLight = Color(0xFFFFFFFF)

val CardGlassDark = Color(0x80161B33) // 50% opacity
val CardGlassLight = Color(0xCCFFFFFF) // 80% opacity

// Shimmer/Loading Colors
val ShimmerBaseColorDark = Color(0xFF1E2440)
val ShimmerHighlightColorDark = Color(0xFF2D3548)

val ShimmerBaseColorLight = Color(0xFFE8EBF0)
val ShimmerHighlightColorLight = Color(0xFFF5F7FA)

// Overlay Colors
val OverlayDark = Color(0x99000000) // 60% black
val OverlayLight = Color(0x99FFFFFF) // 60% white

// Scrim Colors (for dialogs, bottom sheets)
val ScrimDark = Color(0xCC000000) // 80% black
val ScrimLight = Color(0xCC000000) // 80% black (same for both)
