package com.cryptocompare.helpers

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder

/**
 * Переход с очисткой стека до [popUpToRoute] включительно: заставка и онбординг
 * не должны оставаться под экраном, куда из них ушли. Маршруты — типизированные
 * `@Serializable`-объекты фич.
 */
fun <T : Any> NavHostController.navigateAndClearStack(
    route: T,
    popUpToRoute: Any,
    builder: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(route) {
        popUpTo(popUpToRoute) { inclusive = true }
        launchSingleTop = true
        builder()
    }
}
