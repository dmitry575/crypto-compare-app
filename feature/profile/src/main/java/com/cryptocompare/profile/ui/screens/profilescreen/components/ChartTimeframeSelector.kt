package com.cryptocompare.profile.ui.screens.profilescreen.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.profile.R
import com.cryptocompare.ui.components.AppSegmentedControl

@Composable
internal fun ChartTimeframeSelector(
    selected: ChartTimeframe,
    onSelect: (ChartTimeframe) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = ChartTimeframe.entries.associateWith { timeframe -> stringResource(timeframe.labelRes()) }

    AppSegmentedControl(
        options = ChartTimeframe.entries,
        selected = selected,
        onSelect = onSelect,
        label = { labels.getValue(it) },
        modifier = modifier,
    )
}

private fun ChartTimeframe.labelRes(): Int =
    when (this) {
        ChartTimeframe.M15 -> R.string.profile_timeframe_m15
        ChartTimeframe.H1 -> R.string.profile_timeframe_h1
        ChartTimeframe.H4 -> R.string.profile_timeframe_h4
        ChartTimeframe.D1 -> R.string.profile_timeframe_d1
        ChartTimeframe.W1 -> R.string.profile_timeframe_w1
    }
