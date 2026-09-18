package com.cryptocompare.portfolio.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.cryptocompare.portfolio.ui.screens.portfolioscreen.PortfolioScreen
import com.cryptocompare.portfolio.ui.screens.positioneditscreen.PositionEditScreen
import com.cryptocompare.portfolio.util.PortfolioConstants

/**
 * Вложенный граф портфеля. Контроллер приходит снаружи: на всё приложение один
 * [NavHostController], иначе у каждой фичи свой back stack.
 *
 * Форма позиции открывается и отсюда, и с экрана пары — туда маршрут отдаёт
 * `AppNavigation`, фича пар о портфеле не знает.
 */
fun NavGraphBuilder.portfolioNavigation(
    navController: NavHostController,
    onOpenPairs: () -> Unit,
) {
    navigation(
        route = PortfolioDestination.ROUTE,
        startDestination = PortfolioScreens.PortfolioScreen.route,
    ) {
        composable(PortfolioScreens.PortfolioScreen.route) {
            PortfolioScreen(
                onPositionClick = { symbolId, ticker ->
                    navController.navigate(PortfolioScreens.PositionEditScreen.createRoute(symbolId, ticker))
                },
                onOpenPairs = onOpenPairs,
            )
        }

        composable(
            route = PortfolioScreens.PositionEditScreen.route,
            arguments =
                listOf(
                    navArgument(PortfolioConstants.Navigation.SYMBOL_ID_ARG) { type = NavType.LongType },
                    navArgument(PortfolioConstants.Navigation.TICKER_ARG) { type = NavType.StringType },
                    navArgument(PortfolioConstants.Navigation.PRICE_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
        ) {
            PositionEditScreen(onDone = { navController.popBackStack() })
        }
    }
}
