package com.cryptocompare.auth.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Константы фичи auth. */
object AuthConstants {
    /**
     * Тексты ошибок ViewModel. Соседние экраны пока держат их прямо в коде —
     * при общей чистке feature:auth эти строки переедут в `strings.xml`.
     */
    object Errors {
        const val INVALID_EMAIL = "Incorrect email was entered"
    }

    object ForgotPassword {
        /** Подзаголовок приглушён относительно основного текста. */
        const val SUBTITLE_ALPHA = 0.7f
    }

    /** Пропорции знака: два столбика и разрыв укладываются в квадрат. */
    object Logo {
        const val BAR_WIDTH_RATIO = 0.34f
        const val GAP_RATIO = 0.32f
        const val SHORT_BAR_RATIO = 0.62f
    }

    /** Онбординг: индикатор страниц и данные иллюстраций. */
    object Onboarding {
        val dotSize: Dp = 8.dp
        val dotWidthActive: Dp = 24.dp

        /** Общая высота блока с иллюстрацией: заголовки не должны прыгать. */
        val artHeight: Dp = 180.dp
        val artNameWidth: Dp = 44.dp
        val artNameHeight: Dp = 8.dp
        val artCandlesHeight: Dp = 120.dp

        /**
         * Цена одной пары на трёх биржах. Общее начало и расходящийся хвост
         * разделены заранее: приглушая первое, показываем разницу типографикой.
         */
        val SAMPLE_QUOTES =
            listOf(
                "65 9" to "05.32",
                "66 0" to "18.40",
                "66 0" to "31.12",
            )

        const val SAMPLE_LOW = "65 905.32"
        const val SAMPLE_HIGH = "66 031.12"
        const val SAMPLE_SPREAD = "0.19%"

        /** Свеча: верх и низ тела в долях высоты плюс направление. */
        val SAMPLE_CANDLES =
            listOf(
                Triple(0.62f, 0.80f, false),
                Triple(0.52f, 0.68f, true),
                Triple(0.40f, 0.56f, true),
                Triple(0.46f, 0.62f, false),
                Triple(0.30f, 0.48f, true),
                Triple(0.34f, 0.44f, false),
                Triple(0.16f, 0.36f, true),
                Triple(0.22f, 0.32f, true),
            )

        const val CANDLE_BODY_RATIO = 0.46f
        const val CANDLE_WICK_WIDTH = 2f
        const val CANDLE_WICK_OVERHANG = 0.05f
    }
}
