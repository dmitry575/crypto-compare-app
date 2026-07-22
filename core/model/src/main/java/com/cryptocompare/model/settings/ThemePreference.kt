package com.cryptocompare.model.settings

/** Выбор темы пользователем. [SYSTEM] означает «следовать системной настройке». */
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        val DEFAULT: ThemePreference = SYSTEM
    }
}
