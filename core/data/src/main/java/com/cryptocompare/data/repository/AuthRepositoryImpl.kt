package com.cryptocompare.data.repository

import com.cryptocompare.data.mapper.toAuthUser
import com.cryptocompare.data.util.DataConstants
import com.cryptocompare.data.util.appRunCatching
import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.domain.repository.CrashReporter
import com.cryptocompare.model.auth.AuthUser
import com.cryptocompare.model.error.AppError
import com.cryptocompare.model.error.AppException
import com.cryptocompare.model.error.AuthErrorReason
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl
    @Inject
    constructor(
        private val auth: FirebaseAuth,
        private val crashReporter: CrashReporter,
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
            appRunCatching {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: error(DataConstants.Auth.NULL_USER)
                user.toAuthUser()
            }.onSuccess { user -> crashReporter.setUser(user.uid) }

        override suspend fun signInWithEmail(
            email: String,
            password: String,
        ): Result<AuthUser> =
            appRunCatching {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user ?: error(DataConstants.Auth.NULL_USER)
                user.toAuthUser()
            }.onSuccess { user -> crashReporter.setUser(user.uid) }

        override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> =
            appRunCatching {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user ?: error(DataConstants.Auth.NULL_USER)
                user.toAuthUser()
            }.onSuccess { user -> crashReporter.setUser(user.uid) }

        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
            appRunCatching {
                auth.sendPasswordResetEmail(email).await()
                Unit
            }

        override suspend fun signOut() {
            auth.signOut()
            // отчёты после выхода не должны приписываться прежнему пользователю
            crashReporter.clearUser()
        }

        override suspend fun deleteAccount(): Result<Unit> =
            appRunCatching {
                val user = auth.currentUser ?: throw AppException(AppError.Auth(AuthErrorReason.NOT_SIGNED_IN))
                // FirebaseAuthRecentLoginRequiredException превращается в Auth(RECENT_LOGIN_REQUIRED),
                // и экран просит войти заново, а не показывает фразу Firebase
                user.delete().await()
                crashReporter.clearUser()
            }

        override suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
        ): Result<Unit> =
            appRunCatching {
                val user = auth.currentUser ?: throw AppException(AppError.Auth(AuthErrorReason.NOT_SIGNED_IN))
                val email = user.email ?: throw AppException(AppError.Auth(AuthErrorReason.NO_PASSWORD_PROVIDER))
                // Firebase не меняет пароль по старой сессии, поэтому сначала
                // переавторизуемся текущим паролем — заодно это его и проверяет.
                // Неверный пароль прилетит как FirebaseAuthInvalidCredentialsException.
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                user.reauthenticate(credential).await()
                user.updatePassword(newPassword).await()
                Unit
            }
    }
