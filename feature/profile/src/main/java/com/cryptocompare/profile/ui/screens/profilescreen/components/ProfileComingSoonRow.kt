package com.cryptocompare.profile.ui.screens.profilescreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.cryptocompare.profile.R
import com.cryptocompare.profile.util.ProfileConstants
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.textTertiary

/**
 * Слот под будущую настройку: место в вёрстке занято, чтобы экран не пришлось
 * пересобирать, когда настройка появится.
 */
@Composable
internal fun ProfileComingSoonRow(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val disabledColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = ProfileConstants.Row.DISABLED_ALPHA)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = Dimensions.TouchTarget.min)
                .padding(
                    horizontal = Dimensions.Padding.listItemHorizontal,
                    vertical = Dimensions.Padding.listItemVertical,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = disabledColor,
            modifier = Modifier.size(Dimensions.IconSize.md),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = disabledColor,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.profile_coming_soon),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.textTertiary,
        )
    }
}
