package com.cryptocompare.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Размеры по сетке 4dp. Здесь только то, что действительно используется —
 * группы вроде Elevation, Grid, ZIndex и AnimationDuration были объявлены,
 * но ни разу не вызваны, и удалены.
 *
 * Горизонтальные поля везде одни: [Padding.screenHorizontal] совпадает с
 * [Padding.listItemHorizontal]. Раньше экран отступал на 24dp, а строка списка
 * на 16dp, и левый край содержимого гулял между заголовком и строками.
 */
object Dimensions {
    object Spacing {
        val xxs: Dp = 4.dp
        val xs: Dp = 8.dp
        val sm: Dp = 12.dp
        val md: Dp = 16.dp
        val lg: Dp = 24.dp
        val xl: Dp = 32.dp
        val xxl: Dp = 48.dp
    }

    object Padding {
        /** Единое горизонтальное поле экрана и строк списка. */
        val screenHorizontal: Dp = 16.dp
        val screenVertical: Dp = 16.dp
        val screen: Dp = 16.dp

        val listItemHorizontal: Dp = 16.dp
        val listItemVertical: Dp = 12.dp

        val cardSmall: Dp = 12.dp
        val cardMedium: Dp = 16.dp
        val cardLarge: Dp = 20.dp

        val inputHorizontal: Dp = 16.dp
        val inputVertical: Dp = 16.dp

        val chipHorizontal: Dp = 12.dp
        val chipVertical: Dp = 8.dp
    }

    object Radius {
        val sm: Dp = 8.dp
        val md: Dp = 12.dp
        val lg: Dp = 16.dp
        val xl: Dp = 20.dp
        val full: Dp = 999.dp

        val button: Dp = 14.dp
        val input: Dp = 12.dp
        val card: Dp = 16.dp
    }

    object Height {
        val button: Dp = 52.dp
        val input: Dp = 52.dp

        /** Фиксированная высота строки каталога: от неё считается число подписок. */
        val listItemSmall: Dp = 64.dp
        val listItem: Dp = 72.dp

        /**
         * Строка каталога с 24ч-статистикой: по две строки текста слева и справа.
         * На шаг сетки больше [listItem] — впритык к 72dp контент обрезался бы
         * при увеличенном системном шрифте.
         */
        val listItemStats: Dp = 80.dp

        val chip: Dp = 34.dp
        val divider: Dp = 1.dp
    }

    object IconSize {
        val sm: Dp = 16.dp
        val md: Dp = 24.dp
        val lg: Dp = 32.dp
        val xxl: Dp = 64.dp

        val chipDot: Dp = 6.dp
    }

    object Avatar {
        val sm: Dp = 32.dp
        val md: Dp = 40.dp
        val lg: Dp = 56.dp
        val xl: Dp = 72.dp
    }

    object Border {
        val thin: Dp = 1.dp
        val card: Dp = 1.dp
        val input: Dp = 1.dp
        val inputFocused: Dp = 2.dp
    }

    object Gap {
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 12.dp
        val lg: Dp = 16.dp
        val xl: Dp = 24.dp

        val formFields: Dp = 16.dp
    }

    object TouchTarget {
        val min: Dp = 48.dp
    }

    object Crypto {
        /** Квадратный значок пары в строке списка. */
        val pairBadge: Dp = 36.dp

        val chartLarge: Dp = 300.dp

        /** Толщина полосы разброса. */
        val spreadTrack: Dp = 6.dp
    }
}
