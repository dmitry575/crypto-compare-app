package com.cryptocompare.domain.repository

import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.model.portfolio.PortfolioQuote
import com.cryptocompare.model.symbol.SymbolSellQuote
import kotlinx.coroutines.flow.Flow

/**
 * Портфель лежит на устройстве и с аккаунтом не связан: это не избранное, и
 * молча уносить позиции в Firestore мы не будем.
 */
interface PortfolioRepository {
    fun observePositions(): Flow<List<PortfolioPosition>>

    suspend fun getPosition(symbolId: Long): PortfolioPosition?

    /** Повторное сохранение того же символа меняет позицию, а не заводит второй лот. */
    suspend fun savePosition(position: PortfolioPosition): Result<Unit>

    /** Вместе с позицией уходит и последняя цена её биржи. */
    suspend fun deletePosition(symbolId: Long): Result<Unit>

    /**
     * Последние цены продажи позиций с указанной биржей, по `symbolId`. Цена
     * другой биржи — оставшаяся от прошлой настройки позиции — сюда не попадает.
     */
    fun observePinnedQuotes(): Flow<Map<Long, SymbolSellQuote>>

    suspend fun savePinnedQuotes(quotes: List<PortfolioQuote>): Result<Unit>
}
