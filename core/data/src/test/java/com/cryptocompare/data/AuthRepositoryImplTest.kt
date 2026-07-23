package com.cryptocompare.data

import app.cash.turbine.test
import com.cryptocompare.data.repository.AuthRepositoryImpl
import com.cryptocompare.domain.repository.CrashReporter
import com.cryptocompare.model.auth.AuthUser
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {
    private val auth: FirebaseAuth = mockk(relaxed = true)
    private val crashReporter: CrashReporter = mockk(relaxed = true)
    private val repository = AuthRepositoryImpl(auth, crashReporter)

    @Test
    fun `currentUser maps firebase user`() {
        val firebaseUser = firebaseUser(uid = "uid-1", email = "user@example.com", displayName = "User")
        every { auth.currentUser } returns firebaseUser

        val result = repository.currentUser

        assertEquals(
            AuthUser(
                uid = "uid-1",
                email = "user@example.com",
                displayName = "User",
                photoUrl = null,
            ),
            result,
        )
    }

    @Test
    fun `currentUser reports password provider only for email accounts`() {
        val googleOnly =
            firebaseUser(
                uid = "uid-google",
                email = "google@example.com",
                displayName = null,
                providerIds = listOf(GoogleAuthProvider.PROVIDER_ID),
            )
        every { auth.currentUser } returns googleOnly
        assertFalse(repository.currentUser?.hasPasswordProvider ?: true)

        val withPassword =
            firebaseUser(
                uid = "uid-email",
                email = "email@example.com",
                displayName = null,
                providerIds = listOf(GoogleAuthProvider.PROVIDER_ID, EmailAuthProvider.PROVIDER_ID),
            )
        every { auth.currentUser } returns withPassword
        assertTrue(repository.currentUser?.hasPasswordProvider ?: false)
    }

    @Test
    fun `observeAuthState emits updates and unregisters listener`() =
        runTest {
            val listenerSlot = slot<FirebaseAuth.AuthStateListener>()
            var currentUser: FirebaseUser? = null
            every { auth.currentUser } answers { currentUser }
            every { auth.addAuthStateListener(capture(listenerSlot)) } returns Unit
            every { auth.removeAuthStateListener(any()) } returns Unit

            repository.observeAuthState().test {
                currentUser = firebaseUser(uid = "uid-1", email = "user@example.com", displayName = null)
                listenerSlot.captured.onAuthStateChanged(auth)
                assertEquals(
                    AuthUser(
                        uid = "uid-1",
                        email = "user@example.com",
                        displayName = null,
                        photoUrl = null,
                    ),
                    awaitItem(),
                )

                currentUser = null
                listenerSlot.captured.onAuthStateChanged(auth)
                assertNull(awaitItem())

                cancelAndIgnoreRemainingEvents()
            }

            verify { auth.removeAuthStateListener(listenerSlot.captured) }
        }

    @Test
    fun `signUpWithEmail returns mapped user on success`() =
        runTest {
            val firebaseUser = firebaseUser(uid = "uid-2", email = "new@example.com", displayName = "New")
            val authResult = mockk<AuthResult>()
            every { authResult.user } returns firebaseUser
            every { auth.createUserWithEmailAndPassword("new@example.com", "secret") } returns
                Tasks.forResult(authResult)

            val result = repository.signUpWithEmail("new@example.com", "secret")

            assertTrue(result.isSuccess)
            assertEquals(
                AuthUser(
                    uid = "uid-2",
                    email = "new@example.com",
                    displayName = "New",
                    photoUrl = null,
                ),
                result.getOrNull(),
            )
        }

    @Test
    fun `signUpWithEmail returns failure when firebase throws`() =
        runTest {
            every { auth.createUserWithEmailAndPassword("bad@example.com", "secret") } returns
                Tasks.forException(FirebaseAuthUserCollisionException(ERROR_CODE, "email already in use"))

            val result = repository.signUpWithEmail("bad@example.com", "secret")

            assertTrue(result.isFailure)
            // тип не должен схлопываться в IllegalStateException: по нему UI выбирает текст
            assertTrue(result.exceptionOrNull() is FirebaseAuthUserCollisionException)
        }

    @Test
    fun `signInWithEmail returns mapped user on success`() =
        runTest {
            val firebaseUser = firebaseUser(uid = "uid-3", email = "login@example.com", displayName = null)
            val authResult = mockk<AuthResult>()
            every { authResult.user } returns firebaseUser
            every { auth.signInWithEmailAndPassword("login@example.com", "secret") } returns
                Tasks.forResult(authResult)

            val result = repository.signInWithEmail("login@example.com", "secret")

            assertTrue(result.isSuccess)
            assertEquals(
                AuthUser(
                    uid = "uid-3",
                    email = "login@example.com",
                    displayName = null,
                    photoUrl = null,
                ),
                result.getOrNull(),
            )
        }

    @Test
    fun `signInWithGoogle uses credential and returns mapped user`() =
        runTest {
            val firebaseUser = firebaseUser(uid = "uid-4", email = "google@example.com", displayName = "Google")
            val authResult = mockk<AuthResult>()
            every { authResult.user } returns firebaseUser
            every { auth.signInWithCredential(any()) } returns Tasks.forResult(authResult)

            val result = repository.signInWithGoogle("token")

            assertTrue(result.isSuccess)
            assertEquals(
                AuthUser(
                    uid = "uid-4",
                    email = "google@example.com",
                    displayName = "Google",
                    photoUrl = null,
                ),
                result.getOrNull(),
            )
            verify(exactly = 1) { auth.signInWithCredential(any()) }
        }

    @Test
    fun `signOut delegates to firebase auth`() =
        runTest {
            every { auth.signOut() } returns Unit

            repository.signOut()

            verify { auth.signOut() }
        }

    @Test
    fun `signInWithEmail returns failure when firebase throws`() =
        runTest {
            every { auth.signInWithEmailAndPassword("login@example.com", "secret") } returns
                Tasks.forException(FirebaseAuthInvalidCredentialsException(ERROR_CODE, "wrong password"))

            val result = repository.signInWithEmail("login@example.com", "secret")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is FirebaseAuthInvalidCredentialsException)
        }

    @Test
    fun `signInWithGoogle returns failure when firebase throws`() =
        runTest {
            every { auth.signInWithCredential(any()) } returns
                Tasks.forException(FirebaseAuthInvalidUserException(ERROR_CODE, "user disabled"))

            val result = repository.signInWithGoogle("token")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is FirebaseAuthInvalidUserException)
        }

    @Test
    fun `deleteAccount deletes the current firebase user`() =
        runTest {
            val firebaseUser = firebaseUser(uid = "uid-5", email = "bye@example.com", displayName = null)
            every { firebaseUser.delete() } returns Tasks.forResult(null)
            every { auth.currentUser } returns firebaseUser

            val result = repository.deleteAccount()

            assertTrue(result.isSuccess)
            verify(exactly = 1) { firebaseUser.delete() }
        }

    @Test
    fun `deleteAccount returns failure when firebase throws`() =
        runTest {
            val firebaseUser = firebaseUser(uid = "uid-6", email = "bye@example.com", displayName = null)
            // так Firebase отвечает, когда логин слишком старый для чувствительной операции
            every { firebaseUser.delete() } returns
                Tasks.forException(FirebaseAuthRecentLoginRequiredException(ERROR_CODE, "recent login required"))
            every { auth.currentUser } returns firebaseUser

            val result = repository.deleteAccount()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is FirebaseAuthRecentLoginRequiredException)
        }

    @Test
    fun `deleteAccount returns failure without current user`() =
        runTest {
            every { auth.currentUser } returns null

            val result = repository.deleteAccount()

            assertTrue(result.isFailure)
            assertNotNull(result.exceptionOrNull())
        }

    private companion object {
        const val ERROR_CODE = "ERROR_TEST"
    }

    private fun firebaseUser(
        uid: String,
        email: String?,
        displayName: String?,
        providerIds: List<String> = emptyList(),
    ): FirebaseUser {
        val user = mockk<FirebaseUser>()
        every { user.uid } returns uid
        every { user.email } returns email
        every { user.displayName } returns displayName
        every { user.photoUrl } returns null
        every { user.providerData } returns
            providerIds
                .map { id ->
                    val userInfo = mockk<UserInfo>()
                    every { userInfo.providerId } returns id
                    userInfo
                }.toMutableList()
        return user
    }
}
