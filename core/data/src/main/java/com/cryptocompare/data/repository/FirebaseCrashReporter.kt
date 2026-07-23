package com.cryptocompare.data.repository

import com.cryptocompare.domain.repository.CrashReporter
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject

/**
 * Реализация [CrashReporter] поверх Crashlytics. Тонкая обёртка: своей логики
 * нет, поэтому и теста нет — проверять нечего, кроме самого SDK.
 *
 * FirebaseCrashlytics приходит в конструктор, а не берётся статикой, чтобы
 * граф собирал Hilt, а не сам класс лез в синглтон.
 */
class FirebaseCrashReporter
    @Inject
    constructor(
        private val crashlytics: FirebaseCrashlytics,
    ) : CrashReporter {
        override fun recordException(throwable: Throwable) {
            crashlytics.recordException(throwable)
        }

        override fun setUser(userId: String) {
            crashlytics.setUserId(userId)
        }

        override fun clearUser() {
            crashlytics.setUserId("")
        }
    }
