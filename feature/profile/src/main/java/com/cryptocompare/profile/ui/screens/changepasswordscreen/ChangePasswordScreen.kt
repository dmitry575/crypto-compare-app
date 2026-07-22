package com.cryptocompare.profile.ui.screens.changepasswordscreen

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptocompare.profile.R
import com.cryptocompare.profile.util.ChangePasswordError
import com.cryptocompare.profile.viewmodel.changepasswordviewmodel.ChangePasswordViewModel
import com.cryptocompare.ui.components.AppPrimaryButton
import com.cryptocompare.ui.components.AppTextField
import com.cryptocompare.ui.components.PasswordRequirements
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.bgPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    val validationMessage = uiState.validationError?.let { error -> stringResource(error.messageRes()) }
    val errorMessage = validationMessage ?: uiState.errorMessage
    val successMessage = stringResource(R.string.change_password_success)

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onErrorShown()
        }
    }

    // подтверждение показываем до ухода назад, иначе смена пароля выглядит как «ничего не произошло»
    LaunchedEffect(uiState.isPasswordChanged) {
        if (uiState.isPasswordChanged) {
            snackbarHostState.showSnackbar(successMessage)
            onBack()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.change_password_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.profile_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.bgPrimary)
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(
                        horizontal = Dimensions.Padding.screenHorizontal,
                        vertical = Dimensions.Padding.screenVertical,
                    ),
            verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.formFields),
        ) {
            AppTextField(
                value = uiState.currentPassword,
                onValueChange = viewModel::onCurrentPasswordChange,
                placeholder = stringResource(R.string.change_password_current),
                leadingIcon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true,
                isError = uiState.validationError == ChangePasswordError.CURRENT_PASSWORD_EMPTY,
            )

            AppTextField(
                value = uiState.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                placeholder = stringResource(R.string.change_password_new),
                leadingIcon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true,
                isError = uiState.validationError == ChangePasswordError.NEW_PASSWORD_TOO_WEAK,
            )

            PasswordRequirements(
                lengthMet = uiState.passwordLengthMet,
                letterMet = uiState.passwordLetterMet,
                numberMet = uiState.passwordNumberMet,
            )

            AppTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                placeholder = stringResource(R.string.change_password_confirm),
                leadingIcon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true,
                isError = uiState.validationError == ChangePasswordError.PASSWORDS_DO_NOT_MATCH,
            )

            AppPrimaryButton(
                text =
                    if (uiState.isLoading) {
                        stringResource(R.string.change_password_submitting)
                    } else {
                        stringResource(R.string.change_password_submit)
                    },
                onClick = viewModel::changePassword,
                enabled = !uiState.isLoading,
            )
        }
    }
}

@StringRes
private fun ChangePasswordError.messageRes(): Int =
    when (this) {
        ChangePasswordError.CURRENT_PASSWORD_EMPTY -> R.string.change_password_error_current_empty
        ChangePasswordError.NEW_PASSWORD_TOO_WEAK -> R.string.change_password_error_weak
        ChangePasswordError.PASSWORDS_DO_NOT_MATCH -> R.string.change_password_error_mismatch
        ChangePasswordError.NEW_PASSWORD_SAME_AS_CURRENT -> R.string.change_password_error_same
    }
