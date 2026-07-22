package com.cryptocompare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.cryptoSuccess
import com.cryptocompare.ui.theme.textTertiary
import com.cryptocompare.ui.util.UiConstants

/** Строка одного требования к паролю. Используется только внутри [PasswordRequirements]. */
@Composable
internal fun RequirementItem(
    text: String,
    met: Boolean,
) {
    val color =
        if (met) {
            MaterialTheme.colorScheme.cryptoSuccess
        } else {
            MaterialTheme.colorScheme.textTertiary
        }
    val icon =
        if (met) {
            UiConstants.PasswordRequirements.MET_ICON
        } else {
            UiConstants.PasswordRequirements.UNMET_ICON
        }

    Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.sm)) {
        Text(text = icon, color = color, style = MaterialTheme.typography.bodySmall)
        Text(text = text, color = color, style = MaterialTheme.typography.bodySmall)
    }
}
