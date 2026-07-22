package com.cryptocompare.data.repository

import com.cryptocompare.data.mapper.toAuthUser
import com.cryptocompare.data.util.DataConstants
import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.model.auth.AuthUser
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl
    @Inject
    constructor(
        private val auth: FirebaseAuth,
    ) : AuthRepository {
        override val currentUser: AuthUser?
            get() = auth.currentUser?.toAuthUser()

        override fun observeAuthState(): Flow<AuthUser?> =
            callbackFlow {
                val listener =
                    FirebaseAuth.AuthStateListener { firebaseAuth ->
                        trySend(firebaseAuth.currentUser?.toAuthUser())
                    }
                auth.addAuthStateListener(listener)
                awaitClose {
                    auth.removeAuthStateListener(listener)
                }
            }

        override suspend fun signUpWithEmail(
            email: String,
            password: String,
        ): Result<AuthUser> =
            runCatching {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: error(DataConstants.Auth.NULL_USER)
                user.toAuthUser()
            }.onFailure { exception -> if (exception is CancellationException) throw exception }

        override suspend fun signInWithEmail(
            email: String,
            password: String,
        ): Result<AuthUser> =
            runCatching {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user ?: error(DataConstants.Auth.NULL_USER)
                user.toAuthUser()
            }.onFailure { exception -> if (exception is CancellationException) throw exception }

        override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> =
            runCatching {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user ?: error(DataConstants.Auth.NULL_USER)
                user.toAuthUser()
            }.onFailure { exception -> if (exception is CancellationException) throw exception }

        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
            runCatching {
                auth.sendPasswordResetEmail(email).await()
                Unit
            }.onFailure { exception -> if (exception is CancellationException) throw exception }

        override suspend fun signOut() {
            auth.signOut()
        }

        override suspend fun deleteAccount(): Result<Unit> =
            runCatching {
                val user = auth.currentUser ?: error(DataConstants.Auth.NO_CURRENT_USER)
                // FirebaseAuthRecentLoginRequiredException доходит до UI как есть —
                // toUserMessage() превращает его в понятную просьбу перелогиниться
                user.delete().await()
                Unit
            }.onFailure { exception -> if (exception is CancellationException) throw exception }

        override suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
        ): Result<Unit> =
            runCatching {
                val user = auth.currentUser ?: error(DataConstants.Auth.NO_CURRENT_USER)
                val email = user.email ?: error(DataConstants.Auth.NO_PASSWORD_PROVIDER)
                // Firebase не меняет пароль по старой сессии, поэтому сначала
                // переавторизуемся текущим паролем — заодно это его и проверяет.
                // Неверный пароль прилетит как FirebaseAuthInvalidCredentialsException.
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                user.reauthenticate(credential).await()
                user.updatePassword(newPassword).await()
                Unit
            }.onFailure { exception -> if (exception is CancellationException) throw exception }
    }
