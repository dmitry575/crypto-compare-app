package com.cryptocompare.portfolio.navigation

import com.cryptocompare.portfolio.util.PortfolioConstants
import kotlinx.serialization.Serializable

/**
 * Маршруты портфеля. ViewModel читают аргументы из `SavedStateHandle` по имени
 * поля, поэтому имена полей и [PortfolioConstants.Navigation] обязаны совпадать
 * — это закреплено тестом `PortfolioRouteTest`.
 */
sealed interface PortfolioRoute {
    /** Вложенный граф фичи: вкладка «Портфель» переходит сюда. */
    @Serializable
    data object Graph : PortfolioRoute

    @Serializable
    data object Main : PortfolioRoute

    /**
     * Форма позиции. [price] и [providerId] — подсказка для новой позиции: ask и
     * биржа, открытые на экране пары; у существующей форма берёт её собственные.
     *
     * [price] строкой: у навигации нет типа для `Double`, а `Float` потерял бы
     * знаки у цен вроде 0.00002717.
     */
    @Serializable
    data class PositionEdit(
        val symbolId: Long,
        val ticker: String,
        val price: String? = null,
        val providerId: Int? = null,
    ) : PortfolioRoute
}
