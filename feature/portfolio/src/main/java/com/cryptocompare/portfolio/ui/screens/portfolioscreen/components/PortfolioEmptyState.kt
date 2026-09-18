package com.cryptocompare.portfolio.ui.screens.portfolioscreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.cryptocompare.portfolio.R
import com.cryptocompare.ui.components.AppPrimaryButton
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.textSecondary

/**
 * Пустой портфель объясняет, как его наполнить: позиция добавляется с экрана
 * пары, где уже есть и символ, и текущая цена, — отдельный поиск по каталогу
 * здесь повторял бы то, что уже умеет вкладка «Пары».
 */
@Composable
internal fun PortfolioEmptyState(
    onOpenPairs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Dimensions.Padding.screen),
        verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.portfolio_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.portfolio_empty_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textSecondary,
            textAlign = TextAlign.Center,
        )
        AppPrimaryButton(
            text = stringResource(R.string.portfolio_open_pairs),
            onClick = onOpenPairs,
        )
    }
}
