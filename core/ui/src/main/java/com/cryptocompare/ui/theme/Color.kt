package com.cryptocompare.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Сырая палитра. Наружу цвета отдаются только через семантические расширения
 * в [ThemeExtensions] — напрямую эти значения в экранах не используются.
 *
 * Акцент один. Зелёный и красный заняты направлением цены и больше ни для чего
 * не применяются, поэтому акцент уведён от них по тону: иначе «дороже/дешевле»
 * перестаёт читаться с одного взгляда.
 *
 * Нейтрали с холодным зеленоватым уклоном в сторону акцента — чистый серый
 * выглядит не выбранным, а доставшимся по умолчанию.
 */

// Ground — фон экрана
internal val GroundLight = Color(0xFFF2F5F4)
internal val GroundDark = Color(0xFF0E1417)

// Surface — карточки и списки поверх фона
internal val SurfaceLight = Color(0xFFFFFFFF)
internal val SurfaceDark = Color(0xFF161F22)

// Sunk — утопленные элементы: поле поиска, плитки статистики, сегменты
internal val SunkLight = Color(0xFFE7ECEA)
internal val SunkDark = Color(0xFF0A1013)

// Линии
internal val LineLight = Color(0xFFD5DEDB)
internal val LineDark = Color(0xFF263438)
internal val LineSoftLight = Color(0xFFE3EAE8)
internal val LineSoftDark = Color(0xFF223034)

// Текст: три уровня и отключённое состояние
internal val InkLight = Color(0xFF10191B)
internal val InkDark = Color(0xFFEDF3F2)
internal val Ink2Light = Color(0xFF4A5A5C)
internal val Ink2Dark = Color(0xFF9FB2B3)
internal val Ink3Light = Color(0xFF7C8D8E)
internal val Ink3Dark = Color(0xFF6B7E80)
internal val InkMutedLight = Color(0xFFA3B0B0)
internal val InkMutedDark = Color(0xFF4C5C5E)

// Акцент
internal val AccentLight = Color(0xFF00857C)
internal val AccentDark = Color(0xFF2ED3C6)
internal val AccentSoftLight = Color(0xFFDCF0EE)
internal val AccentSoftDark = Color(0xFF10312F)
internal val OnAccentLight = Color(0xFFFFFFFF)
internal val OnAccentDark = Color(0xFF04211F)

// Направление цены
internal val UpLight = Color(0xFF17875A)
internal val UpDark = Color(0xFF34C98B)
internal val DownLight = Color(0xFFC0384E)
internal val DownDark = Color(0xFFFF6B82)
internal val UpSoftLight = Color(0xFFDBEFE5)
internal val UpSoftDark = Color(0xFF0F2C22)
internal val DownSoftLight = Color(0xFFF8E0E4)
internal val DownSoftDark = Color(0xFF331A20)

// Загрузка
internal val ShimmerBaseLight = Color(0xFFE7ECEA)
internal val ShimmerBaseDark = Color(0xFF1B262A)
internal val ShimmerHighlightLight = Color(0xFFF5F8F7)
internal val ShimmerHighlightDark = Color(0xFF26343A)

// Затемнение под диалогами
internal val ScrimLight = Color(0x99101919)
internal val ScrimDark = Color(0xCC05090B)
