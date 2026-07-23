package com.cryptocompare.auth.ui.screens.registerscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptocompare.auth.R
import com.cryptocompare.auth.ui.components.AuthBackground
import com.cryptocompare.auth.ui.components.AuthDivider
import com.cryptocompare.auth.ui.components.AuthErrorMessage
import com.cryptocompare.auth.ui.components.AuthFooterLink
import com.cryptocompare.auth.ui.components.AuthGoogleButton
import com.cryptocompare.auth.ui.components.AuthLogo
import com.cryptocompare.auth.ui.components.rememberGoogleSignInHandler
import com.cryptocompare.auth.viewmodel.registrationviewmodel.RegistrationViewModel
import com.cryptocompare.ui.components.AppPrimaryButton
import com.cryptocompare.ui.components.AppTextField
import com.cryptocompare.ui.components.PasswordRequirements
import com.cryptocompare.ui.theme.Dimensions

@Composable
fun RegisterScreen(
    onLoginClick: () -> Unit,
    onAuthenticated: () -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val scrollState = rememberScrollState()
    val googleSignInHandler =
        rememberGoogleSignInHandler(
            onToken = viewModel::signUpWithGoogle,
            onError = viewModel::onGoogleError,
        )

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthenticated()
        }
    }

    AuthBackground {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        horizontal = Dimensions.Padding.screenHorizontal,
                        vertical = Dimensions.Padding.screenVertical,
                    ),
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(modifier = Modifier.height(Dimensions.Spacing.md))

            AuthLogo()

            Spacer(modifier = Modifier.height(Dimensions.Spacing.md))

            Text(
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = stringResource(R.string.register_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(Dimensions.Spacing.xl))

            AppTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                placeholder = stringResource(R.string.auth_email),
                leadingIcon = Icons.Outlined.MailOutline,
                keyboardType = KeyboardType.Email,
                isError = uiState.errorMessage != null && uiState.email.isBlank(),
            )

            Spacer(modifier = Modifier.height(Dimensions.Spacing.sm))

            AppTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = stringResource(R.string.auth_password),
                leadingIcon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true,
                isError = uiState.errorMessage != null && uiState.password.isBlank(),
            )

            PasswordRequirements(
                lengthMet = uiState.passwordLengthMet,
                letterMet = uiState.passwordLetterMet,
                numberMet = uiState.passwordNumberMet,
            )

            Spacer(modifier = Modifier.height(Dimensions.Spacing.sm))

            AppTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                placeholder = stringResource(R.string.register_confirm_password),
                leadingIcon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true,
                isError = uiState.errorMessage != null && uiState.confirmPassword.isBlank(),
            )

            Spacer(modifier = Modifier.height(Dimensions.Spacing.md))

            AppPrimaryButton(
                text =
                    stringResource(
                        if (uiState.isLoading) R.string.register_submitting else R.string.register_submit,
                    ),
                onClick = viewModel::signUpWithEmail,
                enabled = !uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(Dimensions.Spacing.md))

            AuthDivider(text = stringResource(R.string.auth_divider_or))

            Spacer(modifier = Modifier.height(Dimensions.Spacing.md))

            AuthGoogleButton(
                text = stringResource(R.string.auth_continue_google),
                onClick = googleSignInHandler,
            )

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(Dimensions.Spacing.md))
                AuthErrorMessage(text = message)
            }

            Spacer(modifier = Modifier.height(Dimensions.Spacing.lg))

            AuthFooterLink(
                prompt = stringResource(R.string.register_have_account),
                actionText = stringResource(R.string.register_sign_in),
                onClick = onLoginClick,
            )

            Spacer(modifier = Modifier.height(Dimensions.Spacing.xl))
        }
    }
}
