package com.cryptocompare.data

import app.cash.turbine.test
import com.cryptocompare.data.local.dao.FavouriteTickerDao
import com.cryptocompare.data.local.dao.PendingFavouriteOperationDao
import com.cryptocompare.data.local.entity.PendingFavoriteOperationEntity
import com.cryptocompare.data.repository.FavouriteTickerRepositoryImpl
import com.cryptocompare.data.transactionrunner.DatabaseTransactionRunner
import com.cryptocompare.data.util.DataConstants
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavouriteTickerRepositoryImplTest {
    private val firestore: FirebaseFirestore = mockk()
    private val dao: FavouriteTickerDao = mockk()
    private val pendingDao: PendingFavouriteOperationDao = mockk(relaxed = true)
    private val auth: FirebaseAuth = mockk(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()

    // Реальный RoomDatabase.withTransaction трогает внутреннюю
    // executor/coroutine машинерию Room, которая не работает на
    // mockk-моках — просто выполняем блок напрямую. Это НЕ проверяет
    // реальную SQLite-сериализацию транзакций (для этого нужен
    // инструментальный тест с настоящим Room, см. примечание внизу файла) —
    // здесь мы проверяем только бизнес-логику вокруг вызовов DAO.
    private val transactionRunner =
        object : DatabaseTransactionRunner {
            override suspend fun <T> run(block: suspend () -> T): T = block()
        }

    private val repository =
        FavouriteTickerRepositoryImpl(
            firestore = firestore,
            favouriteTickerDao = dao,
            pendingFavouriteOperationDao = pendingDao,
            transactionRunner = transactionRunner,
            auth = auth,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `observeFavouriteTickers emits empty set when current user is null`() =
        runTest {
            every { auth.currentUser } returns null

            repository.observeFavouriteTickers().test {
                assertEquals(emptySet<String>(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `toggleFavouriteTicker returns failure when user is not authorized`() =
        runTest {
            every { auth.currentUser } returns null

            val result = repository.toggleFavouriteTicker("BTCUSDT")

            assertTrue(result.isFailure)
        }

    @Test
    fun `toggleFavouriteTicker adds ticker locally and creates pending ADD without touching firestore`() =
        runTest {
            val userId = "user-1"
            val normalized = "BTCUSDT"
            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            coEvery { dao.exists(userId, normalized) } returns false
            coEvery { dao.upsert(any()) } just runs

            val result = repository.toggleFavouriteTicker("btcusdt")

            assertTrue(result.isSuccess)
            assertEquals(true, result.getOrNull())
            coVerify { dao.upsert(match { it.userId == userId && it.ticker == normalized }) }
            coVerify {
                pendingDao.upsert(
                    match { it.ticker == normalized && it.operation == PendingFavoriteOperationEntity.Operation.ADD },
                )
            }
            verify(exactly = 0) { firestore.collection(any()) }
        }

    @Test
    fun `toggleFavouriteTicker deletes ticker locally and creates pending DELETE without touching firestore`() =
        runTest {
            val userId = "user-1"
            val ticker = "ETHUSDT"
            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            coEvery { dao.exists(userId, ticker) } returns true
            coEvery { dao.delete(userId, ticker) } just runs

            val result = repository.toggleFavouriteTicker(ticker)

            assertTrue(result.isSuccess)
            assertEquals(false, result.getOrNull())
            coVerify { dao.delete(userId, ticker) }
            coVerify {
                pendingDao.upsert(
                    match { it.ticker == ticker && it.operation == PendingFavoriteOperationEntity.Operation.DELETE },
                )
            }
            verify(exactly = 0) { firestore.collection(any()) }
        }

    @Test
    fun `syncFavouriteTickers pushes pending DELETE before reading remote and clears it via deleteIfMatches`() =
        runTest {
            val userId = "user-1"
            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            coEvery { pendingDao.getAllByUser(userId) } returnsMany
                listOf(
                    listOf(
                        PendingFavoriteOperationEntity(
                            userId,
                            "BTCUSDT",
                            PendingFavoriteOperationEntity.Operation.DELETE,
                            1000L,
                        ),
                    ),
                    emptyList(),
                )

            val (favoritesCol) = setupFavoritesCollectionPath(userId)
            val docRef = mockk<DocumentReference>()
            every { favoritesCol.document("BTCUSDT") } returns docRef
            every { docRef.delete() } returns Tasks.forResult(null)

            val querySnapshot = mockk<QuerySnapshot> { every { documents } returns emptyList() }
            every { favoritesCol.get() } returns Tasks.forResult(querySnapshot)

            coEvery { dao.getUserTickers(userId) } returns emptyList()
            coEvery { dao.replaceAll(userId, any()) } just runs

            val result = repository.syncFavouriteTickers()

            assertTrue(result.isSuccess)
            verify { docRef.delete() }
            coVerify {
                pendingDao.delete(userId, "BTCUSDT", PendingFavoriteOperationEntity.Operation.DELETE, 1000L)
            }
            verify { favoritesCol.get() }
            coVerify { dao.replaceAll(userId, emptyList()) }
        }

    @Test
    fun `syncFavouriteTickers does not read remote when a pending operation fails`() =
        runTest {
            val userId = "user-1"
            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            coEvery { pendingDao.getAllByUser(userId) } returns
                listOf(
                    PendingFavoriteOperationEntity(
                        userId,
                        "BTCUSDT",
                        PendingFavoriteOperationEntity.Operation.DELETE,
                        1000L,
                    ),
                )

            val (favoritesCol) = setupFavoritesCollectionPath(userId)
            val docRef = mockk<DocumentReference>()
            every { favoritesCol.document("BTCUSDT") } returns docRef
            every { docRef.delete() } returns Tasks.forException(IllegalStateException("offline"))

            val result = repository.syncFavouriteTickers()

            assertFalse(result.isSuccess)
            verify(exactly = 0) { favoritesCol.get() }
            coVerify(exactly = 0) { pendingDao.delete(any(), any(), any(), any()) }
            coVerify(exactly = 0) { dao.replaceAll(any(), any()) }
        }

    @Test
    fun `syncFavouriteTickers fails when the queue never settles within MAX_SYNC_PASSES`() =
        runTest {
            val userId = "user-1"
            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            // Каждый getAll() возвращает "свежую" операцию — очередь как
            // будто пополняется быстрее, чем успевает опустеть.
            coEvery { pendingDao.getAllByUser(userId) } answers {
                listOf(
                    PendingFavoriteOperationEntity(
                        userId,
                        "BTCUSDT",
                        PendingFavoriteOperationEntity.Operation.ADD,
                        System.nanoTime(),
                    ),
                )
            }

            val (favoritesCol) = setupFavoritesCollectionPath(userId)
            val docRef = mockk<DocumentReference>()
            every { favoritesCol.document("BTCUSDT") } returns docRef
            every { docRef.set(any<Map<String, Any>>()) } returns Tasks.forResult(null)
            // deleteIfMatches намеренно ничего не удаляет (нет точного совпадения updatedAt) —
            // имитируем случай "операция всегда чуть новее, чем мы успели обработать"
            coEvery { pendingDao.delete(any(), any(), any(), any()) } just runs

            val result = repository.syncFavouriteTickers()

            assertFalse(result.isSuccess)
            verify(exactly = 0) { favoritesCol.get() }
            coVerify(exactly = 0) { dao.replaceAll(any(), any()) }
        }

    @Test
    fun `syncFavouriteTickers re-pushes a newer operation that arrived while an older one was in flight`() =
        runTest {
            val userId = "user-1"
            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            coEvery { pendingDao.getAllByUser(userId) } returnsMany
                listOf(
                    listOf(
                        PendingFavoriteOperationEntity(
                            userId,
                            "BTCUSDT",
                            PendingFavoriteOperationEntity.Operation.ADD,
                            1000L,
                        ),
                    ),
                    listOf(
                        PendingFavoriteOperationEntity(
                            userId,
                            "BTCUSDT",
                            PendingFavoriteOperationEntity.Operation.DELETE,
                            2000L,
                        ),
                    ),
                    emptyList(),
                )

            val (favoritesCol) = setupFavoritesCollectionPath(userId)
            val docRef = mockk<DocumentReference>()
            every { favoritesCol.document("BTCUSDT") } returns docRef
            every { docRef.set(any<Map<String, Any>>()) } returns Tasks.forResult(null)
            every { docRef.delete() } returns Tasks.forResult(null)

            val querySnapshot = mockk<QuerySnapshot> { every { documents } returns emptyList() }
            every { favoritesCol.get() } returns Tasks.forResult(querySnapshot)

            coEvery { dao.getUserTickers(userId) } returns emptyList()
            coEvery { dao.replaceAll(userId, any()) } just runs

            val result = repository.syncFavouriteTickers()

            assertTrue(result.isSuccess)
            verify { docRef.set(any<Map<String, Any>>()) }
            verify { docRef.delete() }
            coVerify {
                pendingDao.delete(userId, "BTCUSDT", PendingFavoriteOperationEntity.Operation.ADD, 1000L)
            }
            coVerify {
                pendingDao.delete(userId, "BTCUSDT", PendingFavoriteOperationEntity.Operation.DELETE, 2000L)
            }
            coVerify { dao.replaceAll(userId, emptyList()) }
        }

    @Test
    fun `syncFavouriteTickers aborts merge when a pending operation arrives after the queue was drained`() =
        runTest {
            val userId = "user-1"
            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            // Первый вызов — drain-цикл видит пустую очередь и завершает
            // syncPendingFavouriteOperations() как обычно. Второй вызов —
            // уже внутри транзакции merge — видит операцию, которую
            // toggle() успел положить в окне между drain и merge.
            coEvery { pendingDao.getAllByUser(userId) } returnsMany
                listOf(
                    emptyList(),
                    listOf(
                        PendingFavoriteOperationEntity(
                            userId,
                            "BTCUSDT",
                            PendingFavoriteOperationEntity.Operation.DELETE,
                            5000L,
                        ),
                    ),
                )

            val querySnapshot = mockk<QuerySnapshot> { every { documents } returns emptyList() }
            val (favoritesCol) = setupFavoritesCollectionPath(userId)
            every { favoritesCol.get() } returns Tasks.forResult(querySnapshot)

            val result = repository.syncFavouriteTickers()

            assertFalse(result.isSuccess)
            assertEquals(DataConstants.Favourites.SYNC_INCOMPLETE, result.exceptionOrNull()?.message)
            coVerify(exactly = 0) { dao.replaceAll(any(), any()) }
            verify(exactly = 0) { firestore.batch() }
        }

    @Test
    fun `syncFavouriteTickers merges remote-only ticker into empty local db`() =
        runTest {
            val userId = "user-1"
            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            coEvery { pendingDao.getAllByUser(userId) } returns emptyList()

            val remoteDoc =
                mockk<DocumentSnapshot> {
                    every { getString("ticker") } returns "BTCUSDT"
                    every { getLong("updatedAt") } returns 1000L
                }
            val querySnapshot = mockk<QuerySnapshot> { every { documents } returns listOf(remoteDoc) }
            val (favoritesCol) = setupFavoritesCollectionPath(userId)
            every { favoritesCol.get() } returns Tasks.forResult(querySnapshot)

            val docRef = mockk<DocumentReference>()
            every { favoritesCol.document("BTCUSDT") } returns docRef
            val batch = mockk<WriteBatch>()
            every { firestore.batch() } returns batch
            every { batch.set(docRef, any<Map<String, Any>>()) } returns batch
            every { batch.commit() } returns Tasks.forResult(null)

            coEvery { dao.getUserTickers(userId) } returns emptyList()
            coEvery { dao.replaceAll(userId, any()) } just runs

            val result = repository.syncFavouriteTickers()

            assertTrue(result.isSuccess)
            coVerify { dao.replaceAll(userId, match { it.size == 1 && it[0].ticker == "BTCUSDT" }) }
        }

    @Test
    fun `syncFavouriteTickers serializes concurrent calls via mutex`() =
        runTest {
            val userId = "user-1"
            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            coEvery { pendingDao.getAllByUser(userId) } returns emptyList()
            val querySnapshot = mockk<QuerySnapshot> { every { documents } returns emptyList() }
            val (favoritesCol) = setupFavoritesCollectionPath(userId)
            every { favoritesCol.get() } returns Tasks.forResult(querySnapshot)
            coEvery { dao.getUserTickers(userId) } returns emptyList()
            coEvery { dao.replaceAll(userId, any()) } just runs

            val first = repository.syncFavouriteTickers()
            val second = repository.syncFavouriteTickers()

            assertTrue(first.isSuccess)
            assertTrue(second.isSuccess)
            coVerify(exactly = 2) { dao.replaceAll(userId, emptyList()) }
        }

    @Test
    fun `deleteAllFavourites batch-deletes remote docs and clears local dao and pending queue`() =
        runTest {
            val userId = "user-1"
            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            val docRef = mockk<DocumentReference>()
            val remoteDoc = mockk<DocumentSnapshot> { every { reference } returns docRef }
            val querySnapshot = mockk<QuerySnapshot> { every { documents } returns listOf(remoteDoc) }
            val (favoritesCol) = setupFavoritesCollectionPath(userId)
            every { favoritesCol.get() } returns Tasks.forResult(querySnapshot)

            val batch = mockk<WriteBatch>()
            every { firestore.batch() } returns batch
            every { batch.delete(docRef) } returns batch
            every { batch.commit() } returns Tasks.forResult(null)

            coEvery { dao.deleteByUser(userId) } just runs

            val result = repository.deleteAllFavorites()

            assertTrue(result.isSuccess)
            verify { batch.delete(docRef) }
            coVerify { dao.deleteByUser(userId) }
            coVerify { pendingDao.deleteByUser(userId) }
        }

    private fun setupFavoritesCollectionPath(userId: String): Pair<CollectionReference, DocumentReference> {
        val usersCol = mockk<CollectionReference>()
        val userDoc = mockk<DocumentReference>()
        val favoritesCol = mockk<CollectionReference>()

        every { firestore.collection("users") } returns usersCol
        every { usersCol.document(userId) } returns userDoc
        every { userDoc.collection("favorites") } returns favoritesCol

        return favoritesCol to userDoc
    }

    // ПРИМЕЧАНИЕ: фейковый transactionRunner здесь просто вызывает block()
    // напрямую и НЕ проверяет реальную SQLite-сериализацию транзакций,
    // защищающую от гонки toggle() ↔ merge() в syncFavouriteTickers(). Это
    // проверяется отдельным androidTest с настоящим Room (in-memory),
    // где два корутина параллельно дёргают toggle() и syncFavouriteTickers()
    // и проверяется, что ни одна запись не теряется.
}
