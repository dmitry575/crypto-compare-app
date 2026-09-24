package com.cryptocompare.portfolio.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.cryptocompare.domain.repository.PortfolioRepository
import com.cryptocompare.domain.usecase.pairs.GetProvidersUseCase
import com.cryptocompare.domain.usecase.pairs.GetTickerDetailUseCase
import com.cryptocompare.domain.usecase.portfolio.DeletePortfolioPositionUseCase
import com.cryptocompare.domain.usecase.portfolio.GetPortfolioPositionUseCase
import com.cryptocompare.domain.usecase.portfolio.SavePortfolioPositionUseCase
import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.model.provider.Provider
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.model.provider.ProviderStatus
import com.cryptocompare.model.ticker.TickerDetail
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PositionEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: PortfolioRepository = mockk(relaxed = true)
    private val getTickerDetail: GetTickerDetailUseCase = mockk()
    private val getProviders: GetProvidersUseCase = mockk()

    @Before
    fun setUp() {
        coEvery { getTickerDetail("BTCUSDT", SYMBOL_ID) } returns
            Result.success(TickerDetail("BTCUSDT", listOf(exchange(BYBIT_PROVIDER), exchange(OKX_PROVIDER)), SYMBOL_ID))
        coEvery { getProviders() } returns Result.success(listOf(BYBIT_PROVIDER, OKX_PROVIDER, HTX_PROVIDER))
    }

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

    @Test
    fun `a new position starts on the exchange open on the pair screen`() =
        runTest {
            // цена подсказана с этой биржи — скорее всего, там и купили
            coEvery { repository.getPosition(SYMBOL_ID) } returns null

            val viewModel = createViewModel(price = "76852.0", providerId = 5)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(5, state.providerId)
            assertEquals("bybit", state.exchangeName)
        }

    @Test
    fun `the choice is limited to exchanges that trade the symbol`() =
        runTest {
            // у htx этой пары нет: закреплённая за ней позиция не получила бы цену никогда
            coEvery { repository.getPosition(SYMBOL_ID) } returns null

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(listOf(BYBIT_PROVIDER, OKX_PROVIDER), viewModel.uiState.value.exchanges)
        }

    @Test
    fun `an existing position opens on its own exchange, not the suggested one`() =
        runTest {
            coEvery { repository.getPosition(SYMBOL_ID) } returns EXISTING.copy(providerId = 7, exchangeName = "okx")

            val viewModel = createViewModel(price = "76852.0", providerId = 5)
            advanceUntilIdle()

            assertEquals(7, viewModel.uiState.value.providerId)
            assertEquals("okx", viewModel.uiState.value.exchangeName)
        }

    @Test
    fun `a position from before the exchange field keeps the best price`() =
        runTest {
            coEvery { repository.getPosition(SYMBOL_ID) } returns EXISTING

            val viewModel = createViewModel(providerId = 5)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.providerId)
            assertNull(viewModel.uiState.value.exchangeName)
        }

    @Test
    fun `offline the chosen exchange is still named from the directory`() =
        runTest {
            coEvery { repository.getPosition(SYMBOL_ID) } returns null
            coEvery { getTickerDetail(any(), any()) } returns Result.failure(IllegalStateException("offline"))

            val viewModel = createViewModel(providerId = 5)
            advanceUntilIdle()

            // в шторке — только она: выбрать биржу, про которую не знаешь, торгуется
            // ли там пара, значило бы остаться без цены
            assertEquals(listOf(BYBIT_PROVIDER), viewModel.uiState.value.exchanges)
            assertEquals("bybit", viewModel.uiState.value.exchangeName)
        }

    @Test
    fun `save stores the chosen exchange`() =
        runTest {
            coEvery { repository.getPosition(SYMBOL_ID) } returns null
            coEvery { repository.savePosition(any()) } returns Result.success(Unit)
            val viewModel = createViewModel(providerId = 5)
            advanceUntilIdle()

            viewModel.onExchangeClick()
            viewModel.onExchangeSelected(7)
            viewModel.onAmountChange("1")
            viewModel.onPriceChange("1")
            viewModel.onSave()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showExchangePicker)
            coVerify(exactly = 1) { repository.savePosition(match { it.providerId == 7 }) }
        }

    @Test
    fun `choosing no exchange goes back to the best price`() =
        runTest {
            coEvery { repository.getPosition(SYMBOL_ID) } returns null
            coEvery { repository.savePosition(any()) } returns Result.success(Unit)
            val viewModel = createViewModel(providerId = 5)
            advanceUntilIdle()

            viewModel.onExchangeSelected(null)
            viewModel.onAmountChange("1")
            viewModel.onPriceChange("1")
            viewModel.onSave()
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.savePosition(match { it.providerId == null }) }
        }

    private fun createViewModel(
        price: String? = null,
        providerId: Int? = null,
    ): PositionEditViewModel =
        PositionEditViewModel(
            savedStateHandle =
                SavedStateHandle(
                    buildMap {
                        put(PortfolioConstants.Navigation.SYMBOL_ID_ARG, SYMBOL_ID)
                        put(PortfolioConstants.Navigation.TICKER_ARG, "BTCUSDT")
                        if (price != null) put(PortfolioConstants.Navigation.PRICE_ARG, price)
                        if (providerId != null) put(PortfolioConstants.Navigation.PROVIDER_ID_ARG, providerId)
                    },
                ),
            getPortfolioPositionUseCase = GetPortfolioPositionUseCase(repository),
            savePortfolioPositionUseCase = SavePortfolioPositionUseCase(repository),
            deletePortfolioPositionUseCase = DeletePortfolioPositionUseCase(repository),
            getTickerDetailUseCase = getTickerDetail,
            getProvidersUseCase = getProviders,
        )

    private fun exchange(provider: Provider) =
        ProviderDetail(provider = provider, priceSell = 81_100.0, priceBuy = 81_000.0)

    private companion object {
        const val SYMBOL_ID = 1L

        val BYBIT_PROVIDER = Provider(id = 5, name = "bybit", referralUrl = null, status = ProviderStatus.Enabled)
        val OKX_PROVIDER = Provider(id = 7, name = "okx", referralUrl = null, status = ProviderStatus.Enabled)
        val HTX_PROVIDER = Provider(id = 9, name = "htx", referralUrl = null, status = ProviderStatus.Enabled)

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
