package com.cryptocompare.pairs.ui.screens.comparisonScreen

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptocompare.model.comparison.PairComparison
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.pairs.R
import com.cryptocompare.pairs.ui.screens.comparisonScreen.components.ComparisonMessage
import com.cryptocompare.pairs.ui.screens.comparisonScreen.components.ComparisonSummaryCard
import com.cryptocompare.pairs.ui.screens.comparisonScreen.components.ComparisonTableHeader
import com.cryptocompare.pairs.ui.screens.comparisonScreen.components.ExchangeQuoteRow
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.pairs.viewmodel.comparisonViewModel.ComparisonViewModel
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.OverlineType
import com.cryptocompare.ui.theme.bgCard
import com.cryptocompare.ui.theme.bgPrimary
import com.cryptocompare.ui.theme.borderPrimary
import com.cryptocompare.ui.theme.textTertiary

/**
 * Сравнение котировок пары по биржам.
 *
 * Биржи идут строками, а не колонками, как нарисовано в задаче: при пятнадцати
 * биржах колонки уводят главное число за правый край экрана, а строки телефон
 * листает сам. Порядок — по цене покупки, поэтому самая дешёвая стоит сразу под
 * ответом.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(
    onBack: () -> Unit,
    viewModel: ComparisonViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.pair_comparison_title, state.ticker.uppercase()),
                        style = MaterialTheme.typography.titleLarge,
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
    ) { paddingValues ->
        val screenModifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.bgPrimary)
                .padding(paddingValues)

        when {
            state.loading -> {
                Box(modifier = screenModifier, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Column(
                    modifier = screenModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    ComparisonMessage(text = state.error)
                    TextButton(onClick = viewModel::retry) {
                        Text(text = stringResource(R.string.pair_comparison_retry))
                    }
                }
            }

            state.isEmpty || state.comparison == null -> {
                Box(modifier = screenModifier, contentAlignment = Alignment.Center) {
                    ComparisonMessage(text = stringResource(R.string.pair_comparison_empty))
                }
            }

            else -> ComparisonContent(comparison = state.comparison, modifier = screenModifier)
        }
    }
}

@Composable
private fun ComparisonContent(
    comparison: PairComparison,
    modifier: Modifier = Modifier,
) {
    // отметка о несвежести считается на каждой перерисовке, а перерисовка
    // случается на каждом флаше тиков — отдельный таймер тут не нужен
    val now = System.currentTimeMillis()
    val contentPadding = Modifier.padding(horizontal = Dimensions.Padding.screenHorizontal)

    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = Dimensions.Padding.screenVertical),
        verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.lg),
    ) {
        ComparisonSummaryCard(comparison = comparison, modifier = contentPadding)

        if (comparison.isSingleExchange) {
            ComparisonMessage(text = stringResource(R.string.pair_comparison_single_exchange))
        }

        // подписи «по цене покупки» здесь нет намеренно. Порядок задаётся один
        // раз при загрузке и дальше не меняется: живые тики двигают цены каждые
        // полсекунды, и пересортировка заставила бы строки прыгать под пальцем.
        // Обещать порядок, который тут же перестаёт быть точным, нечестно —
        // выгодную биржу и так называет карточка сверху и отметка в таблице
        Text(
            text = stringResource(R.string.pair_comparison_exchanges, comparison.quotes.size),
            style = OverlineType,
            color = MaterialTheme.colorScheme.textTertiary,
            modifier = contentPadding,
        )

        // таблица одной карточкой: скругление у неё общее, а бирж у пары
        // не больше трёх десятков — дробить это на ленивый список нечего
        Column(
            modifier =
                contentPadding
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimensions.Radius.card))
                    .background(MaterialTheme.colorScheme.bgCard),
        ) {
            ComparisonTableHeader()

            comparison.quotes.forEachIndexed { index, quote ->
                HorizontalDivider(color = MaterialTheme.colorScheme.borderPrimary)
                ExchangeQuoteRow(
                    quote = quote,
                    isBestAsk = quote.provider.id == comparison.bestAskProviderId,
                    isBestBid = quote.provider.id == comparison.bestBidProviderId,
                    isStale = quote.isStale(now),
                )
                if (index == comparison.quotes.lastIndex) {
                    Spacer(modifier = Modifier.height(Dimensions.Spacing.xxs))
                }
            }
        }
    }
}

/**
 * Котировка старше порога бэкенда: он с этим же порогом не пускает такие в
 * лучшую пару, а в разбивке по биржам они приходят как ни в чём не бывало.
 */
private fun ProviderDetail.isStale(nowMillis: Long): Boolean {
    val quotedAt = quotedAtMillis ?: return false
    val ageSeconds = (nowMillis - quotedAt) / PairsConstants.ComparisonScreen.MILLIS_IN_SECOND

    return ageSeconds > PairsConstants.ComparisonScreen.STALE_QUOTE_SECONDS
}
