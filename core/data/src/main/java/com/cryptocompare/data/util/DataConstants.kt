package com.cryptocompare.data.util

/** Константы слоя данных: миграции и разбор ответов API. */
object DataConstants {
    object Preferences {
        const val FILE_NAME = "settings"
        const val THEME_KEY = "theme_preference"
    }

    object Migrations {
        const val ASSETS_DIR = "migrations"
        const val FILE_NAME_PATTERN = "migration_(\\d+)_(\\d+)\\.sql"
        const val STATEMENT_SEPARATOR = ";"
        const val COMMENT_PREFIX = "--"
    }

    /**
     * Ошибки, которые формулируем мы сами. Всё, что приходит от Firebase,
     * пробрасывается своим типом — текст подбирает `toUserMessage()`.
     */
    object Auth {
        const val NO_CURRENT_USER = "User not authorized"
        const val NULL_USER = "Firebase returned null user"

        /** Пароля нет у аккаунтов, заведённых только через Google. */
        const val NO_PASSWORD_PROVIDER = "Account has no email and password sign-in"
    }

    object History {
        /** Время меньше этого порога считаем секундами, а не миллисекундами. */
        const val MILLIS_THRESHOLD = 1_000_000_000_000L
        const val ERROR_RESPONSE = "Error"
        const val UNKNOWN_ERROR = "Unknown error"
    }
}
