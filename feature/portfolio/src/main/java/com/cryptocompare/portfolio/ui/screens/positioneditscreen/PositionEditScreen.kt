package com.cryptocompare.portfolio.ui.screens.positioneditscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptocompare.portfolio.R
import com.cryptocompare.portfolio.ui.screens.positioneditscreen.components.ExchangeField
import com.cryptocompare.portfolio.viewmodel.positioneditviewmodel.PositionEditViewModel
import com.cryptocompare.ui.components.AppPrimaryButton
import com.cryptocompare.ui.components.AppTextField
import com.cryptocompare.ui.components.ExchangeSheet
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.bgPrimary
import com.cryptocompare.ui.theme.cryptoError
import com.cryptocompare.ui.theme.textSecondary
import com.cryptocompare.ui.theme.textTertiary

/**
 * Позиция: где куплено, сколько и по какой средней цене.
 *
 * Средняя, а не цена каждой покупки: ручной портфель ведут как «0.42 BTC по
 * 72 000», и заставлять вводить лоты значило бы превратить его в бухгалтерию.
 *
 * Биржа идёт первой: от неё зависит, по какой цене позиция оценивается, и
 * без неё портфель брал бы лучший bid с площадки, где монеты у пользователя нет.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PositionEditScreen(
    onDone: () -> Unit,
    viewModel: PositionEditViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.portfolio_save_failed)

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onDone()
    }

    LaunchedEffect(uiState.saveFailed) {
        if (uiState.saveFailed) snackbarHostState.showSnackbar(saveFailedMessage)
    }

    if (uiState.showExchangePicker) {
        ExchangeSheet(
            title = stringResource(R.string.portfolio_exchange),
            anyExchangeLabel = stringResource(R.string.portfolio_exchange_best),
            providers = uiState.exchanges,
            selectedProviderId = uiState.providerId,
            onSelect = viewModel::onExchangeSelected,
            onDismiss = viewModel::onExchangeDismissed,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.ticker.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.portfolio_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.bgPrimary),
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
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimensions.Padding.screen),
            verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
        ) {
            Text(
                text = stringResource(R.string.portfolio_exchange),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.textSecondary,
            )
            ExchangeField(
                value = uiState.exchangeName ?: stringResource(R.string.portfolio_exchange_best),
                onClick = viewModel::onExchangeClick,
                enabled = !uiState.isSaving,
            )
            Text(
                text =
                    stringResource(
                        if (uiState.providerId != null) {
                            R.string.portfolio_exchange_pinned_hint
                        } else {
                            R.string.portfolio_exchange_best_hint
                        },
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textTertiary,
            )

            Text(
                text = stringResource(R.string.portfolio_amount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.textSecondary,
            )
            AppTextField(
                value = uiState.amountInput,
                onValueChange = viewModel::onAmountChange,
                placeholder = stringResource(R.string.portfolio_amount_hint),
                keyboardType = KeyboardType.Decimal,
                isError = uiState.amountInput.isNotEmpty() && uiState.amount == null,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.portfolio_buy_price),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.textSecondary,
            )
            AppTextField(
                value = uiState.priceInput,
                onValueChange = viewModel::onPriceChange,
                placeholder = stringResource(R.string.portfolio_buy_price_hint),
                keyboardType = KeyboardType.Decimal,
                isError = uiState.priceInput.isNotEmpty() && uiState.price == null,
                modifier = Modifier.fillMaxWidth(),
            )

            AppPrimaryButton(
                text = stringResource(R.string.portfolio_save),
                onClick = viewModel::onSave,
                enabled = uiState.canSave,
            )

            if (uiState.isExisting) {
                TextButton(
                    onClick = viewModel::onDelete,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.portfolio_delete),
                        color = MaterialTheme.colorScheme.cryptoError,
                    )
                }
            }
        }
    }
}
