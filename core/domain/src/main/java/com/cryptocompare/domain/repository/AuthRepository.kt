package com.cryptocompare.domain.repository

import com.cryptocompare.model.auth.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: AuthUser?

    fun observeAuthState(): Flow<AuthUser?>

    suspend fun signUpWithEmail(
        email: String,
        password: String,
    ): Result<AuthUser>

    suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<AuthUser>

    suspend fun signInWithGoogle(idToken: String): Result<AuthUser>

    /**
     * Отправляет письмо со ссылкой на сброс пароля. Firebase намеренно отвечает
     * успехом и для незарегистрированного адреса — иначе форма позволяла бы
     * перебирать чужие почты.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    suspend fun signOut()

    /** Необратимо удаляет аккаунт в Firebase. Требует свежей авторизации. */
    suspend fun deleteAccount(): Result<Unit>

    /**
     * Меняет пароль. [currentPassword] нужен не для проверки на нашей стороне,
     * а для повторной авторизации: Firebase не даёт менять пароль по старой сессии.
     */
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): Result<Unit>
}
