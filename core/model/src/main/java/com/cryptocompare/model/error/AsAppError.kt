package com.cryptocompare.model.error

/**
 * Что случилось, для того, кто получил неудачу из `Result`.
 *
 * Разбирать исключения по типам здесь нельзя: модель не знает ни Retrofit, ни
 * Room, ни Firebase — это работа слоя данных, и он заворачивает всё в
 * [AppException]. Сюда без обёртки доходит только то, что он не видел, и оно
 * честно называется [AppError.Unknown].
 */
fun Throwable.asAppError(): AppError = (this as? AppException)?.error ?: AppError.Unknown
