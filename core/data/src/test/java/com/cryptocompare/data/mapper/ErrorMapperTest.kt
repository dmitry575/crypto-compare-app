package com.cryptocompare.data.mapper

import android.database.SQLException
import com.cryptocompare.model.error.AppError
import com.cryptocompare.model.error.AppException
import com.cryptocompare.model.error.AuthErrorReason
import com.cryptocompare.model.error.ValidationErrorReason
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorMapperTest {
    @Test
    fun `no route to the server is a network error, and worth retrying`() {
        listOf(UnknownHostException("api"), SocketTimeoutException(), IOException("reset")).forEach { exception ->
            val error = exception.toAppError()

            assertEquals(AppError.Network, error)
            assertTrue(error.isRetryable)
        }
    }

    @Test
    fun `a non-2xx answer keeps the http code`() {
        val exception = HttpException(Response.error<Unit>(HTTP_UNAVAILABLE, "".toResponseBody()))

        assertEquals(AppError.Api(HTTP_UNAVAILABLE), exception.toAppError())
    }

    @Test
    fun `a broken database is not something a retry fixes`() {
        val error = mockk<SQLException>().toAppError()

        assertEquals(AppError.Database, error)
        assertFalse(error.isRetryable)
    }

    @Test
    fun `firebase auth exceptions become explicit reasons`() {
        assertEquals(
            AppError.Auth(AuthErrorReason.INVALID_CREDENTIALS),
            FirebaseAuthInvalidCredentialsException(CODE, TEXT).toAppError(),
        )
        assertEquals(
            AppError.Auth(AuthErrorReason.EMAIL_ALREADY_IN_USE),
            FirebaseAuthUserCollisionException(CODE, TEXT).toAppError(),
        )
        assertEquals(
            AppError.Auth(AuthErrorReason.USER_NOT_FOUND),
            FirebaseAuthInvalidUserException(CODE, TEXT).toAppError(),
        )
        assertEquals(
            AppError.Auth(AuthErrorReason.RECENT_LOGIN_REQUIRED),
            FirebaseAuthRecentLoginRequiredException(CODE, TEXT).toAppError(),
        )
    }

    @Test
    fun `weak password wins over invalid credentials`() {
        // FirebaseAuthWeakPasswordException наследует FirebaseAuthInvalidCredentialsException,
        // и при неверном порядке веток слабый пароль показался бы как «неверный пароль»
        val exception = FirebaseAuthWeakPasswordException(CODE, TEXT, "too short")

        assertEquals(AppError.Auth(AuthErrorReason.WEAK_PASSWORD), exception.toAppError())
    }

    @Test
    fun `auth errors are not retried - the password will not fix itself`() {
        assertFalse(FirebaseAuthInvalidCredentialsException(CODE, TEXT).toAppError().isRetryable)
    }

    @Test
    fun `firebase network trouble is a network error, whichever sdk reports it`() {
        assertEquals(AppError.Network, FirebaseNetworkException(TEXT).toAppError())
        assertEquals(
            AppError.Network,
            FirebaseFirestoreException(TEXT, FirebaseFirestoreException.Code.UNAVAILABLE).toAppError(),
        )
    }

    @Test
    fun `firestore refusing access means the session is gone`() {
        assertEquals(
            AppError.Auth(AuthErrorReason.NOT_SIGNED_IN),
            FirebaseFirestoreException(TEXT, FirebaseFirestoreException.Code.PERMISSION_DENIED).toAppError(),
        )
    }

    @Test
    fun `an already classified error passes through untouched`() {
        val classified = AppException(AppError.Validation(ValidationErrorReason.NEGATIVE_VALUES))

        assertSame(classified, classified.toAppException())
        assertEquals(AppError.Validation(ValidationErrorReason.NEGATIVE_VALUES), classified.toAppError())
    }

    @Test
    fun `anything else is unknown, and the original stays as the cause`() {
        val original = IllegalStateException("something odd")

        val wrapped = original.toAppException()

        assertEquals(AppError.Unknown, wrapped.error)
        assertSame(original, wrapped.cause)
    }

    private companion object {
        const val CODE = "ERROR_CODE"
        const val TEXT = "Firebase sentence nobody should see"
        const val HTTP_UNAVAILABLE = 503
    }
}
