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

    /** Строки интервалов свечей, которые понимает бэкенд (`GET /v1/klines`). */
    object Klines {
        const val INTERVAL_M15 = "15m"
        const val INTERVAL_H1 = "1h"
        const val INTERVAL_H4 = "4h"
        const val INTERVAL_D1 = "1d"
        const val INTERVAL_W1 = "1w"

        /** Ошибок API нет, если errorCode == 0. */
        const val ERROR_CODE_OK = 0
        const val UNKNOWN_ERROR = "Unknown error"
    }
}
