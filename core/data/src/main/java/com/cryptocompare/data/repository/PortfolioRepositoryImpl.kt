package com.cryptocompare.data.repository

import com.cryptocompare.data.local.dao.PortfolioPositionDao
import com.cryptocompare.data.mapper.toDomain
import com.cryptocompare.data.mapper.toEntity
import com.cryptocompare.domain.repository.PortfolioRepository
import com.cryptocompare.model.portfolio.PortfolioPosition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PortfolioRepositoryImpl
    @Inject
    constructor(
        private val portfolioPositionDao: PortfolioPositionDao,
        @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher,
    ) : PortfolioRepository {
        override fun observePositions(): Flow<List<PortfolioPosition>> =
            portfolioPositionDao.observeAll().map { positions -> positions.map { it.toDomain() } }

        override suspend fun getPosition(symbolId: Long): PortfolioPosition? =
            withContext(ioDispatcher) { portfolioPositionDao.getBySymbol(symbolId)?.toDomain() }

        override suspend fun savePosition(position: PortfolioPosition): Result<Unit> =
            withContext(ioDispatcher) {
                runCatching { portfolioPositionDao.upsert(position.toEntity()) }
                    .onFailure { exception -> if (exception is CancellationException) throw exception }
            }

        override suspend fun deletePosition(symbolId: Long): Result<Unit> =
            withContext(ioDispatcher) {
                runCatching { portfolioPositionDao.delete(symbolId) }
                    .onFailure { exception -> if (exception is CancellationException) throw exception }
            }
    }
