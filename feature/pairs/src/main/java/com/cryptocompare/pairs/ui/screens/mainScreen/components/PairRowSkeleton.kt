package com.cryptocompare.pairs.ui.screens.mainScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.borderPrimary
import com.cryptocompare.ui.theme.shimmerBase

@Composable
fun PairRowSkeleton(
    modifier: Modifier = Modifier,
    rowHeight: Dp? = null,
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
                ),
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
            Box(
                modifier =
                    Modifier
                        .weight(PairsConstants.MainScreen.TICKER_COLUMN_WEIGHT)
                        .height(PairsConstants.MainScreen.skeletonTickerHeight)
                        .background(MaterialTheme.colorScheme.shimmerBase, MaterialTheme.shapes.small),
            )

            Row(
                modifier = Modifier.weight(PairsConstants.MainScreen.PRICE_COLUMN_WEIGHT),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
            ) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(PairsConstants.MainScreen.skeletonPriceHeight)
                            .background(MaterialTheme.colorScheme.shimmerBase, MaterialTheme.shapes.small),
                )
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(PairsConstants.MainScreen.skeletonPriceHeight)
                            .background(MaterialTheme.colorScheme.shimmerBase, MaterialTheme.shapes.small),
                )
            }
        }
    }
}
