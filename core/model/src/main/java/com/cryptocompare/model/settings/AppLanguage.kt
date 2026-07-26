package com.cryptocompare.model.settings

/**
 * Язык интерфейса. [SYSTEM] означает «следовать языку устройства» — как
 * [ThemePreference.SYSTEM] для темы: без него из выбранного языка нельзя
 * вернуться к системному.
 *
 * Новый язык добавляется одним элементом: значение здесь + `values-<tag>` +
 * строка-название. [localeTag] — тег для java.util.Locale, пустой у SYSTEM.
 */
enum class AppLanguage(
    val localeTag: String,
) {
    SYSTEM(""),
    ENGLISH("en"),
    RUSSIAN("ru"),
    ;

    companion object {
        val DEFAULT: AppLanguage = SYSTEM
    }
}
