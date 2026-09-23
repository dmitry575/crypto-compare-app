package com.cryptocompare.helpers.util

/** Фоновые задачи WorkManager: имена и расписание. Политика целиком — `core/data/CACHE_POLICY.md`. */
object WorkerConstants {
    /**
     * Имя осталось от времён, когда каталог обновлялся раз в сутки. Не менять:
     * WorkManager ищет работу по имени, и новое имя завело бы вторую работу,
     * а старая продолжила бы крутиться у всех, кто уже установил приложение.
     */
    const val UNIQUE_WORK_NAME = "refresh_catalog_once_per_day"
    const val UNIQUE_FAVOURITES_WORK_NAME = "sync_favourite_tickers"

    /**
     * Полная перекачка каталога в фоне. Было раз в сутки, 2026-08-26 стало
     * 20 минут: бэкенд перечитывает цены с бирж каждые пару минут, а строки,
     * которых нет на экране, сокет не обновляет, и до этого они стояли сутками.
     */
    const val CATALOG_REFRESH_INTERVAL_MINUTES = 20L

    /** Досылка офлайн-правок избранного в Firestore. 15 минут — минимум WorkManager. */
    const val FAVOURITES_SYNC_INTERVAL_MINUTES = 15L
}
