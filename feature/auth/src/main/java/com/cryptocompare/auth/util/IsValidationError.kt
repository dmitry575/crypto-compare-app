package com.cryptocompare.auth.util

import com.cryptocompare.model.error.AppError
import com.cryptocompare.model.error.ValidationErrorReason

/**
 * Относится ли ошибка к полю с этими причинами — тогда поле и подсвечивается.
 *
 * Пока ошибка была строкой, экран красил пустые поля наугад: на «неверной
 * почте» краснело пустое поле пароля, а сама почта оставалась обычной. Теперь
 * ошибка говорит, про что она, и красным становится ровно то поле, которое надо
 * исправить. Ошибки входа («неверный пароль») полей не красят — какое из двух
 * неверно, сервер не говорит.
 */
internal fun AppError?.isValidation(vararg reasons: ValidationErrorReason): Boolean =
    this is AppError.Validation && reason in reasons
