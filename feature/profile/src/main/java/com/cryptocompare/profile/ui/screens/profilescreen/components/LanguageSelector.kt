package com.cryptocompare.profile.ui.screens.profilescreen.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.model.settings.AppLanguage
import com.cryptocompare.profile.R
import com.cryptocompare.ui.components.AppSegmentedControl

@Composable
internal fun LanguageSelector(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels =
        AppLanguage.entries.associateWith { language ->
            stringResource(language.labelRes())
        }

    AppSegmentedControl(
        options = AppLanguage.entries,
        selected = selected,
        onSelect = onSelect,
        label = { labels.getValue(it) },
        modifier = modifier,
    )
}

private fun AppLanguage.labelRes(): Int =
    when (this) {
        AppLanguage.SYSTEM -> R.string.profile_language_system
        AppLanguage.ENGLISH -> R.string.profile_language_english
        AppLanguage.RUSSIAN -> R.string.profile_language_russian
    }
