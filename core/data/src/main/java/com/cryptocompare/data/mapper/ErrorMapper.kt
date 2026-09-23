package com.cryptocompare.data.mapper

import android.database.SQLException
import com.cryptocompare.model.error.AppError
import com.cryptocompare.model.error.AppException
import com.cryptocompare.model.error.AuthErrorReason
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import retrofit2.HttpException
import java.io.IOException

/**
 * Что случилось, по исключению конкретной сети, базы или Firebase.
 *
 * Единственное место, где разбираются типы исключений: слой данных знает и
 * Retrofit, и Room, и Firebase, а выше уходит уже [AppError]. Раньше этим
 * занимался `toUserMessage()` в `core:helpers` — он тащил Firebase в общий
 * модуль и возвращал английские строки, а всё неопознанное показывал текстом
 * исключения.
 */
internal fun Throwable.toAppError(): AppError =
    when (this) {
        is AppException -> error

        // FirebaseAuthWeakPasswordException наследует FirebaseAuthInvalidCredentialsException,
        // поэтому обязан стоять выше — иначе слабый пароль покажется как «неверный пароль»
        is FirebaseAuthWeakPasswordException -> AppError.Auth(AuthErrorReason.WEAK_PASSWORD)
        is FirebaseAuthInvalidCredentialsException -> AppError.Auth(AuthErrorReason.INVALID_CREDENTIALS)
        is FirebaseAuthUserCollisionException -> AppError.Auth(AuthErrorReason.EMAIL_ALREADY_IN_USE)
        is FirebaseAuthInvalidUserException -> AppError.Auth(AuthErrorReason.USER_NOT_FOUND)
        is FirebaseAuthRecentLoginRequiredException -> AppError.Auth(AuthErrorReason.RECENT_LOGIN_REQUIRED)
        is FirebaseNetworkException -> AppError.Network
        is FirebaseFirestoreException -> firestoreError()

        // бэкенд ответил не-2xx: Retrofit бросает это вместо тела
        is HttpException -> AppError.Api(code())

        // UnknownHost, Connect, SocketTimeout, SSL — всё это IOException
        is IOException -> AppError.Network
        is SQLException -> AppError.Database

        else -> AppError.Unknown
    }

/** Уже разобранное исключение остаётся как есть, чужое заворачивается с причиной. */
internal fun Throwable.toAppException(): AppException = this as? AppException ?: AppException(toAppError(), this)

private fun FirebaseFirestoreException.firestoreError(): AppError =
    when (code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        -> AppError.Network

        FirebaseFirestoreException.Code.UNAUTHENTICATED,
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        -> AppError.Auth(AuthErrorReason.NOT_SIGNED_IN)

        else -> AppError.Unknown
    }
