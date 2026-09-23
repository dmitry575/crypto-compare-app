package com.cryptocompare.model.error

/**
 * Исключение, в котором уже решено, что случилось.
 *
 * Репозитории по-прежнему отдают `Result<T>`, но неудача внутри — это
 * [AppException] с [error], а исходное исключение лежит в [cause]: оно нужно
 * отчётам о сбоях, а не пользователю.
 */
class AppException(
    val error: AppError,
    cause: Throwable? = null,
) : Exception(error.toString(), cause)
