package com.cryptocompare.pairs.ui.screens.mainScreen.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.cryptocompare.helpers.toCompactPriceString
import com.cryptocompare.model.symbol.PairUiItem
import com.cryptocompare.pairs.R
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.borderPrimary
import com.cryptocompare.ui.theme.textSecondary

@Composable
fun PairRow(
    pair: PairUiItem,
    modifier: Modifier = Modifier,
    rowHeight: Dp? = null,
    isFavourite: Boolean = false,
    onFavouriteClick: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    val rowModifier =
        if (rowHeight != null) {
            modifier.height(rowHeight)
        } else {
            modifier
        }
    Surface(
        modifier =
            rowModifier
                .fillMaxWidth()
                .border(
                    width = Dimensions.Border.card,
                    color = MaterialTheme.colorScheme.borderPrimary,
                    shape = MaterialTheme.shapes.medium,
                ).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Dimensions.Padding.listItemHorizontal,
                        vertical = Dimensions.Padding.listItemVertical,
                    ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pair.ticker,
                modifier = Modifier.weight(PairsConstants.MainScreen.TICKER_COLUMN_WEIGHT),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.weight(PairsConstants.MainScreen.PRICE_COLUMN_WEIGHT),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = pair.maxPrice.toCompactPriceString(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pair.minPrice.toCompactPriceString(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textSecondary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onFavouriteClick) {
                Icon(
                    imageVector = if (isFavourite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription =
                        stringResource(
                            if (isFavourite) {
                                R.string.pairs_remove_from_favorites
                            } else {
                                R.string.pairs_add_to_favorites
                            },
                        ),
                    tint = if (isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
