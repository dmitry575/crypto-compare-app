package com.cryptocompare.model.auth

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    /**
     * У аккаунта есть провайдер email + пароль. Аккаунтам, заведённым только через
     * Google, менять нечего, поэтому UI прячет соответствующий пункт.
     */
    val hasPasswordProvider: Boolean = false,
)
