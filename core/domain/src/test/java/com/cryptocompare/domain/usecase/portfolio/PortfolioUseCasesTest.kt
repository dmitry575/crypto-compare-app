package com.cryptocompare.domain.usecase.portfolio

import app.cash.turbine.test
import com.cryptocompare.domain.repository.PortfolioRepository
import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.model.portfolio.PortfolioPositionDraft
import com.cryptocompare.model.portfolio.PortfolioQuote
import com.cryptocompare.model.ticker.TickerPrice
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Use case'ы портфеля: тонкие обёртки над [PortfolioRepository] и правила формы. */
class PortfolioUseCasesTest {
    private val repository: PortfolioRepository = mockk(relaxed = true)

    @Test
    fun `ObservePortfolioUseCase forwards every emission`() =
        runTest {
            every { repository.observePositions() } returns flowOf(emptyList(), listOf(POSITION))

            ObservePortfolioUseCase(repository)().test {
                assertEquals(emptyList<PortfolioPosition>(), awaitItem())
                assertEquals(listOf(POSITION), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `SavePortfolioPositionUseCase stores the draft with a fresh timestamp`() =
        runTest {
            coEvery { repository.savePosition(any()) } returns Result.success(Unit)

            val result = SavePortfolioPositionUseCase(repository)(DRAFT)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                repository.savePosition(
                    match { it.symbolId == DRAFT.symbolId && it.amount == DRAFT.amount && it.updatedAtMillis > 0 },
                )
            }
        }

    @Test
    fun `SavePortfolioPositionUseCase keeps the exchange the coin was bought on`() =
        runTest {
            coEvery { repository.savePosition(any()) } returns Result.success(Unit)

            SavePortfolioPositionUseCase(repository)(DRAFT.copy(providerId = 5))

            coVerify(exactly = 1) { repository.savePosition(match { it.providerId == 5 }) }
        }

    @Test
    fun `ApplyPinnedPriceTicksUseCase stores the exchange bid as the sell price`() =
        runTest {
            coEvery { repository.savePinnedQuotes(any()) } returns Result.success(Unit)

            ApplyPinnedPriceTicksUseCase(repository)(listOf(tick(ask = 81_060.0, bid = 81_050.0)))

            // у события типа 4 имена от лица биржи: её bid — это priceBuy
            coVerify(exactly = 1) {
                repository.savePinnedQuotes(
                    match<List<PortfolioQuote>> { quotes ->
                        quotes.single().let { it.symbolId == 1L && it.providerId == 5 && it.price == 81_050.0 }
                    },
                )
            }
        }

    @Test
    fun `ApplyPinnedPriceTicksUseCase drops a tick without a usable bid`() =
        runTest {
            val result = ApplyPinnedPriceTicksUseCase(repository)(listOf(tick(ask = 81_060.0, bid = 0.0)))

            assertTrue(result.isSuccess)
            coVerify(exactly = 0) { repository.savePinnedQuotes(any()) }
        }

    @Test
    fun `a zero amount deletes the position instead of storing an empty one`() =
        runTest {
            coEvery { repository.deletePosition(any()) } returns Result.success(Unit)

            val result = SavePortfolioPositionUseCase(repository)(DRAFT.copy(amount = 0.0))

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.deletePosition(DRAFT.symbolId) }
            coVerify(exactly = 0) { repository.savePosition(any()) }
        }

    @Test
    fun `negative values are refused rather than quietly flipped`() =
        runTest {
            // короткие позиции в ручном портфеле не поддерживаются
            val negativeAmount = SavePortfolioPositionUseCase(repository)(DRAFT.copy(amount = -1.0))
            val negativePrice = SavePortfolioPositionUseCase(repository)(DRAFT.copy(buyPrice = -1.0))

            assertTrue(negativeAmount.isFailure)
            assertTrue(negativePrice.isFailure)
            coVerify(exactly = 0) { repository.savePosition(any()) }
            coVerify(exactly = 0) { repository.deletePosition(any()) }
        }

    @Test
    fun `DeletePortfolioPositionUseCase delegates to the repository`() =
        runTest {
            coEvery { repository.deletePosition(1L) } returns Result.success(Unit)

            assertTrue(DeletePortfolioPositionUseCase(repository)(1L).isSuccess)
            coVerify(exactly = 1) { repository.deletePosition(1L) }
        }

    @Test
    fun `GetPortfolioPositionUseCase returns what the repository has`() =
        runTest {
            coEvery { repository.getPosition(1L) } returns POSITION

            assertEquals(POSITION, GetPortfolioPositionUseCase(repository)(1L))
        }

    private fun tick(
        ask: Double,
        bid: Double,
    ) = TickerPrice(ticker = "BTCUSDT", symbolId = 1, providerId = 5, priceSell = ask, priceBuy = bid)

    private companion object {
        val DRAFT =
            PortfolioPositionDraft(
                symbolId = 1L,
                ticker = "BTCUSDT",
                amount = 0.42,
                buyPrice = 72_000.0,
            )

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
