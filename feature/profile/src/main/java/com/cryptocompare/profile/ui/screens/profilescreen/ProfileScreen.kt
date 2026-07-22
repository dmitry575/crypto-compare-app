package com.cryptocompare.profile.ui.screens.profilescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptocompare.profile.R
import com.cryptocompare.profile.ui.screens.profilescreen.components.ProfileActionRow
import com.cryptocompare.profile.ui.screens.profilescreen.components.ProfileComingSoonRow
import com.cryptocompare.profile.ui.screens.profilescreen.components.ProfileConfirmDialog
import com.cryptocompare.profile.ui.screens.profilescreen.components.ProfileHeader
import com.cryptocompare.profile.ui.screens.profilescreen.components.ProfileSectionTitle
import com.cryptocompare.profile.viewmodel.profileviewmodel.ProfileViewModel
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.bgPrimary
import com.cryptocompare.ui.theme.cryptoError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    onChangePasswordClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.isSignedOut) {
        if (uiState.isSignedOut) {
            onSignedOut()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onErrorShown()
        }
    }

    if (uiState.showSignOutConfirmation) {
        ProfileConfirmDialog(
            title = stringResource(R.string.profile_sign_out_dialog_title),
            text = stringResource(R.string.profile_sign_out_dialog_text),
            confirmText = stringResource(R.string.profile_sign_out_dialog_confirm),
            onConfirm = viewModel::onSignOutConfirmed,
            onDismiss = viewModel::onSignOutDismissed,
        )
    }

    if (uiState.showDeleteConfirmation) {
        ProfileConfirmDialog(
            title = stringResource(R.string.profile_delete_dialog_title),
            text = stringResource(R.string.profile_delete_dialog_text),
            confirmText = stringResource(R.string.profile_delete_dialog_confirm),
            onConfirm = viewModel::onDeleteAccountConfirmed,
            onDismiss = viewModel::onDeleteAccountDismissed,
            confirmColor = MaterialTheme.colorScheme.cryptoError,
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_title),
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
                    .padding(vertical = Dimensions.Padding.screenVertical),
            verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.lg),
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            uiState.user?.let { user ->
                ProfileHeader(
                    user = user,
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.screenHorizontal),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.xs)) {
                ProfileSectionTitle(text = stringResource(R.string.profile_account_section))

                if (uiState.user?.hasPasswordProvider == true) {
                    ProfileActionRow(
                        text = stringResource(R.string.profile_change_password),
                        icon = Icons.Outlined.Lock,
                        onClick = onChangePasswordClick,
                        enabled = !uiState.isLoading,
                    )
                }

                ProfileActionRow(
                    text = stringResource(R.string.profile_sign_out),
                    icon = Icons.AutoMirrored.Filled.Logout,
                    onClick = viewModel::onSignOutClick,
                    enabled = !uiState.isLoading,
                )

                ProfileActionRow(
                    text = stringResource(R.string.profile_delete_account),
                    icon = Icons.Filled.DeleteForever,
                    onClick = viewModel::onDeleteAccountClick,
                    enabled = !uiState.isLoading,
                    tint = MaterialTheme.colorScheme.cryptoError,
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Dimensions.Padding.screenHorizontal),
            )

            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.xs)) {
                ProfileSectionTitle(text = stringResource(R.string.profile_settings_section))

                ProfileComingSoonRow(
                    text = stringResource(R.string.profile_theme),
                    icon = Icons.Outlined.DarkMode,
                )

                ProfileComingSoonRow(
                    text = stringResource(R.string.profile_base_currency),
                    icon = Icons.Outlined.CurrencyExchange,
                )
            }
        }
    }
}
