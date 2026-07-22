package com.cryptocompare.pairs.ui.screens.detailScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.pairs.R
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.borderPrimary
import com.cryptocompare.ui.theme.textSecondary

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
            val isSelected = timeframe == selected
            val shape = RoundedCornerShape(Dimensions.Radius.full)

            Text(
                text = stringResource(timeframe.labelRes()),
                style = MaterialTheme.typography.labelMedium,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.textSecondary
                    },
                modifier =
                    Modifier
                        .background(
                            color =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.background
                                },
                            shape = shape,
                        ).border(
                            width = Dimensions.Border.thin,
                            color =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.borderPrimary
                                },
                            shape = shape,
                        ).clickable { onTimeframeSelected(timeframe) }
                        .padding(
                            horizontal = Dimensions.Padding.chipHorizontal,
                            vertical = Dimensions.Padding.chipVertical,
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
