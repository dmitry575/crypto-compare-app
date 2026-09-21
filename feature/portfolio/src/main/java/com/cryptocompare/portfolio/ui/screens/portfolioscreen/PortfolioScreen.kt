package com.cryptocompare.portfolio.ui.screens.portfolioscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptocompare.portfolio.R
import com.cryptocompare.portfolio.ui.screens.portfolioscreen.components.PortfolioEmptyState
import com.cryptocompare.portfolio.ui.screens.portfolioscreen.components.PortfolioPositionRow
import com.cryptocompare.portfolio.ui.screens.portfolioscreen.components.PortfolioSummaryCard
import com.cryptocompare.portfolio.util.PortfolioConstants
import com.cryptocompare.portfolio.viewmodel.portfolioviewmodel.PortfolioViewModel
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.bgCard
import com.cryptocompare.ui.theme.bgPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    onPositionClick: (symbolId: Long, ticker: String) -> Unit,
    onOpenPairs: () -> Unit,
    viewModel: PortfolioViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    // Подписки сокета живут ровно столько, сколько экран на виду: вкладка
    // сохраняет свою ViewModel, и по её жизни каталог ждал бы свои подписки
    // до закрытия приложения.
    DisposableEffect(viewModel) {
        viewModel.onScreenShown()
        onDispose { viewModel.onScreenHidden() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.portfolio_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.bgPrimary,
                    ),
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.bgPrimary)
                    .padding(paddingValues),
        ) {
            when {
                uiState.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                uiState.holdings.isEmpty() -> PortfolioEmptyState(onOpenPairs = onOpenPairs)

                else ->
                    LazyColumn(
                        contentPadding =
                            PaddingValues(
                                horizontal = Dimensions.Padding.screenHorizontal,
                                vertical = Dimensions.Padding.screenVertical,
                            ),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.sm),
                    ) {
                        // итог едет вместе со списком: у портфеля из десятка позиций
                        // закреплённая карточка отъедала бы у него пол-экрана
                        item(key = PortfolioConstants.Screen.SUMMARY_KEY) {
                            PortfolioSummaryCard(
                                summary = uiState.summary,
                                modifier = Modifier.padding(bottom = Dimensions.Gap.sm),
                            )
                        }

                        items(uiState.holdings, key = { holding -> holding.position.symbolId }) { holding ->
                            PortfolioPositionRow(
                                holding = holding,
                                onClick = {
                                    onPositionClick(holding.position.symbolId, holding.position.ticker)
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(Dimensions.Radius.card))
                                        .background(MaterialTheme.colorScheme.bgCard),
                            )
                        }
                    }
            }
        }
    }
}
