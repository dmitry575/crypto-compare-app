package com.cryptocompare.profile.ui.screens.profilescreen.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.model.settings.AppLanguage
import com.cryptocompare.profile.R
import com.cryptocompare.ui.components.AppSegmentedControl
import com.cryptocompare.ui.locale.AppLocale

/**
 * Выбор языка. Состояние держит сам AppCompat (в отличие от темы, у которой
 * свой DataStore), поэтому ViewModel тут не нужен: читаем текущий выбор и
 * применяем новый напрямую. Смена языка пересоздаёт активность.
 *
 * Названия языков — эндонимы (English, Русский): язык называет себя одинаково
 * в любой локали, переводится только «Системный».
 */
@Composable
internal fun LanguageSelector(modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf(AppLocale.current()) }
    val labels =
        AppLanguage.entries.associateWith { language ->
            stringResource(language.labelRes())
        }

    AppSegmentedControl(
        options = AppLanguage.entries,
        selected = selected,
        onSelect = { language ->
            selected = language
            AppLocale.apply(language)
        },
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
