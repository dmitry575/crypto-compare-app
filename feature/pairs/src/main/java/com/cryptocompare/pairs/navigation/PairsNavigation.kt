package com.cryptocompare.pairs.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.cryptocompare.pairs.ui.screens.comparisonScreen.ComparisonScreen
import com.cryptocompare.pairs.ui.screens.detailScreen.DetailsScreen
import com.cryptocompare.pairs.ui.screens.mainScreen.MainScreen

/**
 * Вложенный граф каталога. Контроллер приходит снаружи: на всё приложение
 * один [NavHostController], иначе у каждой фичи свой back stack.
 */
fun NavGraphBuilder.pairsNavigation(
    navController: NavHostController,
    onProfileClick: () -> Unit,
    onSignInClick: () -> Unit,
    onAddToPortfolio: (symbolId: Long, ticker: String, price: Double?, providerId: Int?) -> Unit,
) {
    navigation<PairsRoute.Graph>(startDestination = PairsRoute.Main) {
        composable<PairsRoute.Main> {
            MainScreen(
                onPairClick = { ticker, symbolId -> navController.navigate(PairsRoute.Details(ticker, symbolId)) },
                onProfileClick = onProfileClick,
                onSignInClick = onSignInClick,
            )
        }

        composable<PairsRoute.Details> {
            DetailsScreen(
                onBack = { navController.popBackStack() },
                onCompareClick = { ticker, symbolId ->
                    navController.navigate(PairsRoute.Comparison(ticker, symbolId))
                },
                onAddToPortfolio = onAddToPortfolio,
            )
        }

        composable<PairsRoute.Comparison> {
            ComparisonScreen(onBack = { navController.popBackStack() })
        }
    }
}
