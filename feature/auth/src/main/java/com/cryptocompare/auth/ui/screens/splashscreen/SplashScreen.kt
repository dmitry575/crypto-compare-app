package com.cryptocompare.auth.ui.screens.splashscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

/**
 * Заставка: ждёт минимальную паузу и уходит дальше.
 *
 * Развилка здесь одна — онбординг. Вход не проверяется: каталог открыт и гостю,
 * а войти можно из профиля, когда понадобится избранное.
 */
@Composable
fun SplashScreen(
    onReady: () -> Unit,
    onNavigateOnboarding: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(uiState.isPreparing, uiState.shouldShowOnboarding) {
        if (uiState.isPreparing) return@LaunchedEffect

        if (uiState.shouldShowOnboarding) onNavigateOnboarding() else onReady()
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
        Spacer(modifier = Modifier.height(Dimensions.Spacing.md))
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
