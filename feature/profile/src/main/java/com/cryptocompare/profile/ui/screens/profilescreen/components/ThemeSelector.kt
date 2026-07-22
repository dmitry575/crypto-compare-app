package com.cryptocompare.profile.ui.screens.profilescreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.model.settings.ThemePreference
import com.cryptocompare.profile.R
import com.cryptocompare.ui.theme.Dimensions

@Composable
internal fun ThemeSelector(
    selected: ThemePreference,
    onSelect: (ThemePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.Padding.listItemHorizontal),
        verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.sm),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemePreference.entries.forEachIndexed { index, preference ->
                SegmentedButton(
                    selected = preference == selected,
                    onClick = { onSelect(preference) },
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemePreference.entries.size,
                        ),
                ) {
                    Text(text = stringResource(preference.labelRes()))
                }
            }
        }
    }
}

private fun ThemePreference.labelRes(): Int =
    when (this) {
        ThemePreference.SYSTEM -> R.string.profile_theme_system
        ThemePreference.LIGHT -> R.string.profile_theme_light
        ThemePreference.DARK -> R.string.profile_theme_dark
    }
