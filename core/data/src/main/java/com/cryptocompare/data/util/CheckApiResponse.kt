package com.cryptocompare.data.util

import com.cryptocompare.model.error.AppError
import com.cryptocompare.model.error.AppException

/**
 * Бэкенд отвечает `200` и с ошибкой — тогда `errorCode` не ноль, а в
 * `errorMsgs` его объяснение.
 *
 * Объяснение уходит в причину исключения — оно нужно отчётам о сбоях, — но не
 * пользователю: раньше экран показывал его как есть, по-английски и словами
 * бэкенда («Invalid request»).
 */
internal fun checkApiResponse(
    errorCode: Int,
    errorMsgs: List<String>?,
) {
    if (errorCode == DataConstants.Api.ERROR_CODE_OK) return

    val details = errorMsgs?.joinToString(DataConstants.Api.MESSAGES_SEPARATOR).orEmpty()
    throw AppException(AppError.Api(errorCode), IllegalStateException(details))
}
