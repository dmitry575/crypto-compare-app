package com.cryptocompare.pairs.navigation

import com.cryptocompare.pairs.util.PairsConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class PairsRouteTest {
    @Test
    fun `route fields are the keys the view models read`() {
        // навигация кладёт аргументы в SavedStateHandle под именами полей: переименуй
        // поле — и экран пары молча откроется пустым, без тикера
        listOf(PairsRoute.Details.serializer().descriptor, PairsRoute.Comparison.serializer().descriptor)
            .forEach { descriptor ->
                assertEquals(
                    listOf(PairsConstants.Navigation.TICKER_ARG, PairsConstants.Navigation.SYMBOL_ID_ARG),
                    (0 until descriptor.elementsCount).map(descriptor::getElementName),
                )
            }
    }
}
