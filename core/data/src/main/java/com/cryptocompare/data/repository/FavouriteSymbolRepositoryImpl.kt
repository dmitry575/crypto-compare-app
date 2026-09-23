package com.cryptocompare.data.repository

import com.cryptocompare.data.local.dao.FavouriteSymbolDao
import com.cryptocompare.data.local.dao.PendingFavouriteOperationDao
import com.cryptocompare.data.local.dao.SymbolDao
import com.cryptocompare.data.local.entity.FavouriteSymbolEntity
import com.cryptocompare.data.local.entity.PendingFavouriteOperationEntity
import com.cryptocompare.data.transactionrunner.DatabaseTransactionRunner
import com.cryptocompare.data.util.DataConstants
import com.cryptocompare.data.util.DataConstants.Favourites.BATCH_CHUNK_SIZE
import com.cryptocompare.data.util.DataConstants.Favourites.MAX_SYNC_PASSES
import com.cryptocompare.data.util.appRunCatching
import com.cryptocompare.domain.repository.FavouriteSymbolRepository
import com.cryptocompare.helpers.util.FirestoreConstants
import com.cryptocompare.model.error.AppError
import com.cryptocompare.model.error.AppException
import com.cryptocompare.model.error.AuthErrorReason
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
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
class FavouriteSymbolRepositoryImpl
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val favouriteSymbolDao: FavouriteSymbolDao,
        private val pendingFavouriteOperationDao: PendingFavouriteOperationDao,
        private val symbolDao: SymbolDao,
        private val transactionRunner: DatabaseTransactionRunner,
        private val auth: FirebaseAuth,
        @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher,
    ) : FavouriteSymbolRepository {
        private val syncMutex = Mutex()

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun observeFavouriteSymbolIds(): Flow<Set<Long>> =
            callbackFlow {
                val listener =
                    FirebaseAuth.AuthStateListener { firebaseAuth -> trySend(firebaseAuth.currentUser?.uid) }
                auth.addAuthStateListener(listener)
                trySend(auth.currentUser?.uid)
                awaitClose { auth.removeAuthStateListener(listener) }
            }.flatMapLatest { userId ->
                if (userId == null) flowOf(emptyList()) else favouriteSymbolDao.observeUserSymbols(userId)
            }.map { entities -> entities.mapTo(mutableSetOf()) { it.symbolId } }

        override suspend fun toggleFavouriteSymbol(
            symbolId: Long,
            ticker: String,
        ): Result<Boolean> =
            withContext(ioDispatcher) {
                appRunCatching {
                    val normalizedTicker = ticker.trim().uppercase()
                    val userId =
                        auth.currentUser?.uid ?: throw AppException(AppError.Auth(AuthErrorReason.NOT_SIGNED_IN))
                    val updatedAt = System.currentTimeMillis()

                    transactionRunner.run {
                        val exists = favouriteSymbolDao.exists(userId, symbolId)

                        if (exists) {
                            favouriteSymbolDao.delete(userId, symbolId)
                            savePendingOperation(
                                userId,
                                symbolId,
                                normalizedTicker,
                                PendingFavouriteOperationEntity.Operation.DELETE,
                                updatedAt,
                            )
                            false
                        } else {
                            favouriteSymbolDao.upsert(
                                FavouriteSymbolEntity(userId, symbolId, normalizedTicker, updatedAt),
                            )
                            savePendingOperation(
                                userId,
                                symbolId,
                                normalizedTicker,
                                PendingFavouriteOperationEntity.Operation.ADD,
                                updatedAt,
                            )
                            true
                        }
                    }
                }
            }

        override suspend fun syncFavouriteSymbols(): Result<Unit> =
            syncMutex.withLock {
                withContext(ioDispatcher) {
                    appRunCatching {
                        val userId = auth.currentUser?.uid ?: return@appRunCatching

                        syncPendingFavouriteOperations(userId)

                        val remote = fetchRemoteFavourites(userId)
                        val mergedFavourites =
                            transactionRunner.run {
                                if (pendingFavouriteOperationDao.getAllByUser(userId).isNotEmpty()) {
                                    error(DataConstants.Favourites.SYNC_INCOMPLETE)
                                }

                                val localFavourites =
                                    favouriteSymbolDao.getUserSymbols(userId).associateBy { it.symbolId }
                                val merged = mergeFavourites(localFavourites, remote.favourites)
                                favouriteSymbolDao.replaceAll(userId, merged)
                                merged
                            }

                        pushMergedFavourites(userId, mergedFavourites)

                        // документы старого формата удаляются последними: если сюда не
                        // дошли, следующая синхронизация развернёт их заново
                        deleteDocuments(remote.legacyDocs)
                    }
                }
            }

        override suspend fun deleteAllFavourites(): Result<Unit> =
            withContext(ioDispatcher) {
                appRunCatching {
                    val userId = auth.currentUser?.uid ?: return@appRunCatching

                    val remoteDocs =
                        favouritesCollection(userId)
                            .get()
                            .await()
                            .documents
                            .map { document -> document.reference }

                    deleteDocuments(remoteDocs)

                    transactionRunner.run {
                        favouriteSymbolDao.deleteByUser(userId)
                        pendingFavouriteOperationDao.deleteByUser(userId)
                    }
                }
            }

        private suspend fun syncPendingFavouriteOperations(userId: String) {
            repeat(MAX_SYNC_PASSES) {
                val operations = pendingFavouriteOperationDao.getAllByUser(userId)
                if (operations.isEmpty()) return

                operations.forEach { operation ->
                    val document = favouriteDoc(userId, operation.symbolId)

                    when (operation.operation) {
                        PendingFavouriteOperationEntity.Operation.ADD ->
                            document
                                .set(favouriteFields(operation.symbolId, operation.ticker, operation.updatedAt))
                                .await()

                        PendingFavouriteOperationEntity.Operation.DELETE -> document.delete().await()
                    }

                    pendingFavouriteOperationDao.delete(
                        userId,
                        operation.symbolId,
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
            symbolId: Long,
            ticker: String,
            operation: PendingFavouriteOperationEntity.Operation,
            updatedAt: Long,
        ) {
            pendingFavouriteOperationDao.upsert(
                PendingFavouriteOperationEntity(userId, symbolId, ticker, operation, updatedAt),
            )
        }

        private suspend fun fetchRemoteFavourites(userId: String): RemoteFavourites {
            val documents = favouritesCollection(userId).get().await().documents

            val favourites = mutableMapOf<Long, FavouriteSymbolEntity>()
            val legacyDocs = mutableListOf<DocumentReference>()

            documents.forEach { document ->
                val updatedAt = document.getLong(FirestoreConstants.UPDATED_AT_FIELD) ?: 0L
                val ticker =
                    document.getString(FirestoreConstants.TICKER_FIELD)?.trim()?.uppercase()
                        ?: document.id.trim().uppercase()
                val symbolId = document.getLong(FirestoreConstants.SYMBOL_ID_FIELD)

                if (symbolId != null) {
                    favourites[symbolId] = FavouriteSymbolEntity(userId, symbolId, ticker, updatedAt)
                    return@forEach
                }

                val expanded = expandLegacyFavourite(userId, ticker, updatedAt)
                if (expanded.isEmpty()) return@forEach

                expanded.forEach { favourite -> favourites[favourite.symbolId] = favourite }
                legacyDocs += document.reference
            }

            return RemoteFavourites(favourites, legacyDocs)
        }

        /**
         * Документ старого формата назван тикером и про сети не знает: разворачиваем
         * его во все символы этого тикера — ровно то, что пользователь видел до
         * разделения по сетям.
         *
         * Если каталога ещё нет (первый запуск на новом устройстве), разворачивать
         * не во что: пустой ответ оставляет документ нетронутым до следующей
         * синхронизации, а не теряет избранное.
         */
        private suspend fun expandLegacyFavourite(
            userId: String,
            ticker: String,
            updatedAt: Long,
        ): List<FavouriteSymbolEntity> =
            symbolDao
                .getByTicker(ticker)
                .map { symbol -> FavouriteSymbolEntity(userId, symbol.id, ticker, updatedAt) }

        private suspend fun pushMergedFavourites(
            userId: String,
            mergedFavourites: List<FavouriteSymbolEntity>,
        ) {
            mergedFavourites.chunked(BATCH_CHUNK_SIZE).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { favourite ->
                    batch.set(
                        favouriteDoc(userId, favourite.symbolId),
                        favouriteFields(favourite.symbolId, favourite.ticker, favourite.updatedAt),
                    )
                }
                batch.commit().await()
            }
        }

        private suspend fun deleteDocuments(documents: List<DocumentReference>) {
            documents.chunked(BATCH_CHUNK_SIZE).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { document -> batch.delete(document) }
                batch.commit().await()
            }
        }

        private fun favouriteFields(
            symbolId: Long,
            ticker: String,
            updatedAt: Long,
        ): Map<String, Any> =
            mapOf(
                FirestoreConstants.SYMBOL_ID_FIELD to symbolId,
                FirestoreConstants.TICKER_FIELD to ticker,
                FirestoreConstants.UPDATED_AT_FIELD to updatedAt,
            )

        private fun favouritesCollection(userId: String) =
            firestore
                .collection(FirestoreConstants.USERS_COLLECTION)
                .document(userId)
                .collection(FirestoreConstants.FAVORITES_COLLECTION)

        private fun favouriteDoc(
            userId: String,
            symbolId: Long,
        ): DocumentReference = favouritesCollection(userId).document(symbolId.toString())

        private fun mergeFavourites(
            localFavourites: Map<Long, FavouriteSymbolEntity>,
            remoteFavourites: Map<Long, FavouriteSymbolEntity>,
        ): List<FavouriteSymbolEntity> {
            val symbolIds = localFavourites.keys + remoteFavourites.keys
            return symbolIds.mapNotNull { symbolId ->
                val local = localFavourites[symbolId]
                val remote = remoteFavourites[symbolId]

                when {
                    local == null -> remote
                    remote == null -> local
                    local.updatedAt >= remote.updatedAt -> local
                    else -> remote
                }
            }
        }
    }
