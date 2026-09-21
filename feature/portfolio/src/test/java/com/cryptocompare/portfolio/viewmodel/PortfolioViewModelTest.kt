package com.cryptocompare.portfolio.viewmodel

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.domain.repository.PortfolioRepository
import com.cryptocompare.domain.usecase.portfolio.CalculatePortfolioUseCase
import com.cryptocompare.domain.usecase.portfolio.ObservePortfolioPricesUseCase
import com.cryptocompare.domain.usecase.portfolio.ObservePortfolioUseCase
import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.portfolio.viewmodel.portfolioviewmodel.PortfolioViewModel
import com.cryptocompare.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val portfolioRepository: PortfolioRepository = mockk()
    private val cryptoCompareRepository: CryptoCompareRepository = mockk()

    @Test
    fun `until the database answers the screen is loading, not empty`() =
        runTest {
            val positions = MutableSharedFlow<List<PortfolioPosition>>()
            every { portfolioRepository.observePositions() } returns positions
            every { cryptoCompareRepository.observeSellPrices(any()) } returns MutableStateFlow(emptyMap())

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.loading)

            positions.emit(emptyList())
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.loading)
            assertTrue(state.holdings.isEmpty())
            assertNull(state.summary)
        }

    @Test
    fun `positions follow the database and get priced`() =
        runTest {
            val positions = MutableSharedFlow<List<PortfolioPosition>>()
            every { portfolioRepository.observePositions() } returns positions
            every { cryptoCompareRepository.observeSellPrices(setOf(SYMBOL_ID)) } returns
                MutableStateFlow(mapOf(SYMBOL_ID to 80_000.0))

            val viewModel = createViewModel()
            advanceUntilIdle()

            positions.emit(listOf(POSITION))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            val holding = state.holdings.single()
            assertEquals(POSITION, holding.position)
            assertEquals(33_600.0, holding.currentValue!!, DELTA)
            assertEquals(3_360.0, state.summary!!.profit, DELTA)
        }

    @Test
    fun `a price tick repriced the portfolio without touching the positions`() =
        runTest {
            // цены приходят из каталога, который двигает сокет: позиции при этом
            // в базе не меняются, и без пересчёта итог застыл бы на входном
            val prices = MutableStateFlow(mapOf(SYMBOL_ID to 80_000.0))
            every { portfolioRepository.observePositions() } returns MutableStateFlow(listOf(POSITION))
            every { cryptoCompareRepository.observeSellPrices(setOf(SYMBOL_ID)) } returns prices

            val viewModel = createViewModel()
            advanceUntilIdle()

            prices.value = mapOf(SYMBOL_ID to 60_000.0)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(25_200.0, state.holdings.single().currentValue!!, DELTA)
            assertEquals(-5_040.0, state.summary!!.profit, DELTA)
        }

    private fun createViewModel() =
        PortfolioViewModel(
            observePortfolioUseCase = ObservePortfolioUseCase(portfolioRepository),
            observePortfolioPricesUseCase = ObservePortfolioPricesUseCase(cryptoCompareRepository),
            calculatePortfolioUseCase = CalculatePortfolioUseCase(),
        )

    private companion object {
        const val SYMBOL_ID = 1L
        const val DELTA = 1e-9

        val POSITION =
            PortfolioPosition(
                symbolId = SYMBOL_ID,
                ticker = "BTCUSDT",
                amount = 0.42,
                buyPrice = 72_000.0,
                updatedAtMillis = 1_700_000_000_000L,
            )
    }
}
