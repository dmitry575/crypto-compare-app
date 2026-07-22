package com.cryptocompare.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.cryptocompare.auth.navigation.AuthDestination
import com.cryptocompare.auth.navigation.authNavigation
import com.cryptocompare.helpers.navigateAndClearStack
import com.cryptocompare.pairs.navigation.PairsDestination
import com.cryptocompare.pairs.navigation.pairsNavigation
import com.cryptocompare.profile.navigation.ProfileDestination
import com.cryptocompare.profile.navigation.profileNavigation

/**
 * Единственный [rememberNavController] в приложении: фичи отдают вложенные графы
 * и получают контроллер параметром. Переходы между фичами живут здесь — фичи
 * друг о друге не знают.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AuthDestination.ROUTE,
    ) {
        authNavigation(
            navController = navController,
            onAuthenticated = {
                navController.navigateAndClearStack(PairsDestination.ROUTE, AuthDestination.ROUTE)
            },
        )

        pairsNavigation(
            navController = navController,
            onProfileClick = { navController.navigate(ProfileDestination.ROUTE) },
        )

        profileNavigation(
            navController = navController,
            // после выхода каталог за спиной не нужен: чистим стек до самого низа
            onSignedOut = {
                navController.navigateAndClearStack(AuthDestination.ROUTE, PairsDestination.ROUTE)
            },
        )
    }
}
