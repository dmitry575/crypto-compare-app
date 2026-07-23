package com.cryptocompare.model.settings

/**
 * Язык интерфейса. [SYSTEM] означает «следовать языку устройства» — как
 * [ThemePreference.SYSTEM] для темы: без него из выбранного языка нельзя
 * вернуться к системному.
 *
 * Новый язык добавляется одним элементом: значение здесь + `values-<tag>` +
 * строка-название. [localeTag] — тег для AppCompatDelegate, пустой у SYSTEM.
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

        /** Обратное сопоставление: тег локали, который вернул фреймворк, → выбор. */
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.localeTag.isNotEmpty() && tag?.startsWith(it.localeTag) == true }
                ?: SYSTEM
    }
}
