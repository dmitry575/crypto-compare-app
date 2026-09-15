package com.cryptocompare.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cryptocompare.data.local.CryptoCompareDatabase
import com.cryptocompare.data.local.migrations.AssetMigrations
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Миграции проверяются только на устройстве: нужен настоящий SQLite.
 *
 * Экспортированные схемы лежат в `core/data/schemas` и подключены к ассетам
 * androidTest. Схема есть начиная с версии 5 — от неё и проверяем следующие
 * миграции. Как добавить новую, написано в `core/data/MIGRATIONS.md`.
 */
@RunWith(AndroidJUnit4::class)
class CryptoCompareDatabaseMigrationTest {
    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            CryptoCompareDatabase::class.java,
        )

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun allMigrationsFromAssetsAreDiscovered() {
        val migrations = AssetMigrations.loadAll(context)

        assertTrue("миграции не найдены в assets/migrations", migrations.isNotEmpty())
        // цепочка должна быть непрерывной, иначе Room не доведёт старую базу до текущей
        val sorted = migrations.sortedBy { it.startVersion }
        sorted.zipWithNext { current, next ->
            assertTrue(
                "разрыв в цепочке миграций: ${current.endVersion} -> ${next.startVersion}",
                current.endVersion == next.startVersion,
            )
        }
    }

    @Test
    fun currentSchemaOpensWithAllMigrationsApplied() {
        // создаём базу текущей версии из экспортированной схемы и открываем её
        // настоящим Room: так ловится расхождение схемы и Entity-классов
        helper.createDatabase(TEST_DB, CURRENT_VERSION).close()

        helper.runMigrationsAndValidate(
            TEST_DB,
            CURRENT_VERSION,
            true,
            *AssetMigrations.loadAll(context),
        )
    }

    @Test
    fun migrate5To6() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL("INSERT INTO favourite_tickers (userId, ticker, updatedAt) VALUES ('u', 'BTCUSDT', 1)")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, *AssetMigrations.loadAll(context))

        db.query("SELECT ticker FROM favourite_tickers").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("BTCUSDT", cursor.getString(0))
        }
        db.query("SELECT COUNT(*) FROM pending_favourite_operations").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migrate6To7KeepsFavouritesAndCatalog() {
        helper.createDatabase(TEST_DB, 6).apply {
            insertFavouritesAndPendingOperation()
            execSQL(
                "INSERT INTO providers (id, name, website, status, syncedAtMillis) VALUES (13, 'binance', NULL, 'Enabled', 1)",
            )
            execSQL(
                "INSERT INTO symbols " +
                    "(id, ticker, symbol, providerId, priceSell, priceBuy, updatedAt, syncedAtMillis) " +
                    "VALUES (1, 'solusdt', 'sol/usdt', 13, 103.35, 103.10, '2026-09-08T00:00:00Z', 1)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true, *AssetMigrations.loadAll(context))

        assertFavouritesAndPendingOperationSurvived(db)
        // 6→7 только добавляет колонки: строка каталога остаётся, новые поля пустые
        db.query("SELECT priceSell, change24h, volume24h, quoteVolume24h FROM symbols WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(103.35, cursor.getDouble(0), 0.0)
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
        }
    }

    @Test
    fun migrate7To8KeepsFavouritesAndRefetchesCatalog() {
        helper.createDatabase(TEST_DB, 7).apply {
            insertFavouritesAndPendingOperation()
            execSQL(
                "INSERT INTO providers (id, name, website, status, syncedAtMillis) VALUES (13, 'binance', NULL, 'Enabled', 1)",
            )
            execSQL(
                "INSERT INTO symbols " +
                    "(id, ticker, symbol, providerId, priceSell, priceBuy, updatedAt, syncedAtMillis) " +
                    "VALUES (1, 'solusdt', 'sol/usdt', 13, 103.35, 103.10, '2026-09-08T00:00:00Z', 1)",
            )
            execSQL("INSERT INTO catalog_remote_key (id, nextSkip, endReached) VALUES (0, 500, 0)")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 8, true, *AssetMigrations.loadAll(context))

        assertFavouritesAndPendingOperationSurvived(db)
        // строки каталога в старой форме значили другое — таблица пересоздана пустой,
        // а позиция подкачки сброшена, иначе медиатор докачивал бы с середины
        assertEquals(0, db.count("symbols"))
        assertEquals(0, db.count("catalog_remote_key"))
        assertEquals(1, db.count("providers"))
    }

    @Test
    fun migrate8To9AddsNetworkAndRefetchesCatalog() {
        helper.createDatabase(TEST_DB, 8).apply {
            insertFavouritesAndPendingOperation()
            execSQL(
                "INSERT INTO symbols (id, ticker, symbol, bestAskProviderId, bestAskPrice, bestBidProviderId, " +
                    "bestBidPrice, spreadPercent, updatedAt, syncedAtMillis) " +
                    "VALUES (143, 'ethusdc', 'eth/usdc', 18, 2498.35, 3, 2498.92, 0.02, '2026-09-15T00:00:00Z', 1)",
            )
            execSQL("INSERT INTO catalog_remote_key (id, nextSkip, endReached) VALUES (0, 500, 0)")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 9, true, *AssetMigrations.loadAll(context))

        assertFavouritesAndPendingOperationSurvived(db)
        // строка остаётся, сети у неё пока нет — придёт с перекачкой каталога
        db.query("SELECT network FROM symbols WHERE id = 143").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
        }
        // позиция подкачки сброшена: иначе медиатор не дойдёт до старых страниц,
        // и сети у них так и не появятся
        assertEquals(0, db.count("catalog_remote_key"))
    }

    @Test
    fun migrate5To8KeepsFavouritesAlongTheWholeChain() {
        // устройство, пропустившее несколько обновлений, проходит всю цепочку разом
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL("INSERT INTO favourite_tickers (userId, ticker, updatedAt) VALUES ('u', 'BTCUSDT', 1)")
            execSQL("INSERT INTO favourite_tickers (userId, ticker, updatedAt) VALUES ('u', 'SOLUSDT', 2)")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, CURRENT_VERSION, true, *AssetMigrations.loadAll(context))

        db.query("SELECT ticker FROM favourite_tickers ORDER BY ticker").use { cursor ->
            assertEquals(2, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals("BTCUSDT", cursor.getString(0))
            assertTrue(cursor.moveToNext())
            assertEquals("SOLUSDT", cursor.getString(0))
        }
    }

    private fun SupportSQLiteDatabase.insertFavouritesAndPendingOperation() {
        execSQL("INSERT INTO favourite_tickers (userId, ticker, updatedAt) VALUES ('u', 'BTCUSDT', 1)")
        execSQL("INSERT INTO favourite_tickers (userId, ticker, updatedAt) VALUES ('u', 'ETHUSDC', 2)")
        // офлайн-правка, ещё не доехавшая до Firestore: потерять её — значит молча
        // откатить действие пользователя
        execSQL(
            "INSERT INTO pending_favourite_operations (userId, ticker, operation, updatedAt) VALUES ('u', 'SOLUSDT', 'ADD', 3)",
        )
    }

    private fun assertFavouritesAndPendingOperationSurvived(db: SupportSQLiteDatabase) {
        db.query("SELECT ticker FROM favourite_tickers ORDER BY ticker").use { cursor ->
            assertEquals(2, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals("BTCUSDT", cursor.getString(0))
            assertTrue(cursor.moveToNext())
            assertEquals("ETHUSDC", cursor.getString(0))
        }
        db.query("SELECT ticker, operation FROM pending_favourite_operations").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("SOLUSDT", cursor.getString(0))
            assertEquals("ADD", cursor.getString(1))
        }
    }

    private fun SupportSQLiteDatabase.count(table: String): Int =
        query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private companion object {
        const val TEST_DB = "migration-test.db"

        /** Держать равной `version` в `@Database`: иначе тест открывает не ту схему, что у пользователя. */
        const val CURRENT_VERSION = 9
    }
}
