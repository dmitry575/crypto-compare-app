package com.cryptocompare.helpers

import com.cryptocompare.helpers.util.ErrorMessageConstants
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ToUserMessageTest {
    @Test
    fun `wrong password becomes an explicit message instead of a firebase sentence`() {
        val exception = FirebaseAuthInvalidCredentialsException(ERROR_CODE, FIREBASE_TEXT)

        assertEquals(ErrorMessageConstants.INVALID_CREDENTIALS, exception.toUserMessage())
    }

    @Test
    fun `weak password wins over invalid credentials`() {
        // FirebaseAuthWeakPasswordException наследует FirebaseAuthInvalidCredentialsException,
        // и при неверном порядке веток слабый пароль показался бы как «неверный пароль»
        val exception = FirebaseAuthWeakPasswordException(ERROR_CODE, FIREBASE_TEXT, "too short")

        assertEquals(ErrorMessageConstants.WEAK_PASSWORD, exception.toUserMessage())
    }

    @Test
    fun `email collision becomes an explicit message`() {
        val exception = FirebaseAuthUserCollisionException(ERROR_CODE, FIREBASE_TEXT)

        assertEquals(ErrorMessageConstants.EMAIL_ALREADY_IN_USE, exception.toUserMessage())
    }

    @Test
    fun `disabled or missing user becomes an explicit message`() {
        val exception = FirebaseAuthInvalidUserException(ERROR_CODE, FIREBASE_TEXT)

        assertEquals(ErrorMessageConstants.USER_NOT_FOUND, exception.toUserMessage())
    }

    @Test
    fun `stale session asks the user to sign in again`() {
        val exception = FirebaseAuthRecentLoginRequiredException(ERROR_CODE, FIREBASE_TEXT)

        assertEquals(ErrorMessageConstants.RECENT_LOGIN_REQUIRED, exception.toUserMessage())
    }

    @Test
    fun `network failures still report a missing connection`() {
        listOf(
            UnknownHostException("api.example.com"),
            ConnectException("refused"),
            SocketTimeoutException("timeout"),
            IOException("stream closed"),
        ).forEach { exception ->
            assertEquals(exception.toString(), ErrorMessageConstants.NO_INTERNET, exception.toUserMessage())
        }
    }

    @Test
    fun `unknown exception falls back to its own message`() {
        assertEquals("db locked", IllegalStateException("db locked").toUserMessage())
    }

    @Test
    fun `exception without a message falls back to the generic text`() {
        assertEquals(ErrorMessageConstants.UNKNOWN, IllegalStateException().toUserMessage())
    }

    private companion object {
        const val ERROR_CODE = "ERROR_TEST"
        const val FIREBASE_TEXT = "The supplied auth credential is incorrect, malformed or has expired."
    }
}
