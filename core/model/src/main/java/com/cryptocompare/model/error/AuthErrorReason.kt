package com.cryptocompare.model.error

/** Что именно не так со входом или аккаунтом — у каждой причины свой текст. */
enum class AuthErrorReason {
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_IN_USE,
    USER_NOT_FOUND,
    WEAK_PASSWORD,

    /** Удаление аккаунта и смена пароля требуют свежего входа. */
    RECENT_LOGIN_REQUIRED,

    /** Действие для вошедшего пользователя, а сессии нет. */
    NOT_SIGNED_IN,

    /** У аккаунта, заведённого через Google, нет пароля, который можно сменить. */
    NO_PASSWORD_PROVIDER,
    GOOGLE_SIGN_IN_FAILED,
    GOOGLE_TOKEN_MISSING,
}
