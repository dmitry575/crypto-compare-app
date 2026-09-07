package com.cryptocompare.pairs.ui.screens.detailScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.cryptocompare.helpers.toPriceString
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.textTertiary

@Composable
fun PriceBlock(
    label: String,
    price: Double,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimensions.Spacing.xxs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textTertiary,
        )
        Text(
            text = price.toPriceString(),
            style = NumericType.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
