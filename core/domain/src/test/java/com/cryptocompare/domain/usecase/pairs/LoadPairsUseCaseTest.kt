package com.cryptocompare.domain.usecase.pairs

import androidx.paging.PagingData
import app.cash.turbine.test
import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.domain.repository.TickerStreamRepository
import com.cryptocompare.model.symbol.PairUiItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test

class LoadPairsUseCaseTest {
    private val cryptoCompareRepository: CryptoCompareRepository = mockk()
    private val tickerStreamRepository: TickerStreamRepository = mockk(relaxed = true)
    private val useCase = LoadPairsUseCase(cryptoCompareRepository, tickerStreamRepository)

    @Test
    fun `opening the catalog also opens the price stream`() =
        runTest {
            every { cryptoCompareRepository.getPairsPaged(any(), any(), any()) } returns
                flowOf(PagingData.empty())

            useCase(query = "", onlyFavourite = false, favouriteTickers = emptySet())

            verify(exactly = 1) { tickerStreamRepository.connect() }
        }

    @Test
    fun `search and filter arguments reach the repository unchanged`() =
        runTest {
            val favourites = setOf("btcusdt", "ethusdt")
            every {
                cryptoCompareRepository.getPairsPaged("btc", true, favourites)
            } returns flowOf(PagingData.empty())

            val flow = useCase(query = "btc", onlyFavourite = true, favouriteTickers = favourites)

            assertNotNull(flow)
            verify(exactly = 1) { cryptoCompareRepository.getPairsPaged("btc", true, favourites) }
        }

    @Test
    fun `paging flow from the repository is returned to the caller`() =
        runTest {
            val paging = PagingData.from(listOf(pair("btcusdt"), pair("ethusdt")))
            every { cryptoCompareRepository.getPairsPaged(any(), any(), any()) } returns flowOf(paging)

            useCase(query = "", onlyFavourite = false, favouriteTickers = emptySet()).test {
                assertNotNull(awaitItem())
                awaitComplete()
            }
        }

    private fun pair(ticker: String) =
        PairUiItem(
            ticker = ticker,
            symbolIds = listOf(1L),
            providerIds = listOf(1),
            minPrice = 1.0,
            maxPrice = 2.0,
            spreadPercent = 100.0,
        )
}
