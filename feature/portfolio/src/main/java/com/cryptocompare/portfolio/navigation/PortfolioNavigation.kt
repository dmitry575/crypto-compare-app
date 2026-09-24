package com.cryptocompare.portfolio.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.cryptocompare.portfolio.ui.screens.portfolioscreen.PortfolioScreen
import com.cryptocompare.portfolio.ui.screens.positioneditscreen.PositionEditScreen

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
    navigation<PortfolioRoute.Graph>(startDestination = PortfolioRoute.Main) {
        composable<PortfolioRoute.Main> {
            PortfolioScreen(
                onPositionClick = { symbolId, ticker ->
                    navController.navigate(PortfolioRoute.PositionEdit(symbolId, ticker))
                },
                onOpenPairs = onOpenPairs,
            )
        }

        composable<PortfolioRoute.PositionEdit> {
            PositionEditScreen(onDone = { navController.popBackStack() })
        }
    }
}
