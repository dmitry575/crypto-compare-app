package com.cryptocompare.pairs.ui.screens.mainScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.cryptocompare.pairs.R
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.textSecondary

@Composable
fun ListHeader() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.Padding.listItemHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.pairs_column_name),
            modifier = Modifier.weight(PairsConstants.MainScreen.TICKER_COLUMN_WEIGHT),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.textSecondary,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            modifier = Modifier.weight(PairsConstants.MainScreen.PRICE_COLUMN_WEIGHT),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.pairs_column_max),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.textSecondary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
            )
            Text(
                text = stringResource(R.string.pairs_column_min),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.textSecondary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
            )
        }

        // резервируем место под колонку-звёздочку в строках, чтобы заголовки совпадали
        Spacer(modifier = Modifier.width(Dimensions.TouchTarget.min))
    }
}
