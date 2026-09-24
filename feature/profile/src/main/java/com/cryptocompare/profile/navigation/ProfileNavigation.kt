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
    onSignInClick: () -> Unit,
) {
    navigation<ProfileRoute.Graph>(startDestination = ProfileRoute.Main) {
        composable<ProfileRoute.Main> {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSignInClick = onSignInClick,
                onChangePasswordClick = { navController.navigate(ProfileRoute.ChangePassword) },
            )
        }

        composable<ProfileRoute.ChangePassword> {
            ChangePasswordScreen(onBack = { navController.popBackStack() })
        }
    }
}
