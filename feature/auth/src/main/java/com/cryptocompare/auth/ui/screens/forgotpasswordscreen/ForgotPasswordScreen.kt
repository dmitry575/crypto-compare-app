package com.cryptocompare.auth.ui.screens.forgotpasswordscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptocompare.auth.R
import com.cryptocompare.auth.ui.components.AuthBackground
import com.cryptocompare.auth.ui.components.AuthErrorMessage
import com.cryptocompare.auth.ui.components.AuthFooterLink
import com.cryptocompare.auth.ui.components.AuthLogo
import com.cryptocompare.auth.util.AuthConstants
import com.cryptocompare.auth.viewmodel.forgotpasswordviewmodel.ForgotPasswordViewModel
import com.cryptocompare.ui.components.AppPrimaryButton
import com.cryptocompare.ui.components.AppTextField
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.cryptoSuccess

@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val scrollState = rememberScrollState()

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
            Spacer(modifier = Modifier.height(Dimensions.Spacing.lg))

            AuthLogo()

            Spacer(modifier = Modifier.height(Dimensions.Spacing.md))

            Text(
                text = stringResource(R.string.forgot_password_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(Dimensions.Spacing.xs))

            Text(
                text =
                    if (uiState.isEmailSent) {
                        stringResource(R.string.forgot_password_success_hint)
                    } else {
                        stringResource(R.string.forgot_password_subtitle)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme.onBackground.copy(
                        alpha = AuthConstants.ForgotPassword.SUBTITLE_ALPHA,
                    ),
            )

            Spacer(modifier = Modifier.height(Dimensions.Spacing.xl))

            // после отправки форма не нужна: показываем подтверждение, чтобы
            // не провоцировать повторные письма
            if (uiState.isEmailSent) {
                Text(
                    text = stringResource(R.string.forgot_password_success),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.cryptoSuccess,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                AppTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    placeholder = stringResource(R.string.forgot_password_email),
                    leadingIcon = AuthConstants.ForgotPassword.EMAIL_ICON,
                    keyboardType = KeyboardType.Email,
                    isError = uiState.errorMessage != null,
                )

                Spacer(modifier = Modifier.height(Dimensions.Spacing.md))

                AppPrimaryButton(
                    text =
                        if (uiState.isLoading) {
                            stringResource(R.string.forgot_password_sending)
                        } else {
                            stringResource(R.string.forgot_password_submit)
                        },
                    onClick = viewModel::sendResetEmail,
                    enabled = !uiState.isLoading,
                )

                uiState.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(Dimensions.Spacing.md))
                    AuthErrorMessage(text = message)
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.Spacing.lg))

            AuthFooterLink(
                actionText = stringResource(R.string.forgot_password_back_to_login),
                onClick = onBackToLogin,
            )
        }
    }
}
