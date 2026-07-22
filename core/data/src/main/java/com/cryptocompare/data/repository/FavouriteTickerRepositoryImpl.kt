package com.cryptocompare.data.repository

import com.cryptocompare.data.local.dao.FavouriteTickerDao
import com.cryptocompare.data.local.entity.FavouriteTickerEntity
import com.cryptocompare.domain.repository.FavouriteTickerRepository
import com.cryptocompare.helpers.util.FirestoreConstants
import com.google.firebase.auth.FirebaseAuth
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
        private val auth: FirebaseAuth,
        @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher,
    ) : FavouriteTickerRepository {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun observeFavouriteTickers(): Flow<Set<String>> =
            callbackFlow {
                val listener =
                    FirebaseAuth.AuthStateListener { firebaseAuth ->
                        trySend(firebaseAuth.currentUser?.uid)
                    }

                auth.addAuthStateListener(listener)
                trySend(auth.currentUser?.uid)
                awaitClose {
                    auth.removeAuthStateListener(listener)
                }
            }.flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(emptyList())
                } else {
                    favouriteTickerDao.observeUserTickers(userId)
                }
            }.map { entities -> entities.mapTo(mutableSetOf()) { it.ticker } }

        override suspend fun toggleFavouriteTicker(ticker: String): Result<Boolean> =
            withContext(ioDispatcher) {
                runCatching {
                    val normalizedTicker = ticker.trim().uppercase()
                    val userId = auth.currentUser?.uid ?: error("User not authorized")
                    val exists = favouriteTickerDao.exists(userId, normalizedTicker)
                    if (exists) {
                        favouriteTickerDao.delete(userId, normalizedTicker)
                        firestore
                            .collection(FirestoreConstants.USERS_COLLECTION)
                            .document(userId)
                            .collection(FirestoreConstants.FAVORITES_COLLECTION)
                            .document(normalizedTicker)
                            .delete()
                            .await()
                        false
                    } else {
                        val updatedAt = System.currentTimeMillis()
                        favouriteTickerDao.upsert(
                            FavouriteTickerEntity(
                                userId = userId,
                                ticker = normalizedTicker,
                                updatedAt = updatedAt,
                            ),
                        )
                        firestore
                            .collection(FirestoreConstants.USERS_COLLECTION)
                            .document(userId)
                            .collection(FirestoreConstants.FAVORITES_COLLECTION)
                            .document(normalizedTicker)
                            .set(
                                mapOf(
                                    FirestoreConstants.TICKER_FIELD to ticker,
                                    FirestoreConstants.UPDATED_AT_FIELD to updatedAt,
                                ),
                            ).await()

                        true
                    }
                }.onFailure { exception -> if (exception is CancellationException) throw exception }
            }

        override suspend fun syncFavouriteTickers(): Result<Unit> =
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

                    val remoteTickers =
                        remoteDocs
                            .mapNotNull { documentSnapshot ->
                                val ticker =
                                    documentSnapshot.getString(FirestoreConstants.TICKER_FIELD)?.trim()?.uppercase()
                                        ?: return@mapNotNull null
                                val updatedAt = documentSnapshot.getLong(FirestoreConstants.UPDATED_AT_FIELD) ?: 0L
                                ticker to
                                    FavouriteTickerEntity(
                                        userId = userId,
                                        ticker = ticker,
                                        updatedAt = updatedAt,
                                    )
                            }.toMap()

                    val localTickers = favouriteTickerDao.getUserTickers(userId).associateBy { it.ticker }
                    val mergedFavourites = mergeTickers(localTickers, remoteTickers)

                    mergedFavourites.chunked(500).forEach { chunk ->
                        val batch = firestore.batch()
                        chunk.forEach { favourite ->
                            val doc =
                                firestore
                                    .collection(FirestoreConstants.USERS_COLLECTION)
                                    .document(userId)
                                    .collection(FirestoreConstants.FAVORITES_COLLECTION)
                                    .document(favourite.ticker)
                            batch.set(
                                doc,
                                mapOf(
                                    FirestoreConstants.TICKER_FIELD to favourite.ticker,
                                    FirestoreConstants.UPDATED_AT_FIELD to favourite.updatedAt,
                                ),
                            )
                        }
                        batch.commit().await()
                    }

                    favouriteTickerDao.replaceAll(userId, mergedFavourites)
                }.onFailure { exception -> if (exception is CancellationException) throw exception }
            }

        private fun mergeTickers(
            localTickers: Map<String, FavouriteTickerEntity>,
            remoteTickers: Map<String, FavouriteTickerEntity>,
        ): List<FavouriteTickerEntity> {
            val mergedTickers = localTickers.keys + remoteTickers.keys
            return mergedTickers.mapNotNull { ticker ->
                val localTicker = localTickers[ticker]
                val remoteTicker = remoteTickers[ticker]

                when {
                    localTicker == null && remoteTicker == null -> null
                    localTicker == null -> remoteTicker
                    remoteTicker == null -> localTicker
                    localTicker.updatedAt >= remoteTicker.updatedAt -> localTicker
                    else -> remoteTicker
                }
            }
        }
    }
