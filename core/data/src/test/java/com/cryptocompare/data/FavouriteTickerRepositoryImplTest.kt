package com.cryptocompare.data

import app.cash.turbine.test
import com.cryptocompare.data.local.dao.FavouriteTickerDao
import com.cryptocompare.data.local.entity.FavouriteTickerEntity
import com.cryptocompare.data.repository.FavouriteTickerRepositoryImpl
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
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavouriteTickerRepositoryImplTest {
    private val firestore: FirebaseFirestore = mockk()
    private val dao: FavouriteTickerDao = mockk()
    private val auth: FirebaseAuth = mockk(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()

    private val repository =
        FavouriteTickerRepositoryImpl(
            firestore = firestore,
            favouriteTickerDao = dao,
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
    fun `observeFavouriteTickers maps dao entities to ticker set when user is logged in`() =
        runTest {
            val userId = "user-1"
            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            val entities =
                listOf(
                    FavouriteTickerEntity(userId, "BTCUSDT", 1000L),
                    FavouriteTickerEntity(userId, "ETHUSDT", 2000L),
                )
            every { dao.observeUserTickers(userId) } returns flowOf(entities)

            repository.observeFavouriteTickers().test {
                assertEquals(setOf("BTCUSDT", "ETHUSDT"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observeFavouriteTickers emits empty set then ticker set as auth state changes`() =
        runTest {
            val userId = "user-1"
            val listenerSlot = slot<FirebaseAuth.AuthStateListener>()
            var currentUser: FirebaseUser? = null

            every { auth.currentUser } answers { currentUser }
            every { auth.addAuthStateListener(capture(listenerSlot)) } returns Unit
            every { auth.removeAuthStateListener(any()) } returns Unit

            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { dao.observeUserTickers(userId) } returns
                flowOf(listOf(FavouriteTickerEntity(userId, "BTCUSDT", 1000L)))

            repository.observeFavouriteTickers().test {
                // initial: no user → empty
                assertEquals(emptySet<String>(), awaitItem())

                // user logs in
                currentUser = user
                listenerSlot.captured.onAuthStateChanged(auth)
                assertEquals(setOf("BTCUSDT"), awaitItem())

                // user logs out
                currentUser = null
                listenerSlot.captured.onAuthStateChanged(auth)
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
            assertNotNull(result.exceptionOrNull())
        }

    @Test
    fun `toggleFavouriteTicker upserts to dao and firestore when ticker does not exist, returns true`() =
        runTest {
            val userId = "user-1"
            val ticker = "btcusdt"
            val normalized = "BTCUSDT"

            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            coEvery { dao.exists(userId, normalized) } returns false
            coEvery { dao.upsert(any()) } just runs

            val docRef = mockk<DocumentReference>()
            setupFavoritesDocPath(userId, normalized, docRef)
            every { docRef.set(any<Map<String, Any>>()) } returns Tasks.forResult(null)

            val result = repository.toggleFavouriteTicker(ticker)

            assertTrue(result.isSuccess)
            assertEquals(true, result.getOrNull())
            coVerify { dao.upsert(match { it.userId == userId && it.ticker == normalized }) }
            verify { docRef.set(any<Map<String, Any>>()) }
        }

    @Test
    fun `toggleFavouriteTicker deletes from dao and firestore when ticker exists, returns false`() =
        runTest {
            val userId = "user-1"
            val ticker = "ETHUSDT"

            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            coEvery { dao.exists(userId, ticker) } returns true
            coEvery { dao.delete(userId, ticker) } just runs

            val docRef = mockk<DocumentReference>()
            setupFavoritesDocPath(userId, ticker, docRef)
            every { docRef.delete() } returns Tasks.forResult(null)

            val result = repository.toggleFavouriteTicker(ticker)

            assertTrue(result.isSuccess)
            assertEquals(false, result.getOrNull())
            coVerify { dao.delete(userId, ticker) }
            verify { docRef.delete() }
        }

    @Test
    fun `toggleFavouriteTicker trims and normalizes ticker to uppercase`() =
        runTest {
            val userId = "user-1"
            val normalized = "ETHUSDT"

            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            coEvery { dao.exists(userId, normalized) } returns false
            coEvery { dao.upsert(any()) } just runs

            val docRef = mockk<DocumentReference>()
            setupFavoritesDocPath(userId, normalized, docRef)
            every { docRef.set(any<Map<String, Any>>()) } returns Tasks.forResult(null)

            repository.toggleFavouriteTicker("  ethusdt  ")

            coVerify { dao.upsert(match { it.ticker == normalized }) }
        }

    @Test
    fun `syncFavouriteTickers returns success without touching firestore when user is not authorized`() =
        runTest {
            every { auth.currentUser } returns null

            val result = repository.syncFavouriteTickers()

            assertTrue(result.isSuccess)
            verify(exactly = 0) { firestore.collection(any()) }
        }

    @Test
    fun `syncFavouriteTickers merges remote-only ticker into empty local db`() =
        runTest {
            val userId = "user-1"
            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            val remoteDoc = remoteDocSnapshot("BTCUSDT", 1000L)
            val querySnapshot = mockk<QuerySnapshot> { every { documents } returns listOf(remoteDoc) }

            val (favoritesCol) = setupFavoritesCollectionPath(userId)
            every { favoritesCol.get() } returns Tasks.forResult(querySnapshot)

            val docRef = mockk<DocumentReference>()
            every { favoritesCol.document("BTCUSDT") } returns docRef

            val batch = setupBatch(docRef)

            coEvery { dao.getUserTickers(userId) } returns emptyList()
            coEvery { dao.replaceAll(userId, any()) } just runs

            val result = repository.syncFavouriteTickers()

            assertTrue(result.isSuccess)
            coVerify {
                dao.replaceAll(userId, match { it.size == 1 && it[0].ticker == "BTCUSDT" })
            }
            verify { batch.commit() }
        }

    @Test
    fun `syncFavouriteTickers keeps local ticker when timestamps are equal`() =
        runTest {
            val userId = "user-1"
            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            val remoteDoc = remoteDocSnapshot("BTCUSDT", 1000L)
            val querySnapshot = mockk<QuerySnapshot> { every { documents } returns listOf(remoteDoc) }
            val localEntity = FavouriteTickerEntity(userId, "BTCUSDT", 1000L)

            val (favoritesCol) = setupFavoritesCollectionPath(userId)
            every { favoritesCol.get() } returns Tasks.forResult(querySnapshot)

            val docRef = mockk<DocumentReference>()
            every { favoritesCol.document("BTCUSDT") } returns docRef
            setupBatch(docRef)

            coEvery { dao.getUserTickers(userId) } returns listOf(localEntity)
            coEvery { dao.replaceAll(userId, any()) } just runs

            repository.syncFavouriteTickers()

            coVerify {
                // same reference: local was chosen because local.updatedAt >= remote.updatedAt
                dao.replaceAll(userId, match { it.size == 1 && it[0] === localEntity })
            }
        }

    @Test
    fun `syncFavouriteTickers keeps remote ticker when remote is newer`() =
        runTest {
            val userId = "user-1"
            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            val remoteDoc = remoteDocSnapshot("BTCUSDT", 2000L)
            val querySnapshot = mockk<QuerySnapshot> { every { documents } returns listOf(remoteDoc) }
            val localEntity = FavouriteTickerEntity(userId, "BTCUSDT", 1000L)

            val (favoritesCol) = setupFavoritesCollectionPath(userId)
            every { favoritesCol.get() } returns Tasks.forResult(querySnapshot)

            val docRef = mockk<DocumentReference>()
            every { favoritesCol.document("BTCUSDT") } returns docRef
            setupBatch(docRef)

            coEvery { dao.getUserTickers(userId) } returns listOf(localEntity)
            coEvery { dao.replaceAll(userId, any()) } just runs

            repository.syncFavouriteTickers()

            coVerify {
                dao.replaceAll(userId, match { it.size == 1 && it[0].updatedAt == 2000L })
            }
        }

    @Test
    fun `syncFavouriteTickers keeps local-only ticker absent from remote`() =
        runTest {
            val userId = "user-1"
            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            val querySnapshot = mockk<QuerySnapshot> { every { documents } returns emptyList() }
            val localEntity = FavouriteTickerEntity(userId, "SOLUSDT", 500L)

            val (favoritesCol) = setupFavoritesCollectionPath(userId)
            every { favoritesCol.get() } returns Tasks.forResult(querySnapshot)

            val docRef = mockk<DocumentReference>()
            every { favoritesCol.document("SOLUSDT") } returns docRef
            setupBatch(docRef)

            coEvery { dao.getUserTickers(userId) } returns listOf(localEntity)
            coEvery { dao.replaceAll(userId, any()) } just runs

            repository.syncFavouriteTickers()

            coVerify {
                dao.replaceAll(userId, match { it.size == 1 && it[0].ticker == "SOLUSDT" })
            }
        }

    @Test
    fun `syncFavouriteTickers merges two disjoint remote and local tickers`() =
        runTest {
            val userId = "user-1"
            val user = mockk<FirebaseUser> { every { uid } returns userId }
            every { auth.currentUser } returns user

            val remoteDoc = remoteDocSnapshot("BTCUSDT", 1000L)
            val querySnapshot = mockk<QuerySnapshot> { every { documents } returns listOf(remoteDoc) }
            val localEntity = FavouriteTickerEntity(userId, "ETHUSDT", 2000L)

            val (favoritesCol) = setupFavoritesCollectionPath(userId)
            every { favoritesCol.get() } returns Tasks.forResult(querySnapshot)

            val btcDocRef = mockk<DocumentReference>()
            val ethDocRef = mockk<DocumentReference>()
            every { favoritesCol.document("BTCUSDT") } returns btcDocRef
            every { favoritesCol.document("ETHUSDT") } returns ethDocRef
            setupBatch(btcDocRef, ethDocRef)

            coEvery { dao.getUserTickers(userId) } returns listOf(localEntity)
            coEvery { dao.replaceAll(userId, any()) } just runs

            repository.syncFavouriteTickers()

            coVerify {
                dao.replaceAll(
                    userId,
                    match { list ->
                        list.size == 2 &&
                            list.any { it.ticker == "BTCUSDT" } &&
                            list.any { it.ticker == "ETHUSDT" }
                    },
                )
            }
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

    private fun setupFavoritesDocPath(
        userId: String,
        ticker: String,
        docRef: DocumentReference,
    ) {
        val (favoritesCol) = setupFavoritesCollectionPath(userId)
        every { favoritesCol.document(ticker) } returns docRef
    }

    private fun setupBatch(vararg docRefs: DocumentReference): WriteBatch {
        val batch = mockk<WriteBatch>()
        every { firestore.batch() } returns batch
        docRefs.forEach { every { batch.set(it, any<Map<String, Any>>()) } returns batch }
        every { batch.commit() } returns Tasks.forResult(null)
        return batch
    }

    private fun remoteDocSnapshot(
        ticker: String,
        updatedAt: Long,
    ): DocumentSnapshot =
        mockk {
            every { getString("ticker") } returns ticker
            every { getLong("updatedAt") } returns updatedAt
        }
}
