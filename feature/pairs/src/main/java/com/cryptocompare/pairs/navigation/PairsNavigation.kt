package com.cryptocompare.pairs.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.cryptocompare.pairs.ui.screens.comparisonScreen.ComparisonScreen
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
                onPairClick = { ticker, symbolId ->
                    navController.navigate(PairsScreens.DetailsScreen.createRoute(ticker, symbolId))
                },
                onProfileClick = onProfileClick,
            )
        }

        composable(
            route = PairsScreens.DetailsScreen.route,
            arguments = pairArguments(),
        ) {
            DetailsScreen(
                onBack = { navController.popBackStack() },
                onCompareClick = { ticker, symbolId ->
                    navController.navigate(PairsScreens.ComparisonScreen.createRoute(ticker, symbolId))
                },
            )
        }

        composable(
            route = PairsScreens.ComparisonScreen.route,
            arguments = pairArguments(),
        ) {
            ComparisonScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun pairArguments() =
    listOf(
        navArgument(PairsConstants.Navigation.TICKER_ARG) {
            type = NavType.StringType
            defaultValue = ""
        },
        navArgument(PairsConstants.Navigation.SYMBOL_ID_ARG) {
            type = NavType.LongType
            defaultValue = PairsConstants.Navigation.NO_SYMBOL_ID
        },
    )
