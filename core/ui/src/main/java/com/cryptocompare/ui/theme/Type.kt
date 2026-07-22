package com.cryptocompare.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Шкала из пяти размеров (28 / 20 / 16 / 14 / 12) и трёх начертаний.
 * Роли Material переиспользуют эти значения — новых размеров между ними нет.
 *
 * Раньше здесь была полная шкала Material плюс двенадцать своих объектов,
 * из которых не использовался ни один, а размер 16sp жил под тремя именами.
 *
 * Своего шрифта в APK нет: современность даёт шкала, интерлиньяж и табличные
 * цифры, а не гарнитура.
 */

private val Sans = FontFamily.Default

/** Всё, что число, набирается этим: разряды обязаны вставать в столбик. */
private val Numeric = FontFamily.Monospace

/** Цифры одной ширины — иначе цена «прыгает» при обновлении по WebSocket. */
private const val TABULAR_FIGURES = "tnum"

val CryptoTypography =
    Typography(
        // 28 — цена-герой на детальном экране
        displayLarge =
            TextStyle(
                fontFamily = Numeric,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.5).sp,
                fontFeatureSettings = TABULAR_FIGURES,
            ),
        displayMedium =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.4).sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.2).sp,
            ),
        // 20 — заголовки экранов
        headlineLarge =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.2).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.2).sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.2).sp,
            ),
        // 16 — крупные строки списков, названия
        titleLarge =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.1).sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.1).sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
            ),
        // 14 — основной текст
        bodyLarge =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp,
            ),
        // 14 / 12 — кнопки, чипы, подписи
        labelLarge =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = Sans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.6.sp,
            ),
    )

/**
 * Числовые стили. Отдельный набор, потому что цены живут по своим правилам:
 * моноширинный шрифт и табличные цифры, иначе колонка цен не выравнивается,
 * а цена дёргается на каждом тике.
 */
object NumericType {
    /** Цена-герой. */
    val Hero: TextStyle = CryptoTypography.displayLarge

    /** Цены в карточках и в полосе разброса. */
    val Medium: TextStyle =
        TextStyle(
            fontFamily = Numeric,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = (-0.3).sp,
            fontFeatureSettings = TABULAR_FIGURES,
        )

    /** Цены в строках списка. */
    val Small: TextStyle =
        TextStyle(
            fontFamily = Numeric,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = (-0.3).sp,
            fontFeatureSettings = TABULAR_FIGURES,
        )

    /** Проценты в плашках, подписи осей. */
    val Caption: TextStyle =
        TextStyle(
            fontFamily = Numeric,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.sp,
            fontFeatureSettings = TABULAR_FIGURES,
        )
}

/** Надзаголовок группы: «АККАУНТ», «РАЗБРОС МЕЖДУ БИРЖАМИ». */
val OverlineType: TextStyle =
    TextStyle(
        fontFamily = Numeric,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.0.sp,
    )
