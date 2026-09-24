package com.cryptocompare.portfolio.navigation

import com.cryptocompare.portfolio.util.PortfolioConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class PortfolioRouteTest {
    @Test
    fun `position form fields are the keys its view model reads`() {
        // навигация кладёт аргументы в SavedStateHandle под именами полей: переименуй
        // поле — и форма молча откроется без символа
        val descriptor = PortfolioRoute.PositionEdit.serializer().descriptor

        assertEquals(
            listOf(
                PortfolioConstants.Navigation.SYMBOL_ID_ARG,
                PortfolioConstants.Navigation.TICKER_ARG,
                PortfolioConstants.Navigation.PRICE_ARG,
                PortfolioConstants.Navigation.PROVIDER_ID_ARG,
            ),
            (0 until descriptor.elementsCount).map(descriptor::getElementName),
        )
    }
}
