package com.cryptocompare.auth.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.cryptocompare.auth.ui.screens.forgotpasswordscreen.ForgotPasswordScreen
import com.cryptocompare.auth.ui.screens.loginscreen.LoginScreen
import com.cryptocompare.auth.ui.screens.onboardingscreen.OnboardingScreen
import com.cryptocompare.auth.ui.screens.registerscreen.RegisterScreen
import com.cryptocompare.auth.ui.screens.splashscreen.SplashScreen
import com.cryptocompare.helpers.navigateAndClearStack

/**
 * Вложенный граф авторизации. Контроллер приходит снаружи: на всё приложение
 * один [NavHostController], иначе у каждой фичи свой back stack.
 */
fun NavGraphBuilder.authNavigation(
    navController: NavHostController,
    onAuthenticated: () -> Unit,
) {
    navigation(
        route = AuthDestination.ROUTE,
        startDestination = AuthScreens.SplashScreen.route,
    ) {
        composable(AuthScreens.LoginScreen.route) {
            LoginScreen(
                onRegisterClick = { navController.navigate(AuthScreens.RegisterScreen.route) },
                onForgotPasswordClick = {
                    navController.navigate(AuthScreens.ForgotPasswordScreen.route)
                },
                onAuthenticated = onAuthenticated,
            )
        }

        composable(AuthScreens.ForgotPasswordScreen.route) {
            ForgotPasswordScreen(onBackToLogin = { navController.popBackStack() })
        }

        composable(AuthScreens.RegisterScreen.route) {
            RegisterScreen(
                onLoginClick = { navController.navigate(AuthScreens.LoginScreen.route) },
                onAuthenticated = onAuthenticated,
            )
        }

        composable(AuthScreens.SplashScreen.route) {
            SplashScreen(
                onNavigateHome = onAuthenticated,
                onNavigateLogin = {
                    navController.navigateAndClearStack(AuthScreens.LoginScreen.route, AuthScreens.SplashScreen.route)
                },
                onNavigateOnboarding = {
                    navController.navigateAndClearStack(
                        AuthScreens.OnboardingScreen.route,
                        AuthScreens.SplashScreen.route,
                    )
                },
            )
        }

        composable(AuthScreens.OnboardingScreen.route) {
            // после онбординга возвращаемся на сплеш: он уже знает про вход
            // и разведёт на каталог или логин, а флаг к этому моменту записан
            OnboardingScreen(
                onDone = {
                    navController.navigateAndClearStack(
                        AuthScreens.SplashScreen.route,
                        AuthScreens.OnboardingScreen.route,
                    )
                },
            )
        }
    }
}
