package com.cryptocompare.auth.util

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
}
