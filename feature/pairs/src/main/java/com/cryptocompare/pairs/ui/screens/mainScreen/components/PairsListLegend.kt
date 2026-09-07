package com.cryptocompare.pairs.ui.screens.mainScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.pairs.R
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.OverlineType
import com.cryptocompare.ui.theme.textTertiary

/**
 * Подписи к числам строки каталога — один раз над списком, а не в каждой строке.
 *
 * В строке на подписи нет места: «Спред 0.42% · Об. 98.75M» это ~180dp против
 * ~112dp, которые остаются левой колонке. Здесь же они стоят один раз и не
 * платят за себя шириной сорок раз подряд.
 *
 * Легенда живёт **над** `LazyColumn`, а не внутри него: экран сопоставляет
 * индексы видимых элементов списка тикерам, чтобы подписаться на них по
 * сокету, и служебный элемент внутри списка сдвинул бы это соответствие.
 */
@Composable
internal fun PairsListLegend(
    showVolume: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.Padding.listItemHorizontal),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text =
                stringResource(
                    if (showVolume) R.string.pairs_legend_spread_volume else R.string.pairs_legend_spread,
                ),
            style = OverlineType,
            color = MaterialTheme.colorScheme.textTertiary,
        )
        Text(
            text = stringResource(R.string.pairs_legend_price_change),
            style = OverlineType,
            color = MaterialTheme.colorScheme.textTertiary,
        )
    }
}
