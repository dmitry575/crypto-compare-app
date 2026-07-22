package com.cryptocompare.auth.navigation

sealed class AuthScreens(
    val route: String,
) {
    object LoginScreen : AuthScreens("login")

    object RegisterScreen : AuthScreens("register")

    object ForgotPasswordScreen : AuthScreens("forgot_password")

    object SplashScreen : AuthScreens("splash")
}
