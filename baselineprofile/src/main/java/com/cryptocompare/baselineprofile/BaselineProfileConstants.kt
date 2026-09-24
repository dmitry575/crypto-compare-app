package com.cryptocompare.baselineprofile

/** Что генератор профиля ищет на экране и сколько ждёт. */
object BaselineProfileConstants {
    const val TARGET_PACKAGE = "com.boomhaa.cryptocompare"

    /**
     * Тексты английской локали: генератор идёт на чистом эмуляторе, где язык —
     * системный английский.
     */
    const val ONBOARDING_SKIP = "Skip"
    const val TAB_PORTFOLIO = "Portfolio"
    const val TAB_PAIRS = "Pairs"
    const val TIMEFRAME_H1 = "1H"

    /** Первая страница каталога приходит из сети — ждём с запасом. */
    const val NETWORK_TIMEOUT_MS = 20_000L
    const val SCREEN_TIMEOUT_MS = 5_000L

    /** Пара, у которой всегда есть биржи и график: её экран и попадает в профиль. */
    const val PAIR_QUERY = "btcusdt"
    const val PAIR_NAME = "BTC/USDT"

    /** Отступ жеста от краёв списка: иначе свайп задевает системную навигацию. */
    const val GESTURE_MARGIN_FRACTION = 5

    /** Сколько раз искать список заново, если он пересобрался прямо под жестом. */
    const val GESTURE_ATTEMPTS = 3
}
