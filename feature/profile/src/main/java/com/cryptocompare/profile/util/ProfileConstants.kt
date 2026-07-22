package com.cryptocompare.profile.util

/**
 * Константы фичи profile. Размеры берутся из
 * [com.cryptocompare.ui.theme.Dimensions] и сюда не дублируются.
 */
object ProfileConstants {
    object Avatar {
        /** Заглушка, когда ни displayName, ни email не пришли. */
        const val FALLBACK_INITIAL = "?"
    }

    object Row {
        /** Приглушение пунктов, которые ещё не работают («скоро»). */
        const val DISABLED_ALPHA = 0.4f
    }

    object ChangePassword {
        /** Ведущая иконка полей ввода — как на экранах авторизации. */
        const val FIELD_ICON = "🔒"
    }
}
