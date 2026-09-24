package com.cryptocompare.profile.navigation

import kotlinx.serialization.Serializable

/** Маршруты профиля. */
sealed interface ProfileRoute {
    /** Вложенный граф фичи: шапка каталога открывает его, а не экран напрямую. */
    @Serializable
    data object Graph : ProfileRoute

    @Serializable
    data object Main : ProfileRoute

    @Serializable
    data object ChangePassword : ProfileRoute
}
