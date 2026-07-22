package com.cryptocompare.pairs.ui.screens.detailScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptocompare.pairs.R
import com.cryptocompare.pairs.ui.screens.detailScreen.components.CandlestickChart
import com.cryptocompare.pairs.ui.screens.detailScreen.components.ExchangeInfoCard
import com.cryptocompare.pairs.ui.screens.detailScreen.components.ExchangeSelector
import com.cryptocompare.pairs.ui.screens.detailScreen.components.SpreadBar
import com.cryptocompare.pairs.ui.screens.detailScreen.components.TimeframeSelector
import com.cryptocompare.pairs.viewmodel.detailViewModel.DetailsViewModel
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.bgPrimary
import com.cryptocompare.ui.theme.textSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState.value
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.ticker.uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.bgPrimary,
                    ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.pair_detail_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        when {
            state.loading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.bgPrimary)
                            .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.exchanges.isEmpty() -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.bgPrimary)
                            .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.pair_detail_no_exchanges, state.ticker),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            else -> {
                // отступ по краям висит на каждом блоке, а не на колонке: график
                // тогда занимает всю ширину экрана, а не остаток после полей
                val contentPadding = Modifier.padding(horizontal = Dimensions.Padding.screenHorizontal)

                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.bgPrimary)
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = Dimensions.Padding.screenVertical),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.lg),
                ) {
                    // Разброс между биржами — главный элемент экрана
                    SpreadBar(
                        exchanges = state.exchanges,
                        modifier = contentPadding,
                    )

                    // Выбор биржи (если больше одной)
                    if (state.exchanges.size > 1) {
                        ExchangeSelector(
                            exchanges = state.exchanges,
                            selectedIndex = state.selectedExchangeIndex,
                            onExchangeSelected = viewModel::onExchangeSelected,
                            modifier = contentPadding,
                        )
                    }

                    // Масштаб графика: переключение не трогает выбор биржи
                    TimeframeSelector(
                        selected = state.timeframe,
                        onTimeframeSelected = viewModel::onTimeframeSelected,
                        modifier = contentPadding,
                    )

                    // Свечной график, общий по всем биржам (агрегат) — во всю ширину
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(Dimensions.Crypto.chartLarge),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            state.chartLoading -> CircularProgressIndicator()

                            state.candles.isEmpty() ->
                                Text(
                                    text = stringResource(R.string.pair_detail_no_chart_data),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.textSecondary,
                                )

                            else ->
                                CandlestickChart(
                                    candles = state.candles,
                                    timeframe = state.timeframe,
                                    modifier = Modifier.fillMaxSize(),
                                )
                        }
                    }

                    // Карточка с информацией о бирже
                    state.selectedExchange?.let { exchange ->
                        ExchangeInfoCard(exchange = exchange, modifier = contentPadding)
                    }

                    Spacer(modifier = Modifier.height(Dimensions.Spacing.md))
                }
            }
        }
    }
}
