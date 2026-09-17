package com.cryptocompare.pairs.ui.screens.detailScreen.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.model.chart.ChartIndicator
import com.cryptocompare.pairs.R
import com.cryptocompare.ui.components.AppChipRow
import com.cryptocompare.ui.components.AppFilterChip

/**
 * Какие скользящие средние показывать поверх свечей.
 *
 * Чипы, а не сегменты: средние включаются независимо друг от друга, и обычный
 * случай — ни одной или одна.
 */
@Composable
fun IndicatorSelector(
    selected: Set<ChartIndicator>,
    onIndicatorToggled: (ChartIndicator) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppChipRow(modifier = modifier) {
        ChartIndicator.entries.forEach { indicator ->
            AppFilterChip(
                label = stringResource(indicator.labelRes()),
                selected = indicator in selected,
                onClick = { onIndicatorToggled(indicator) },
            )
        }
    }
}

private fun ChartIndicator.labelRes(): Int =
    when (this) {
        ChartIndicator.SMA_20 -> R.string.chart_indicator_sma_20
        ChartIndicator.SMA_50 -> R.string.chart_indicator_sma_50
        ChartIndicator.EMA_20 -> R.string.chart_indicator_ema_20
        ChartIndicator.EMA_50 -> R.string.chart_indicator_ema_50
    }
