package com.cryptocompare.helpers.util

/** Коды закрытия и параметры переподключения веб-сокета. */
object WebSocketConstants {
    /** Тег логов потока котировок: `adb logcat -s TickerSocket`. */
    const val LOG_TAG = "TickerSocket"

    /** Сколько символов сырого сообщения писать в лог при ошибке разбора. */
    const val RAW_LOG_LIMIT = 300

    /**
     * Больше бэкенд не принимает: девятая подписка в соединении возвращает
     * `Subscribe failed` (errorCode 2), причём без указания тикера. Слот
     * освобождается после `unsubscribe`.
     */
    const val MAX_SUBSCRIPTIONS = 8

    const val NORMAL_CLOSURE_STATUS = 1000
    const val BASE_RECONNECT_DELAY_MS = 1_000L
    const val MAX_RECONNECT_DELAY_MS = 30_000L
    const val RECONNECT_JITTER_MS = 500L
    const val MAX_EXPONENT = 5
    const val UNKNOWN_ERROR_CODE = -1
}
