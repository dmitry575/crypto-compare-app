package com.cryptocompare.pairs.navigation

import com.cryptocompare.pairs.util.PairsConstants
import kotlinx.serialization.Serializable

/**
 * Маршруты фичи пар. Аргументы — поля классов, и компилятор не даст открыть
 * экран пары без тикера: раньше маршрут собирался строкой, и опечатка в имени
 * аргумента молча давала пустой экран.
 *
 * ViewModel читают аргументы из `SavedStateHandle` по имени поля, поэтому имена
 * полей и [PairsConstants.Navigation] обязаны совпадать — это закреплено тестом
 * `PairsRouteTest`.
 */
sealed interface PairsRoute {
    /** Вложенный граф фичи: вкладка «Пары» переходит сюда, а не на экран каталога. */
    @Serializable
    data object Graph : PairsRoute

    @Serializable
    data object Main : PairsRoute

    /** [symbolId] — один набор сетей тикера; `null` — открыт тикер целиком. */
    @Serializable
    data class Details(
        val ticker: String,
        val symbolId: Long? = null,
    ) : PairsRoute

    @Serializable
    data class Comparison(
        val ticker: String,
        val symbolId: Long? = null,
    ) : PairsRoute
}
