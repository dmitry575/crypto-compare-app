package com.cryptocompare.pairs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.cryptocompare.pairs.R
import com.cryptocompare.pairs.util.formatLastUpdate
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.bgSunk
import com.cryptocompare.ui.theme.textSecondary

/**
 * Цены на экране больше не живые.
 *
 * Список при обрыве остаётся на месте — это кеш каталога, и он полезнее пустого
 * экрана, — но выглядит он ровно так же, как живой. Полоска говорит, что числа
 * замерли, и называет время, на котором они замерли.
 *
 * Появляется не сразу: короткий разрыв чинится сам, и мигающая полоска
 * раздражала бы сильнее, чем помогала.
 */
@Composable
internal fun StaleDataNotice(
    lastUpdateMillis: Long?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimensions.Radius.card))
                .background(MaterialTheme.colorScheme.bgSunk)
                .padding(start = Dimensions.Padding.cardMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text =
                if (lastUpdateMillis == null) {
                    stringResource(R.string.pairs_stale_prices)
                } else {
                    stringResource(R.string.pairs_stale_prices_at, formatLastUpdate(lastUpdateMillis))
                },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textSecondary,
        )

        TextButton(onClick = onRefresh) {
            Text(
                text = stringResource(R.string.pairs_stale_refresh),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
