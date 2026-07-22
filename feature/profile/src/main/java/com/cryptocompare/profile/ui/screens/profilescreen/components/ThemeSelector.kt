package com.cryptocompare.profile.ui.screens.profilescreen.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.model.settings.ThemePreference
import com.cryptocompare.profile.R
import com.cryptocompare.ui.components.AppSegmentedControl

@Composable
internal fun ThemeSelector(
    selected: ThemePreference,
    onSelect: (ThemePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels =
        ThemePreference.entries.associateWith { preference ->
            stringResource(preference.labelRes())
        }

    AppSegmentedControl(
        options = ThemePreference.entries,
        selected = selected,
        onSelect = onSelect,
        label = { labels.getValue(it) },
        modifier = modifier,
    )
}

private fun ThemePreference.labelRes(): Int =
    when (this) {
        ThemePreference.SYSTEM -> R.string.profile_theme_system
        ThemePreference.LIGHT -> R.string.profile_theme_light
        ThemePreference.DARK -> R.string.profile_theme_dark
    }
