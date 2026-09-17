package com.cryptocompare.data

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cryptocompare.data.local.CryptoCompareDatabase
import com.cryptocompare.data.local.dao.FavouriteSymbolDao
import com.cryptocompare.data.local.dao.PendingFavouriteOperationDao
import com.cryptocompare.data.local.entity.FavouriteSymbolEntity
import com.cryptocompare.data.local.entity.PendingFavouriteOperationEntity
import com.cryptocompare.data.repository.FavouriteSymbolRepositoryImpl
import com.cryptocompare.data.transactionrunner.DatabaseTransactionRunner
import com.cryptocompare.data.transactionrunner.DatabaseTransactionRunnerImpl
import com.cryptocompare.data.util.DataConstants
import com.cryptocompare.helpers.util.FirestoreConstants
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentHashMap

/**
 * Инструментальные тесты на реальном in-memory Room + реальных DAO.
 * Firebase остаётся замоканным — нас интересует только SQLite-часть
 * гарантии: что syncFavouriteSymbols() и toggleFavouriteSymbol() не могут
 * интерливиться на уровне отдельных DAO-вызовов.
 *
 * mockk-юнит-тесты (FavouriteSymbolRepositoryImplTest) этого не проверяют:
 * там transactionRunner — фейк, вызывающий block() напрямую, и он ничего
 * не говорит о том, сериализует ли реальная Room-транзакция параллельные
 * вызовы. Здесь — говорит.
 */
@RunWith(AndroidJUnit4::class)
class FavouriteSymbolRepositoryRaceTest {
    private lateinit var database: CryptoCompareDatabase
    private lateinit var favouriteDao: FavouriteSymbolDao
    private lateinit var pendingDao: PendingFavouriteOperationDao

    /** Снимок «удалённого» состояния Firestore-мока; читается в проверках. */
    private lateinit var remoteState: ConcurrentHashMap<String, Long>

    private val firestore: FirebaseFirestore = mockk()
    private val auth: FirebaseAuth = mockk(relaxed = true)

    private val userId = "user-1"

    private companion object {
        const val SYMBOL_ID = 1L
        const val TICKER = "BTCUSDT"
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, CryptoCompareDatabase::class.java)
                .build()
        favouriteDao = database.favouriteSymbolDao()
        pendingDao = database.pendingFavouriteOperationDao()

        val user = mockk<FirebaseUser> { every { uid } returns userId }
        every { auth.currentUser } returns user
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ---------------------------------------------------------------
    // Последовательные базовые случаи — не гонка, но фиксируют инвариант,
    // на фоне которого имеет смысл сама гонка ниже.
    // ---------------------------------------------------------------

    @Test
    fun sequentialBaselineToggleCommittedBeforeSyncIsVisibleToSync() =
        runBlocking {
            favouriteDao.upsert(FavouriteSymbolEntity(userId, SYMBOL_ID, TICKER, 1L))
            firestoreReturns(emptyList()) // remote ещё не знает про BTC

            val repository = createRepository(DatabaseTransactionRunnerImpl(database))

            val toggleResult = repository.toggleFavouriteSymbol(SYMBOL_ID, TICKER)
            assertTrue(toggleResult.isSuccess)
            assertEquals(false, toggleResult.getOrNull()) // ticker уже был => сняли

            assertTrue(favouriteDao.getUserSymbols(userId).isEmpty())
            val pending = pendingDao.getAllByUser(userId)
            assertEquals(1, pending.size)
            assertEquals(PendingFavouriteOperationEntity.Operation.DELETE, pending.single().operation)
        }

