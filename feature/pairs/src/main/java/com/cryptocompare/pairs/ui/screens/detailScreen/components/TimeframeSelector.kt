package com.cryptocompare.pairs.ui.screens.detailScreen.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.pairs.R
import com.cryptocompare.ui.components.AppChipRow
import com.cryptocompare.ui.components.AppFilterChip

@Composable
fun TimeframeSelector(
    selected: ChartTimeframe,
    onTimeframeSelected: (ChartTimeframe) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppChipRow(modifier = modifier) {
        ChartTimeframe.entries.forEach { timeframe ->
            AppFilterChip(
                label = stringResource(timeframe.labelRes()),
                selected = timeframe == selected,
                onClick = { onTimeframeSelected(timeframe) },
            )
        }
    }
}

private fun ChartTimeframe.labelRes(): Int =
    when (this) {
        ChartTimeframe.M15 -> R.string.timeframe_m15
        ChartTimeframe.H1 -> R.string.timeframe_h1
        ChartTimeframe.H4 -> R.string.timeframe_h4
        ChartTimeframe.D1 -> R.string.timeframe_d1
        ChartTimeframe.W1 -> R.string.timeframe_w1
    }
