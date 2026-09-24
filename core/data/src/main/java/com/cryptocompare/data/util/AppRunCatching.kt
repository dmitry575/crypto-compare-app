package com.cryptocompare.data.util

import com.cryptocompare.data.mapper.toReportedAppException
import com.cryptocompare.domain.repository.CrashReporter
import kotlinx.coroutines.CancellationException

/**
 * `runCatching` для репозиториев: неудача наружу уходит уже разобранной.
 *
 * Заменяет повторявшийся в каждом методе шаблон `runCatching { … }.onFailure {
 * if (it is CancellationException) throw it }` и заодно заворачивает исключение
 * в `AppException`, чтобы выше слоя данных никто не разбирал типы Retrofit,
 * Room и Firebase. Отмену корутины по-прежнему не глотаем: иначе отменённый
 * экран считал бы себя сломанным.
 *
 * Непредвиденное — ответ бэкенда с ошибкой, сбой базы, неопознанное исключение —
 * уходит в [crashReporter]: пользователь увидит «Что-то пошло не так», и без
 * отчёта у нас не осталось бы и следа. Сеть и вход не отправляются — это не
 * наши сбои, и отчёты утонули бы в них.
 */
internal inline fun <T> appRunCatching(
    crashReporter: CrashReporter,
    block: () -> T,
): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error.toReportedAppException(crashReporter))
    }
