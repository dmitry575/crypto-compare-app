package com.cryptocompare.pairs.ui.screens.detailScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cryptocompare.helpers.toPriceString
import com.cryptocompare.pairs.R
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.textSecondary
import com.cryptocompare.ui.theme.textTertiary

@Composable
fun PriceSummaryRow(
    minPrice: Double,
    maxPrice: Double,
    exchangeCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PairsConstants.DetailScreen.labelGap),
        ) {
            Text(
                text = stringResource(R.string.pair_detail_lowest_ask),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textTertiary,
            )
            Text(
                text = minPrice.toPriceString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(PairsConstants.DetailScreen.labelGap),
        ) {
            Text(
                text = stringResource(R.string.pair_detail_highest_bid),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textTertiary,
            )
            Text(
                text = maxPrice.toPriceString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.textSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    Text(
        text = pluralStringResource(R.plurals.pair_detail_exchange_count, exchangeCount, exchangeCount),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.textTertiary,
    )
}