    @Test
    fun sequentialBaselineSyncMergeCommittedThenToggleOverwritesItCorrectly() =
        runBlocking {
            firestoreReturns(listOf(SYMBOL_ID.toString() to 1L)) // remote уже содержит BTC

            val repository = createRepository(DatabaseTransactionRunnerImpl(database))

            val syncResult = repository.syncFavouriteSymbols()
            assertTrue(syncResult.isSuccess)
            assertEquals(listOf(SYMBOL_ID), favouriteDao.getUserSymbols(userId).map { it.symbolId })

            // sync полностью завершился (включая push в Firestore) —
            // только теперь пользователь тапает звёздочку
            val toggleResult = repository.toggleFavouriteSymbol(SYMBOL_ID, TICKER)

            assertTrue(toggleResult.isSuccess)
            assertEquals(false, toggleResult.getOrNull())
            assertTrue(favouriteDao.getUserSymbols(userId).isEmpty())
            assertEquals(
                PendingFavouriteOperationEntity.Operation.DELETE,
                pendingDao.getAllByUser(userId).single().operation,
            )
        }

    // ---------------------------------------------------------------
    // Главный тест: доказывает МЕХАНИЗМ (сериализацию транзакций),
    // а не только удачное финальное состояние.
    // ---------------------------------------------------------------

    @Test
    fun syncTransactionFullyBlocksConcurrentToggleTransactionUntilItCommits() =
        runBlocking {
            favouriteDao.upsert(FavouriteSymbolEntity(userId, SYMBOL_ID, TICKER, 1L))
            firestoreReturns(listOf(SYMBOL_ID.toString() to 1L))

            val blockingRunner = BlockingTransactionRunner(database)
            val repository = createRepository(blockingRunner)

            val syncJob = launch { repository.syncFavouriteSymbols() }
            // sync's merge-транзакция уже открыта на уровне Room, но ещё не закоммичена
            blockingRunner.transactionStarted.await()

            val toggleJob = async { repository.toggleFavouriteSymbol(SYMBOL_ID, TICKER) }

            // Даём toggleJob шанс продвинуться. Если бы Room не сериализовала
            // транзакции, он бы успел выполниться здесь — вызов
            // database.withTransaction{} внутри toggle физически блокируется
            // Room'ом, пока открыта транзакция sync, и лямбда toggle'а даже
            // не начнёт выполняться.
            repeat(20) { yield() }
            assertFalse("toggle не должен завершиться, пока открыта транзакция sync", toggleJob.isCompleted)

            blockingRunner.continueTransaction.complete(Unit) // отпускаем sync
            val toggleResult = toggleJob.await()
            syncJob.join()

            assertTrue(toggleResult.isSuccess)
            // toggle применился ПОСЛЕ merge — его удаление не потерялось
            assertTrue(favouriteDao.getUserSymbols(userId).isEmpty())
            val pending = pendingDao.getAllByUser(userId)
            assertEquals(1, pending.size)
            assertEquals(PendingFavouriteOperationEntity.Operation.DELETE, pending.single().operation)
        }

    @Test
    fun repeatedConcurrentToggleAndSyncNeverLosesLocalFavouriteChange() =
        runBlocking {
            repeat(100) { iteration ->
                favouriteDao.deleteByUser(userId)
                pendingDao.deleteByUser(userId)
                favouriteDao.upsert(FavouriteSymbolEntity(userId, SYMBOL_ID, TICKER, 1L))
                firestoreReturns(listOf(SYMBOL_ID.toString() to 1L))

                val repository = createRepository(DatabaseTransactionRunnerImpl(database))

                val syncJob = async { repository.syncFavouriteSymbols() }
                val toggleJob = async { repository.toggleFavouriteSymbol(SYMBOL_ID, TICKER) }

                val syncResult = syncJob.await()
                val toggleResult = toggleJob.await()

                assertTrue("Iteration $iteration: toggle failed", toggleResult.isSuccess)
                // sync иногда легитимно проваливается, если поймал гонку с
                // toggle (обнаружил непустую очередь внутри транзакции
                // merge) — это ожидаемо и самовосстанавливается следующим
                // sync'ом; здесь важно только итоговое состояние данных.
                if (!syncResult.isSuccess) {
                    assertEquals(
                        DataConstants.Favourites.SYNC_INCOMPLETE,
                        syncResult.exceptionOrNull()?.message,
                    )
                }

                val local = favouriteDao.getUserSymbols(userId)
                val pending = pendingDao.getAllByUser(userId)

                assertTrue(
                    "Iteration $iteration: BTC was resurrected in Room",
                    local.none { it.symbolId == SYMBOL_ID },
                )
                // Удаление не должно потеряться, но допустимы ДВА консистентных исхода:
                //  1) sync поймал гонку и не тронул очередь — DELETE ещё в pending,
                //     доедет следующим sync'ом (remote пока содержит BTC);
                //  2) toggle успел закоммититься до чтения очереди — sync сам
                //     протолкнул DELETE в Firestore и очистил pending (remote чист).
                // Потеря — только если BTC остался в remote, а в очереди нет DELETE,
                // который бы его убрал: тогда следующий sync воскресил бы избранное.
                val remoteHasBtc = remoteState.containsKey(SYMBOL_ID.toString())
                val pendingHasDelete =
                    pending.any {
                        it.symbolId == SYMBOL_ID && it.operation == PendingFavouriteOperationEntity.Operation.DELETE
                    }
                assertTrue(
                    "Iteration $iteration: удаление потеряно — BTC остался в remote без DELETE в очереди",
                    !remoteHasBtc || pendingHasDelete,
                )
            }
        }

