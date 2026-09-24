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
    onReady: () -> Unit,
    onAuthenticated: () -> Unit,
) {
    navigation<AuthRoute.Graph>(startDestination = AuthRoute.Splash) {
        composable<AuthRoute.Login> {
            LoginScreen(
                onRegisterClick = { navController.navigate(AuthRoute.Register) },
                onForgotPasswordClick = { navController.navigate(AuthRoute.ForgotPassword) },
                onAuthenticated = onAuthenticated,
            )
        }

        composable<AuthRoute.ForgotPassword> {
            ForgotPasswordScreen(onBackToLogin = { navController.popBackStack() })
        }

        composable<AuthRoute.Register> {
            RegisterScreen(
                onLoginClick = { navController.navigate(AuthRoute.Login) },
                onAuthenticated = onAuthenticated,
            )
        }

        composable<AuthRoute.Splash> {
            SplashScreen(
                onReady = onReady,
                onNavigateOnboarding = {
                    navController.navigateAndClearStack(AuthRoute.Onboarding, AuthRoute.Splash)
                },
            )
        }

        composable<AuthRoute.Onboarding> {
            // после онбординга возвращаемся на сплеш: он уже знает про вход
            // и разведёт на каталог или логин, а флаг к этому моменту записан
            OnboardingScreen(
                onDone = { navController.navigateAndClearStack(AuthRoute.Splash, AuthRoute.Onboarding) },
            )
        }
    }
}
