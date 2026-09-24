package com.cryptocompare.auth.util

import com.cryptocompare.model.error.AppError
import com.cryptocompare.model.error.ValidationErrorReason

/**
 * Ошибка проверки, которую правка поля и исправляет, гаснет вместе с правкой.
 *
 * Иначе поле оставалось красным, пока пользователь его уже чинит, — до
 * следующего нажатия «Войти». Прочие ошибки остаются: неверный пароль от
 * правки почты не исправился.
 */
internal fun AppError?.withoutValidation(vararg reasons: ValidationErrorReason): AppError? =
    takeUnless { it.isValidation(*reasons) }
