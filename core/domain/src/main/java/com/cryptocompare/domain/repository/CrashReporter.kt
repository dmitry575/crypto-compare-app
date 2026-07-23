package com.cryptocompare.domain.repository

/**
 * Отправка отчётов о сбоях. Абстракция над Crashlytics, а не прямой вызов
 * `FirebaseCrashlytics.getInstance()`: статический синглтон в обход Hilt нельзя
 * подменить в тестах, а весь остальной core:data инжектится. Реализация живёт
 * в core:data, интерфейс — здесь, чтобы им могли пользоваться репозитории,
 * не зная про Firebase.
 */
interface CrashReporter {
    /**
     * Залогировать пойманное исключение как non-fatal. Для сбоев, которые
     * сейчас молча проглатываются (сеть, WebSocket): краша нет, но знать о них
     * полезно.
     */
    fun recordException(throwable: Throwable)

    /** Привязать последующие отчёты к пользователю — по uid, не по email. */
    fun setUser(userId: String)

    /** Отвязать пользователя при выходе. */
    fun clearUser()
}
