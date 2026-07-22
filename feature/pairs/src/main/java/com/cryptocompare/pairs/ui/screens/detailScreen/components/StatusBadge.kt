package com.cryptocompare.pairs.ui.screens.detailScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cryptocompare.model.provider.ProviderStatus
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.statusActive
import com.cryptocompare.ui.theme.statusInactive

@Composable
fun StatusBadge(status: ProviderStatus) {
    val color =
        when (status) {
            ProviderStatus.Enabled -> MaterialTheme.colorScheme.statusActive
            ProviderStatus.Disables -> MaterialTheme.colorScheme.statusInactive
            ProviderStatus.None -> MaterialTheme.colorScheme.statusInactive
        }
    val label =
        when (status) {
            ProviderStatus.Enabled -> "Active"
            ProviderStatus.Disables -> "Inactive"
            ProviderStatus.None -> "Unknown"
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.xs),
    ) {
        Icon(
            imageVector = Icons.Filled.Circle,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(PairsConstants.DetailScreen.statusDotSize),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
