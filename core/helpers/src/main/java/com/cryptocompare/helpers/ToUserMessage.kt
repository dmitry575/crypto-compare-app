package com.cryptocompare.helpers

import com.cryptocompare.helpers.util.ErrorMessageConstants
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Throwable.toUserMessage(): String =
    when (this) {
        // FirebaseAuthWeakPasswordException наследует FirebaseAuthInvalidCredentialsException,
        // поэтому обязан стоять выше — иначе слабый пароль покажется как «неверный пароль»
        is FirebaseAuthWeakPasswordException -> ErrorMessageConstants.WEAK_PASSWORD
        is FirebaseAuthInvalidCredentialsException -> ErrorMessageConstants.INVALID_CREDENTIALS
        is FirebaseAuthUserCollisionException -> ErrorMessageConstants.EMAIL_ALREADY_IN_USE
        is FirebaseAuthInvalidUserException -> ErrorMessageConstants.USER_NOT_FOUND
        is FirebaseAuthRecentLoginRequiredException -> ErrorMessageConstants.RECENT_LOGIN_REQUIRED

        is UnknownHostException,
        is ConnectException,
        is SocketTimeoutException,
        -> ErrorMessageConstants.NO_INTERNET
        is IOException -> ErrorMessageConstants.NO_INTERNET
        else -> message ?: ErrorMessageConstants.UNKNOWN
    }
