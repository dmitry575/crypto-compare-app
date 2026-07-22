package com.cryptocompare.helpers.util

/** Тексты ошибок, которые показываются пользователю. */
object ErrorMessageConstants {
    const val NO_INTERNET = "No internet connection"
    const val UNKNOWN = "Unknown error"

    const val INVALID_CREDENTIALS = "Incorrect email or password"
    const val EMAIL_ALREADY_IN_USE = "An account with this email already exists"
    const val USER_NOT_FOUND = "User not found or disabled"
    const val WEAK_PASSWORD = "Password is too weak"
    const val RECENT_LOGIN_REQUIRED = "Please sign in again to confirm it is you"
}
