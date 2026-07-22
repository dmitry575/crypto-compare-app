package com.cryptocompare.profile.navigation

sealed class ProfileScreens(
    val route: String,
) {
    object ProfileScreen : ProfileScreens("profile_main")

    object ChangePasswordScreen : ProfileScreens("change_password")
}
