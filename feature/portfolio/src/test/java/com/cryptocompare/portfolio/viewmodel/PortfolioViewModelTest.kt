package com.cryptocompare.portfolio.viewmodel

import com.cryptocompare.domain.repository.PortfolioRepository
import com.cryptocompare.domain.usecase.portfolio.ObservePortfolioUseCase
import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.portfolio.viewmodel.portfolioviewmodel.PortfolioViewModel
import com.cryptocompare.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: PortfolioRepository = mockk()

    @Test
    fun `until the database answers the screen is loading, not empty`() =
        runTest {
            val positions = MutableSharedFlow<List<PortfolioPosition>>()
            every { repository.observePositions() } returns positions

            val viewModel = PortfolioViewModel(ObservePortfolioUseCase(repository))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.loading)

            positions.emit(emptyList())
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.loading)
            assertEquals(emptyList<PortfolioPosition>(), viewModel.uiState.value.positions)
        }

    @Test
    fun `positions follow the database`() =
        runTest {
            val positions = MutableSharedFlow<List<PortfolioPosition>>()
            every { repository.observePositions() } returns positions

            val viewModel = PortfolioViewModel(ObservePortfolioUseCase(repository))
            advanceUntilIdle()

            positions.emit(listOf(POSITION))
            advanceUntilIdle()

            assertEquals(listOf(POSITION), viewModel.uiState.value.positions)
        }

    private companion object {
        val POSITION =
            PortfolioPosition(
                symbolId = 1L,
                ticker = "BTCUSDT",
                amount = 0.42,
                buyPrice = 72_000.0,
                updatedAtMillis = 1_700_000_000_000L,
            )
    }
}
