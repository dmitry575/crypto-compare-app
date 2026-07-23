package com.cryptocompare.ui.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.cryptocompare.model.settings.AppLanguage

/**
 * Язык приложения поверх per-app locales AppCompat.
 *
 * Выбор хранит и применяет сам AppCompatDelegate (persist через сервис
 * AppLocalesMetadataHolderService в манифесте), поэтому своего DataStore тут
 * нет — источник истины один, и он же переживает перезапуск. В отличие от темы,
 * язык меняет ресурсы на уровне Activity, а не рисуется Compose.
 */
object AppLocale {
    /** Текущий выбор — из того, что запомнил фреймворк. */
    fun current(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) {
            AppLanguage.SYSTEM
        } else {
            AppLanguage.fromTag(locales.get(0)?.language)
        }
    }

    /** Применить язык. SYSTEM отдаёт пустой список — «как в системе». */
    fun apply(language: AppLanguage) {
        val locales =
            if (language == AppLanguage.SYSTEM) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(language.localeTag)
            }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
