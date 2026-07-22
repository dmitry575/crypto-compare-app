package com.cryptocompare.pairs.ui.screens.mainScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.accentSoft

/**
 * Значок пары: базовая валюта на подложке. Логотипов монет у бэкенда нет,
 * а generic-иконка на каждой строке не помогает их различать — тикер помогает.
 */
@Composable
internal fun PairBadge(
    base: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(Dimensions.Crypto.pairBadge)
                .background(
                    color = MaterialTheme.colorScheme.accentSoft,
                    shape = RoundedCornerShape(Dimensions.Radius.sm),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            // ведущий «$» у мем-токенов не различает пары, а место занимает
            text =
                base
                    .removePrefix(PairsConstants.MainScreen.TICKER_PREFIX)
                    .take(PairsConstants.MainScreen.BADGE_MAX_CHARS),
            style = NumericType.Caption,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}
