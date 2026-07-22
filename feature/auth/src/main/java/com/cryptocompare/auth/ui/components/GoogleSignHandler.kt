package com.cryptocompare.auth.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.cryptocompare.auth.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun rememberGoogleSignInHandler(
    onToken: (String) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    val errorTokenMissing = stringResource(R.string.google_sign_in_token_missing)
    val errorSignInFailed = stringResource(R.string.google_sign_in_failed)

    val googleSignInOptions =
        remember(webClientId) {
            GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
        }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    onError(errorTokenMissing)
                } else {
                    onToken(idToken)
                }
            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "ApiException statusCode=${e.statusCode}", e)
                onError(errorSignInFailed)
            }
        }

    return signIn@{
        val activity =
            context.findActivity() ?: run {
                Log.e("GoogleSignIn", "No activity")
                return@signIn
            }

        val client = GoogleSignIn.getClient(activity, googleSignInOptions)

        client.signOut().addOnCompleteListener {
            launcher.launch(client.signInIntent)
        }
    }
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
