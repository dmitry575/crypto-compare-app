package com.cryptocompare.portfolio.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.cryptocompare.domain.repository.PortfolioRepository
import com.cryptocompare.domain.usecase.portfolio.DeletePortfolioPositionUseCase
import com.cryptocompare.domain.usecase.portfolio.GetPortfolioPositionUseCase
import com.cryptocompare.domain.usecase.portfolio.SavePortfolioPositionUseCase
import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.portfolio.util.PortfolioConstants
import com.cryptocompare.portfolio.viewmodel.positioneditviewmodel.PositionEditViewModel
import com.cryptocompare.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PositionEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: PortfolioRepository = mockk(relaxed = true)

    @Test
    fun `a new position starts from the suggested price`() =
        runTest {
            coEvery { repository.getPosition(SYMBOL_ID) } returns null

            val viewModel = createViewModel(price = "76852.0")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("76852", state.priceInput)
            assertEquals("", state.amountInput)
            assertFalse(state.isExisting)
        }

    @Test
    fun `an existing position opens with its own numbers, not the suggestion`() =
        runTest {
            coEvery { repository.getPosition(SYMBOL_ID) } returns EXISTING

            val viewModel = createViewModel(price = "76852.0")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("0.42", state.amountInput)
            assertEquals("72000", state.priceInput)
            assertTrue(state.isExisting)
        }

    @Test
    fun `saving needs a positive amount and a price`() =
        runTest {
            coEvery { repository.getPosition(SYMBOL_ID) } returns null
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.canSave)

            viewModel.onAmountChange("0")
            viewModel.onPriceChange("72 000")
            assertFalse(viewModel.uiState.value.canSave)

            viewModel.onAmountChange("0,42")
            assertTrue(viewModel.uiState.value.canSave)
        }

    @Test
    fun `save stores the parsed numbers and closes the form`() =
        runTest {
            coEvery { repository.getPosition(SYMBOL_ID) } returns null
            coEvery { repository.savePosition(any()) } returns Result.success(Unit)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmountChange("0,42")
            viewModel.onPriceChange("72 000")
            viewModel.onSave()
            advanceUntilIdle()

            coVerify(exactly = 1) {
                repository.savePosition(
                    match { it.symbolId == SYMBOL_ID && it.amount == 0.42 && it.buyPrice == 72_000.0 },
                )
            }
            assertTrue(viewModel.uiState.value.isDone)
        }

    @Test
    fun `a failed save keeps the form open and says so`() =
        runTest {
            coEvery { repository.getPosition(SYMBOL_ID) } returns null
            coEvery { repository.savePosition(any()) } returns Result.failure(IllegalStateException("disk full"))
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmountChange("1")
            viewModel.onPriceChange("1")
            viewModel.onSave()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isDone)
            assertTrue(viewModel.uiState.value.saveFailed)
        }

    @Test
    fun `delete removes the position and closes the form`() =
        runTest {
            coEvery { repository.getPosition(SYMBOL_ID) } returns EXISTING
            coEvery { repository.deletePosition(SYMBOL_ID) } returns Result.success(Unit)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onDelete()
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.deletePosition(SYMBOL_ID) }
            assertTrue(viewModel.uiState.value.isDone)
        }

    private fun createViewModel(price: String? = null): PositionEditViewModel =
        PositionEditViewModel(
            savedStateHandle =
                SavedStateHandle(
                    buildMap {
                        put(PortfolioConstants.Navigation.SYMBOL_ID_ARG, SYMBOL_ID)
                        put(PortfolioConstants.Navigation.TICKER_ARG, "BTCUSDT")
                        if (price != null) put(PortfolioConstants.Navigation.PRICE_ARG, price)
                    },
                ),
            getPortfolioPositionUseCase = GetPortfolioPositionUseCase(repository),
            savePortfolioPositionUseCase = SavePortfolioPositionUseCase(repository),
            deletePortfolioPositionUseCase = DeletePortfolioPositionUseCase(repository),
        )

    private companion object {
        const val SYMBOL_ID = 1L

        val EXISTING =
            PortfolioPosition(
                symbolId = SYMBOL_ID,
                ticker = "BTCUSDT",
                amount = 0.42,
                buyPrice = 72_000.0,
                updatedAtMillis = 1_700_000_000_000L,
            )
    }
}
