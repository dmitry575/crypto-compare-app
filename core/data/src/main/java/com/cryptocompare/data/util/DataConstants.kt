package com.cryptocompare.data.util

/** Константы слоя данных: миграции и разбор ответов API. */
object DataConstants {
    object Preferences {
        const val FILE_NAME = "settings"
        const val THEME_KEY = "theme_preference"
        const val LANGUAGE_KEY = "app_language"
        const val ONBOARDING_SEEN_KEY = "onboarding_seen"
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

    object Favourites {
        /** syncPendingFavouriteOperations() кидает это, если очередь не опустела за MAX_SYNC_PASSES проходов. */
        const val SYNC_INCOMPLETE = "Pending favourite operations did not settle before sync"

        const val BATCH_CHUNK_SIZE = 500

        const val MAX_SYNC_PASSES = 5
    }

    object History {
        /** Время меньше этого порога считаем секундами, а не миллисекундами. */
        const val MILLIS_THRESHOLD = 1_000_000_000_000L
        const val ERROR_RESPONSE = "Error"
        const val UNKNOWN_ERROR = "Unknown error"
    }

    /**
     * Сколько свечей просим для каждого масштаба. Одной константой на все масштабы
     * не обойтись: 365 свечей — это год на дневном ряде, но четверо суток на 15m.
     * Потолок min-api — 2000 точек за запрос.
     */
    object HistoryDepth {
        /** Двое суток. */
        const val M15 = 192

        /** Неделя. */
        const val H1 = 168

        /** Месяц. */
        const val H4 = 180

        /** Год. */
        const val D1 = 365

        /** Пять лет. */
        const val W1 = 260
    }
}
