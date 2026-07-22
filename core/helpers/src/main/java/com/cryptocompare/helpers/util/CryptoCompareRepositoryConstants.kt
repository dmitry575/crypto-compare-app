package com.cryptocompare.helpers.util

/** Параметры загрузки каталога и истории цен. */
object CryptoCompareRepositoryConstants {
    const val CATALOG_CACHE_TTL_MILLIS = 24 * 60 * 60 * 1000L
    const val SYMBOLS_IN_ROW = 25

    /** Меньше свечей — уже диапазон цен, поэтому тела свечей не вырождаются в нити. */
    const val CANDLE_LIMIT = 40
}
