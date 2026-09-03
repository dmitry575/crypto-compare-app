package com.cryptocompare.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/*
 * Семантические цвета поверх [ColorScheme]. Роли Material покрывают не всё:
 * утопленный фон, три уровня текста и направление цены своих слотов не имеют.
 *
 * Каждый цвет обязан иметь вариант под обе темы — токенов без пары здесь быть
 * не должно. Раньше цвета графика и статусов были одним значением на две темы,
 * и в светлой теме сетка выходила почти чёрной, а рост — неразличимым.
 *
 * Тему берём из [LocalIsDarkTheme], а не из isSystemInDarkTheme(): пользователь
 * может выбрать её вручную, и системная настройка тогда ни при чём.
 */

// ============================================
// ФОНЫ
// ============================================

/** Фон экрана. */
val ColorScheme.bgPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) GroundDark else GroundLight

/** Карточки и списки поверх фона. */
val ColorScheme.bgCard: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) SurfaceDark else SurfaceLight

/** Утопленные элементы: поиск, сегменты, плитки статистики. */
val ColorScheme.bgSunk: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) SunkDark else SunkLight

// ============================================
// ТЕКСТ
// ============================================

val ColorScheme.textPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) InkDark else InkLight

val ColorScheme.textSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Ink2Dark else Ink2Light

val ColorScheme.textTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Ink3Dark else Ink3Light

val ColorScheme.textDisabled: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) InkMutedDark else InkMutedLight

// ============================================
// АКЦЕНТ
// ============================================

/** Подложка под акцентным элементом: значок пары, выбранный чип в спокойном виде. */
val ColorScheme.accentSoft: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) AccentSoftDark else AccentSoftLight

// ============================================
// ГРАНИЦЫ
// ============================================

val ColorScheme.borderPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) LineDark else LineLight

val ColorScheme.divider: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) LineSoftDark else LineSoftLight

// ============================================
// ПОЛЯ ВВОДА
// ============================================

val ColorScheme.inputBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) SunkDark else SunkLight

val ColorScheme.inputBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) LineDark else LineLight

val ColorScheme.inputBorderFocused: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) AccentDark else AccentLight

// ============================================
// НАПРАВЛЕНИЕ ЦЕНЫ И СТАТУСЫ
// ============================================

/** Рост, успех, работающая биржа. */
val ColorScheme.cryptoSuccess: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) UpDark else UpLight

/** Падение, ошибка, разрушающее действие. */
val ColorScheme.cryptoError: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DownDark else DownLight

val ColorScheme.cryptoSuccessSoft: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) UpSoftDark else UpSoftLight

val ColorScheme.cryptoErrorSoft: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DownSoftDark else DownSoftLight

/**
 * Цвет изменения цены за период. Знак приходит из
 * `com.cryptocompare.helpers.priceChangeSign()`, чтобы цвет и подпись считались
 * по одному порогу: иначе «0.00%» могло бы оказаться зелёным.
 */
@Composable
@ReadOnlyComposable
fun ColorScheme.priceChangeColor(sign: Int): Color =
    when {
        sign > 0 -> cryptoSuccess
        sign < 0 -> cryptoError
        else -> textSecondary
    }

/** Биржа принимает заявки. */
val ColorScheme.statusActive: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) UpDark else UpLight

/** Биржа выключена или статус неизвестен. */
val ColorScheme.statusInactive: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Ink3Dark else Ink3Light

// ============================================
// ГРАФИК
// ============================================

val ColorScheme.chartPositive: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) UpDark else UpLight

val ColorScheme.chartNegative: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DownDark else DownLight

/** Свеча без движения: открытие совпало с закрытием. */
val ColorScheme.chartNeutral: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Ink3Dark else Ink3Light

// ============================================
// ЗАГРУЗКА
// ============================================

val ColorScheme.shimmerBase: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) ShimmerBaseDark else ShimmerBaseLight

val ColorScheme.shimmerHighlight: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) ShimmerHighlightDark else ShimmerHighlightLight
