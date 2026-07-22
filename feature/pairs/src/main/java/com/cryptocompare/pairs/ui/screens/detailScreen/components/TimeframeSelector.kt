package com.cryptocompare.pairs.ui.screens.detailScreen.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.pairs.R
import com.cryptocompare.ui.theme.Dimensions

@Composable
fun TimeframeSelector(
    selected: ChartTimeframe,
    onTimeframeSelected: (ChartTimeframe) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.sm),
    ) {
        ChartTimeframe.entries.forEach { timeframe ->
            FilterChip(
                selected = timeframe == selected,
                onClick = { onTimeframeSelected(timeframe) },
                label = { Text(stringResource(timeframe.labelRes())) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
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
