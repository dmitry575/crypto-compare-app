package com.cryptocompare.data.repository

import com.cryptocompare.data.local.dao.FavouriteTickerDao
import com.cryptocompare.data.local.dao.PendingFavouriteOperationDao
import com.cryptocompare.data.local.entity.FavouriteTickerEntity
import com.cryptocompare.data.local.entity.PendingFavoriteOperationEntity
import com.cryptocompare.data.transactionrunner.DatabaseTransactionRunner
import com.cryptocompare.data.util.DataConstants
import com.cryptocompare.data.util.DataConstants.Favourites.BATCH_CHUNK_SIZE
import com.cryptocompare.data.util.DataConstants.Favourites.MAX_SYNC_PASSES
import com.cryptocompare.domain.repository.FavouriteTickerRepository
import com.cryptocompare.helpers.util.FirestoreConstants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class FavouriteTickerRepositoryImpl
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val favouriteTickerDao: FavouriteTickerDao,
        private val pendingFavouriteOperationDao: PendingFavouriteOperationDao,
        private val transactionRunner: DatabaseTransactionRunner,
        private val auth: FirebaseAuth,
        @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher,
    ) : FavouriteTickerRepository {
        private val syncMutex = Mutex()

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun observeFavouriteTickers(): Flow<Set<String>> =
            callbackFlow {
                val listener =
                    FirebaseAuth.AuthStateListener { firebaseAuth -> trySend(firebaseAuth.currentUser?.uid) }
                auth.addAuthStateListener(listener)
                trySend(auth.currentUser?.uid)
                awaitClose { auth.removeAuthStateListener(listener) }
            }.flatMapLatest { userId ->
                if (userId == null) flowOf(emptyList()) else favouriteTickerDao.observeUserTickers(userId)
            }.map { entities -> entities.mapTo(mutableSetOf()) { it.ticker } }

        override suspend fun toggleFavouriteTicker(ticker: String): Result<Boolean> =
            withContext(ioDispatcher) {
                runCatching {
                    val normalizedTicker = ticker.trim().uppercase()
                    val userId = auth.currentUser?.uid ?: error(DataConstants.Auth.NO_CURRENT_USER)
                    val updatedAt = System.currentTimeMillis()

                    transactionRunner.run {
                        val exists = favouriteTickerDao.exists(userId, normalizedTicker)

                        if (exists) {
                            favouriteTickerDao.delete(userId, normalizedTicker)
                            savePendingOperation(
                                userId,
                                normalizedTicker,
                                PendingFavoriteOperationEntity.Operation.DELETE,
                                updatedAt,
                            )
                            false
                        } else {
                            favouriteTickerDao.upsert(FavouriteTickerEntity(userId, normalizedTicker, updatedAt))
                            savePendingOperation(
                                userId,
                                normalizedTicker,
                                PendingFavoriteOperationEntity.Operation.ADD,
                                updatedAt,
                            )
                            true
                        }
                    }
                }.onFailure { exception -> if (exception is CancellationException) throw exception }
            }

        override suspend fun syncFavouriteTickers(): Result<Unit> =
            syncMutex.withLock {
                withContext(ioDispatcher) {
                    runCatching {
                        val userId = auth.currentUser?.uid ?: return@runCatching

                        syncPendingFavouriteOperations(userId)

                        val remoteTickers = fetchRemoteTickers(userId)
                        val mergedFavourites =
                            transactionRunner.run {
                                if (pendingFavouriteOperationDao.getAllByUser(userId).isNotEmpty()) {
                                    error(DataConstants.Favourites.SYNC_INCOMPLETE)
                                }

                                val localTickers = favouriteTickerDao.getUserTickers(userId).associateBy { it.ticker }
                                val merged = mergeTickers(localTickers, remoteTickers)
                                favouriteTickerDao.replaceAll(userId, merged)
                                merged
                            }

                        pushMergedFavourites(userId, mergedFavourites)
                    }.onFailure { exception -> if (exception is CancellationException) throw exception }
                }
            }

        override suspend fun deleteAllFavorites(): Result<Unit> =
            withContext(ioDispatcher) {
                runCatching {
                    val userId = auth.currentUser?.uid ?: return@runCatching

                    val remoteDocs =
                        firestore
                            .collection(FirestoreConstants.USERS_COLLECTION)
                            .document(userId)
                            .collection(FirestoreConstants.FAVORITES_COLLECTION)
                            .get()
                            .await()
                            .documents

                    remoteDocs.chunked(BATCH_CHUNK_SIZE).forEach { chunk ->
                        val batch = firestore.batch()
                        chunk.forEach { document -> batch.delete(document.reference) }
                        batch.commit().await()
                    }
                    transactionRunner.run {
                        favouriteTickerDao.deleteByUser(userId)
                        pendingFavouriteOperationDao.deleteByUser(userId)
                    }
                }.onFailure { exception -> if (exception is CancellationException) throw exception }
            }

        private suspend fun syncPendingFavouriteOperations(userId: String) {
            repeat(MAX_SYNC_PASSES) {
                val operations = pendingFavouriteOperationDao.getAllByUser(userId)
                if (operations.isEmpty()) return

                operations.forEach { operation ->
                    val document = favouriteDoc(userId, operation.ticker)

                    when (operation.operation) {
                        PendingFavoriteOperationEntity.Operation.ADD ->
                            document
                                .set(
                                    mapOf(
                                        FirestoreConstants.TICKER_FIELD to operation.ticker,
                                        FirestoreConstants.UPDATED_AT_FIELD to operation.updatedAt,
                                    ),
                                ).await()

                        PendingFavoriteOperationEntity.Operation.DELETE -> document.delete().await()
                    }

                    pendingFavouriteOperationDao.delete(
                        userId,
                        operation.ticker,
                        operation.operation,
                        operation.updatedAt,
                    )
                }
            }

            if (pendingFavouriteOperationDao.getAllByUser(userId).isNotEmpty()) {
                error(DataConstants.Favourites.SYNC_INCOMPLETE)
            }
        }

        private suspend fun savePendingOperation(
            userId: String,
            ticker: String,
            operation: PendingFavoriteOperationEntity.Operation,
            updatedAt: Long,
        ) {
            pendingFavouriteOperationDao.upsert(
                PendingFavoriteOperationEntity(userId, ticker, operation, updatedAt),
            )
        }

        private suspend fun fetchRemoteTickers(userId: String): Map<String, FavouriteTickerEntity> =
            firestore
                .collection(FirestoreConstants.USERS_COLLECTION)
                .document(userId)
                .collection(FirestoreConstants.FAVORITES_COLLECTION)
                .get()
                .await()
                .documents
                .mapNotNull { documentSnapshot ->
                    val ticker =
                        documentSnapshot.getString(FirestoreConstants.TICKER_FIELD)?.trim()?.uppercase()
                            ?: return@mapNotNull null
                    val updatedAt = documentSnapshot.getLong(FirestoreConstants.UPDATED_AT_FIELD) ?: 0L
                    ticker to FavouriteTickerEntity(userId, ticker, updatedAt)
                }.toMap()

        private suspend fun pushMergedFavourites(
            userId: String,
            mergedFavourites: List<FavouriteTickerEntity>,
        ) {
            mergedFavourites.chunked(BATCH_CHUNK_SIZE).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { favourite ->
                    batch.set(
                        favouriteDoc(userId, favourite.ticker),
                        mapOf(
                            FirestoreConstants.TICKER_FIELD to favourite.ticker,
                            FirestoreConstants.UPDATED_AT_FIELD to favourite.updatedAt,
                        ),
                    )
                }
                batch.commit().await()
            }
        }

        private fun favouriteDoc(
            userId: String,
            ticker: String,
        ): DocumentReference =
            firestore
                .collection(FirestoreConstants.USERS_COLLECTION)
                .document(userId)
                .collection(FirestoreConstants.FAVORITES_COLLECTION)
                .document(ticker)

        private fun mergeTickers(
            localTickers: Map<String, FavouriteTickerEntity>,
            remoteTickers: Map<String, FavouriteTickerEntity>,
        ): List<FavouriteTickerEntity> {
            val mergedTickers = localTickers.keys + remoteTickers.keys
            return mergedTickers.mapNotNull { ticker ->
                val localTicker = localTickers[ticker]
                val remoteTicker = remoteTickers[ticker]

                when {
                    localTicker == null -> remoteTicker
                    remoteTicker == null -> localTicker
                    localTicker.updatedAt >= remoteTicker.updatedAt -> localTicker
                    else -> remoteTicker
                }
            }
        }
    }
