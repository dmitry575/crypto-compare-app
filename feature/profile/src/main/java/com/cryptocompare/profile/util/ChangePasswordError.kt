package com.cryptocompare.profile.util

/**
 * Причина, по которой форму нельзя отправить. ViewModel не знает про ресурсы,
 * поэтому отдаёт причину, а текст подставляет экран.
 */
enum class ChangePasswordError {
    CURRENT_PASSWORD_EMPTY,
    NEW_PASSWORD_TOO_WEAK,
    PASSWORDS_DO_NOT_MATCH,
    NEW_PASSWORD_SAME_AS_CURRENT,
}
