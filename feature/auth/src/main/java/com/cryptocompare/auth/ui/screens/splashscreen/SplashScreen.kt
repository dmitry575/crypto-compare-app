package com.cryptocompare.auth.ui.screens.splashscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptocompare.auth.R
import com.cryptocompare.auth.ui.components.AuthLogo
import com.cryptocompare.auth.viewmodel.splashviewmodel.SplashViewModel
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.bgPrimary

@Composable
fun SplashScreen(
    onNavigateHome: () -> Unit,
    onNavigateLogin: () -> Unit,
    onNavigateOnboarding: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(
        uiState.isCheckAuth,
        uiState.isAuthenticated,
        uiState.errorMessage,
        uiState.shouldShowOnboarding,
    ) {
        val authState = uiState.isAuthenticated
        if (uiState.isCheckAuth || authState == null) return@LaunchedEffect

        // онбординг идёт первым и при ошибке проверки входа тоже: он про продукт,
        // а не про авторизацию, и сеть ему не нужна
        if (uiState.shouldShowOnboarding) {
            onNavigateOnboarding()
            return@LaunchedEffect
        }

        if (uiState.errorMessage != null) return@LaunchedEffect

        if (authState) onNavigateHome() else onNavigateLogin()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.bgPrimary)
                .padding(Dimensions.Padding.screen),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthLogo()
        Spacer(modifier = Modifier.height(Dimensions.Spacing.md))
        Text(
            text = stringResource(R.string.auth_app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(Dimensions.Spacing.sm))
        if (uiState.isCheckAuth) {
            Text(
                text = stringResource(R.string.splash_checking),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            )
            Spacer(modifier = Modifier.height(Dimensions.Spacing.md))
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else {
            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(Dimensions.Spacing.md))
                Button(onClick = viewModel::checkAuthentification) {
                    Text(text = stringResource(R.string.splash_retry))
                }
            }
        }
    }
}
