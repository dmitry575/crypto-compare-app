package com.cryptocompare.ui.error

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cryptocompare.model.error.AppError
import com.cryptocompare.model.error.AuthErrorReason
import com.cryptocompare.model.error.ValidationErrorReason
import com.cryptocompare.ui.R

/**
 * Текст ошибки на языке интерфейса.
 *
 * Единственное место, где ошибка превращается в слова. Раньше это делал
 * `toUserMessage()` с английскими константами, и русский интерфейс писал
 * «No internet connection»; а всё, что тот не узнавал, уходило на экран текстом
 * исключения или ответа бэкенда.
 */
@Composable
fun AppError.message(): String = stringResource(messageRes())

@StringRes
private fun AppError.messageRes(): Int =
    when (this) {
        AppError.Network -> R.string.error_network
        is AppError.Api -> R.string.error_server
        AppError.Database -> R.string.error_database
        AppError.Stream -> R.string.error_stream
        AppError.Unknown -> R.string.error_unknown
        is AppError.Auth -> reason.messageRes()
        is AppError.Validation -> reason.messageRes()
    }

@StringRes
private fun AuthErrorReason.messageRes(): Int =
    when (this) {
        AuthErrorReason.INVALID_CREDENTIALS -> R.string.error_auth_invalid_credentials
        AuthErrorReason.EMAIL_ALREADY_IN_USE -> R.string.error_auth_email_in_use
        AuthErrorReason.USER_NOT_FOUND -> R.string.error_auth_user_not_found
        AuthErrorReason.WEAK_PASSWORD -> R.string.error_auth_weak_password
        AuthErrorReason.RECENT_LOGIN_REQUIRED -> R.string.error_auth_recent_login
        AuthErrorReason.NOT_SIGNED_IN -> R.string.error_auth_not_signed_in
        AuthErrorReason.NO_PASSWORD_PROVIDER -> R.string.error_auth_no_password
        AuthErrorReason.GOOGLE_SIGN_IN_FAILED -> R.string.error_auth_google_failed
        AuthErrorReason.GOOGLE_TOKEN_MISSING -> R.string.error_auth_google_token
    }

@StringRes
private fun ValidationErrorReason.messageRes(): Int =
    when (this) {
        ValidationErrorReason.INVALID_EMAIL -> R.string.error_validation_email
        ValidationErrorReason.PASSWORD_TOO_SHORT -> R.string.error_validation_password_short
        ValidationErrorReason.PASSWORD_TOO_WEAK -> R.string.error_validation_password_weak
        ValidationErrorReason.PASSWORDS_DO_NOT_MATCH -> R.string.error_validation_passwords_differ
        ValidationErrorReason.NEGATIVE_VALUES -> R.string.error_validation_negative
    }
