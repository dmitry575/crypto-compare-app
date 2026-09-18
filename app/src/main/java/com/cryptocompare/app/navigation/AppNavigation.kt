package com.cryptocompare.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cryptocompare.auth.navigation.AuthDestination
import com.cryptocompare.auth.navigation.AuthScreens
import com.cryptocompare.auth.navigation.authNavigation
import com.cryptocompare.helpers.navigateAndClearStack
import com.cryptocompare.pairs.navigation.PairsDestination
import com.cryptocompare.pairs.navigation.PairsScreens
import com.cryptocompare.pairs.navigation.pairsNavigation
import com.cryptocompare.portfolio.navigation.PortfolioScreens
import com.cryptocompare.portfolio.navigation.portfolioNavigation
import com.cryptocompare.profile.navigation.ProfileDestination
import com.cryptocompare.profile.navigation.profileNavigation

/**
 * Единственный [rememberNavController] в приложении: фичи отдают вложенные графы
 * и получают контроллер параметром. Переходы между фичами живут здесь — фичи
 * друг о друге не знают.
 *
 * Вход необязателен: запуск всегда приводит в каталог, а экран авторизации
 * открывается поверх него — из профиля или когда гость просит избранное.
 *
 * Нижняя панель видна только на корнях вкладок: на деталях пары, в форме
 * позиции и в профиле она отнимала бы место у экрана, куда пользователь пришёл.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val selectedTab =
        when (backStackEntry?.destination?.route) {
            PairsScreens.MainScreen.route -> AppTab.PAIRS
            PortfolioScreens.PortfolioScreen.route -> AppTab.PORTFOLIO
            else -> null
        }

    val openSignIn = { navController.navigate(AuthScreens.LoginScreen.route) }

    // Якорь — корневой граф, а не граф каталога. Каталог после заставки сам лежит
    // в корне стека, и если прыгать к нему, его же состояние и сохранялось бы, и
    // перетиралось вкладкой портфеля: вернувшись, пользователь терял бы фильтры и
    // прокрутку. От корня обе вкладки — соседи, и у каждой своё сохранённое.
    val openTab: (AppTab) -> Unit = { tab ->
        navController.navigate(tab.graphRoute) {
            popUpTo(navController.graph.id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // «Назад» с корня портфеля ведёт в каталог, а не закрывает приложение: под
    // вкладкой портфеля в стеке ничего нет, и без этого выход был бы неожиданным
    BackHandler(enabled = selectedTab == AppTab.PORTFOLIO) { openTab(AppTab.PAIRS) }

    Scaffold(
        // системные отступы по-прежнему считают экраны фич; отсюда — только высота панели
        contentWindowInsets = WindowInsets(0),
        bottomBar = { selectedTab?.let { tab -> AppBottomBar(selected = tab, onSelect = openTab) } },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AuthDestination.ROUTE,
            modifier =
                Modifier
                    .padding(padding)
                    // полоску навигации системы уже закрыла панель: экран под ней
                    // не должен отступать от неё второй раз
                    .then(
                        if (selectedTab != null) {
                            Modifier.consumeWindowInsets(WindowInsets.navigationBars)
                        } else {
                            Modifier
                        },
                    ),
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
                onAddToPortfolio = { symbolId, ticker, price ->
                    navController.navigate(PortfolioScreens.PositionEditScreen.createRoute(symbolId, ticker, price))
                },
            )

            portfolioNavigation(
                navController = navController,
                onOpenPairs = { openTab(AppTab.PAIRS) },
            )

            profileNavigation(
                navController = navController,
                onSignInClick = openSignIn,
            )
        }
    }
}
