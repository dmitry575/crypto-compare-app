package com.cryptocompare.data.util

import com.cryptocompare.data.mapper.toAppException
import kotlinx.coroutines.CancellationException

/**
 * `runCatching` для репозиториев: неудача наружу уходит уже разобранной.
 *
 * Заменяет повторявшийся в каждом методе шаблон `runCatching { … }.onFailure {
 * if (it is CancellationException) throw it }` и заодно заворачивает исключение
 * в `AppException`, чтобы выше слоя данных никто не разбирал типы Retrofit,
 * Room и Firebase. Отмену корутины по-прежнему не глотаем: иначе отменённый
 * экран считал бы себя сломанным.
 */
internal inline fun <T> appRunCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error.toAppException())
    }
