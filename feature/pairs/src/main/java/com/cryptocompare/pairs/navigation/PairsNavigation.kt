package com.cryptocompare.pairs.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.cryptocompare.pairs.ui.screens.detailScreen.DetailsScreen
import com.cryptocompare.pairs.ui.screens.mainScreen.MainScreen
import com.cryptocompare.pairs.util.PairsConstants

/**
 * Вложенный граф каталога. Контроллер приходит снаружи: на всё приложение
 * один [NavHostController], иначе у каждой фичи свой back stack.
 */
fun NavGraphBuilder.pairsNavigation(
    navController: NavHostController,
    onProfileClick: () -> Unit,
) {
    navigation(
        route = PairsDestination.ROUTE,
        startDestination = PairsScreens.MainScreen.route,
    ) {
        composable(route = PairsScreens.MainScreen.route) {
            MainScreen(
                onPairClick = { ticker ->
                    navController.navigate(PairsScreens.DetailsScreen.createRoute(ticker))
                },
                onProfileClick = onProfileClick,
            )
        }

        composable(
            route = PairsScreens.DetailsScreen.route,
            arguments =
                listOf(
                    navArgument(PairsConstants.Navigation.TICKER_ARG) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
        ) {
            DetailsScreen(onBack = { navController.popBackStack() })
        }
    }
}