    // ---------------------------------------------------------------
    // Аннигиляция последовательных toggle через
    // PRIMARY KEY(userId, ticker) + OnConflictStrategy.REPLACE.
    // Это проверяет реальную SQLite-семантику, не mockk-заглушки.
    // ---------------------------------------------------------------

    @Test
    fun secondToggleReplacesPendingAddWithDelete() =
        runBlocking {
            firestoreReturns(emptyList())
            val repository = createRepository(DatabaseTransactionRunnerImpl(database))

            val first = repository.toggleFavouriteSymbol(SYMBOL_ID, TICKER)
            assertTrue(first.isSuccess)
            assertEquals(true, first.getOrNull())

            val second = repository.toggleFavouriteSymbol(SYMBOL_ID, TICKER)
            assertTrue(second.isSuccess)
            assertEquals(false, second.getOrNull())

            assertTrue(favouriteDao.getUserSymbols(userId).isEmpty())
            val pending = pendingDao.getAllByUser(userId)
            assertEquals(1, pending.size)
            assertEquals(PendingFavouriteOperationEntity.Operation.DELETE, pending.single().operation)
        }

    @Test
    fun threeTogglesLeaveOnlyLatestAddOperation() =
        runBlocking {
            firestoreReturns(emptyList())
            val repository = createRepository(DatabaseTransactionRunnerImpl(database))

            repository.toggleFavouriteSymbol(SYMBOL_ID, TICKER)
            repository.toggleFavouriteSymbol(SYMBOL_ID, TICKER)
            repository.toggleFavouriteSymbol(SYMBOL_ID, TICKER)

            val local = favouriteDao.getUserSymbols(userId)
            assertEquals(1, local.size)
            assertEquals(SYMBOL_ID, local.single().symbolId)

            val pending = pendingDao.getAllByUser(userId)
            assertEquals(1, pending.size)
            assertEquals(PendingFavouriteOperationEntity.Operation.ADD, pending.single().operation)
        }

    // ---------------------------------------------------------------
    // Инфраструктура теста
    // ---------------------------------------------------------------

    private fun createRepository(transactionRunner: DatabaseTransactionRunner): FavouriteSymbolRepositoryImpl =
        FavouriteSymbolRepositoryImpl(
            firestore = firestore,
            favouriteSymbolDao = favouriteDao,
            pendingFavouriteOperationDao = pendingDao,
            symbolDao = database.symbolDao(),
            transactionRunner = transactionRunner,
            auth = auth,
            ioDispatcher = Dispatchers.IO,
        )

