package com.cryptocompare.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.cryptocompare.auth.navigation.AuthDestination
import com.cryptocompare.auth.navigation.AuthScreens
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
 *
 * Вход необязателен: запуск всегда приводит в каталог, а экран авторизации
 * открывается поверх него — из профиля или когда гость просит избранное.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val openSignIn = { navController.navigate(AuthScreens.LoginScreen.route) }

    NavHost(
        navController = navController,
        startDestination = AuthDestination.ROUTE,
    ) {
        authNavigation(
            navController = navController,
            onReady = {
                navController.navigateAndClearStack(PairsDestination.ROUTE, AuthDestination.ROUTE)
            },
            // вход открыт поверх каталога или профиля: закрываем его целиком и
            // возвращаемся туда, откуда пришли
            onAuthenticated = { navController.popBackStack(AuthDestination.ROUTE, inclusive = true) },
        )

        pairsNavigation(
            navController = navController,
            onProfileClick = { navController.navigate(ProfileDestination.ROUTE) },
            onSignInClick = openSignIn,
        )

        profileNavigation(
            navController = navController,
            onSignInClick = openSignIn,
        )
    }
}
