package com.cryptocompare.profile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.cryptocompare.profile.ui.screens.changepasswordscreen.ChangePasswordScreen
import com.cryptocompare.profile.ui.screens.profilescreen.ProfileScreen

/**
 * Вложенный граф профиля. Контроллер приходит снаружи: на всё приложение
 * один [NavHostController], иначе у каждой фичи свой back stack.
 */
fun NavGraphBuilder.profileNavigation(
    navController: NavHostController,
    onSignedOut: () -> Unit,
) {
    navigation(
        route = ProfileDestination.ROUTE,
        startDestination = ProfileScreens.ProfileScreen.route,
    ) {
        composable(ProfileScreens.ProfileScreen.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSignedOut = onSignedOut,
                onChangePasswordClick = {
                    navController.navigate(ProfileScreens.ChangePasswordScreen.route)
                },
            )
        }

        composable(ProfileScreens.ChangePasswordScreen.route) {
            ChangePasswordScreen(onBack = { navController.popBackStack() })
        }
    }
}
