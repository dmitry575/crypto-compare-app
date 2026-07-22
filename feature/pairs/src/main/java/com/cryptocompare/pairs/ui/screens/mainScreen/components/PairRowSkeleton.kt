package com.cryptocompare.pairs.ui.screens.mainScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.shimmerBase

/** Заглушка строки каталога: повторяет раскладку [PairRow], чтобы список не прыгал. */
@Composable
fun PairRowSkeleton(
    modifier: Modifier = Modifier,
    rowHeight: Dp? = null,
) {
    val rowModifier = if (rowHeight != null) modifier.height(rowHeight) else modifier
    val shimmer = MaterialTheme.colorScheme.shimmerBase
    val shape = RoundedCornerShape(Dimensions.Radius.sm)

    Row(
        modifier =
            rowModifier
                .fillMaxWidth()
                .padding(
                    horizontal = Dimensions.Padding.listItemHorizontal,
                    vertical = Dimensions.Padding.listItemVertical,
                ),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(Dimensions.Crypto.pairBadge)
                    .background(shimmer, shape),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimensions.Spacing.xxs),
        ) {
            Box(
                modifier =
                    Modifier
                        .width(PairsConstants.MainScreen.skeletonTickerWidth)
                        .height(PairsConstants.MainScreen.skeletonTickerHeight)
                        .background(shimmer, shape),
            )
            Box(
                modifier =
                    Modifier
                        .width(PairsConstants.MainScreen.skeletonSubtitleWidth)
                        .height(PairsConstants.MainScreen.skeletonSubtitleHeight)
                        .background(shimmer, shape),
            )
        }

        Box(
            modifier =
                Modifier
                    .width(PairsConstants.MainScreen.skeletonPriceWidth)
                    .height(PairsConstants.MainScreen.skeletonPriceHeight)
                    .background(shimmer, shape),
        )

        Box(modifier = Modifier.size(Dimensions.TouchTarget.min))
    }
}
