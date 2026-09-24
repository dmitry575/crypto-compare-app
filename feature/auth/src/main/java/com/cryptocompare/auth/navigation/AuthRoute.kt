package com.cryptocompare.auth.navigation

import kotlinx.serialization.Serializable

/** Маршруты авторизации: заставка, онбординг, вход, регистрация, сброс пароля. */
sealed interface AuthRoute {
    /** Вложенный граф фичи: успешный вход снимает со стека его целиком. */
    @Serializable
    data object Graph : AuthRoute

    @Serializable
    data object Splash : AuthRoute

    @Serializable
    data object Onboarding : AuthRoute

    @Serializable
    data object Login : AuthRoute

    @Serializable
    data object Register : AuthRoute

    @Serializable
    data object ForgotPassword : AuthRoute
}