    /**
     * Настраивает мок Firestore как маленькое in-memory "хранилище":
     * `.set()/.delete()` реально мутируют состояние, а `.get()` каждый раз
     * отдаёт актуальный снимок. Без этого стресс-тест ловит ложные
     * "resurrection" срабатывания — не из-за реальной гонки в репозитории,
     * а потому что статичный мок не отражает собственные же удаления,
     * сделанные внутри syncPendingFavouriteOperations() в этом же прогоне
     * sync(). Настоящий Firestore так себя не ведёт: запись, сделанную
     * тем же клиентом непосредственно перед чтением, следующее чтение
     * увидит.
     */
    private fun firestoreReturns(initialFavourites: List<Pair<String, Long>>) {
        remoteState = ConcurrentHashMap<String, Long>().apply { putAll(initialFavourites) }
        val docRefsById = ConcurrentHashMap<String, DocumentReference>()

        val usersCol = mockk<CollectionReference>()
        val userDoc = mockk<DocumentReference>()
        val favoritesCol = mockk<CollectionReference>()

        every { firestore.collection(FirestoreConstants.USERS_COLLECTION) } returns usersCol
        every { usersCol.document(userId) } returns userDoc
        every { userDoc.collection(FirestoreConstants.FAVORITES_COLLECTION) } returns favoritesCol

        every { favoritesCol.document(any()) } answers {
            val documentId = firstArg<String>()
            docRefsById.getOrPut(documentId) {
                mockk<DocumentReference> {
                    every { set(any<Map<String, Any>>()) } answers {
                        val data = firstArg<Map<String, Any>>()
                        remoteState[documentId] = data[FirestoreConstants.UPDATED_AT_FIELD] as Long
                        Tasks.forResult(null)
                    }
                    every { delete() } answers {
                        remoteState.remove(documentId)
                        Tasks.forResult(null)
                    }
                }
            }
        }

        every { favoritesCol.get() } answers {
            val documents =
                remoteState.map { (documentId, updatedAt) ->
                    mockk<DocumentSnapshot> {
                        every { getString(FirestoreConstants.TICKER_FIELD) } returns TICKER
                        every { getLong(FirestoreConstants.UPDATED_AT_FIELD) } returns updatedAt
                        every { getLong(FirestoreConstants.SYMBOL_ID_FIELD) } returns documentId.toLong()
                    }
                }
            Tasks.forResult(mockk<QuerySnapshot> { every { this@mockk.documents } returns documents })
        }

        val batch = mockk<WriteBatch>()
        every { firestore.batch() } returns batch
        every { batch.set(any(), any<Map<String, Any>>()) } answers {
            val data = secondArg<Map<String, Any>>()
            remoteState[(data[FirestoreConstants.SYMBOL_ID_FIELD] as Long).toString()] =
                data[FirestoreConstants.UPDATED_AT_FIELD] as Long
            batch
        }
        every { batch.delete(any()) } answers {
            val docRef = firstArg<DocumentReference>()
            docRefsById.entries.find { it.value === docRef }?.let { remoteState.remove(it.key) }
            batch
        }
        every { batch.commit() } returns Tasks.forResult(null)
    }

    /**
     * Блокирует выполнение переданного [block] ПОСЛЕ того, как реальная
     * SQLite-транзакция уже открыта. Один и тот же экземпляр используется
     * и для sync(), и для toggle() — второй вызов `database.withTransaction`
     * физически не может начать выполнение своей лямбды, пока не
     * закоммитится первая: это гарантия самого Room/SQLite, а не
     * что-то, что реализует этот класс. `continueTransaction` — общий
     * [CompletableDeferred]: после первого complete() он остаётся
     * завершённым для всех последующих await(), поэтому второй (заблоки-
     * рованный) вызов, дождавшись своей очереди на уровне Room, проходит
     * через continueTransaction.await() мгновенно.
     */
    private class BlockingTransactionRunner(
        private val database: CryptoCompareDatabase,
    ) : DatabaseTransactionRunner {
        val transactionStarted = CompletableDeferred<Unit>()
        val continueTransaction = CompletableDeferred<Unit>()

        override suspend fun <T> run(block: suspend () -> T): T =
            database.withTransaction {
                transactionStarted.complete(Unit)
                continueTransaction.await()
                block()
            }
    }
}
