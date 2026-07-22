package com.cryptocompare.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cryptocompare.data.local.CryptoCompareDatabase
import com.cryptocompare.data.local.migrations.AssetMigrations
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

    /*
     * Шаблон теста для следующей миграции — раскомментировать, подставив версии:
     *
     * @Test
     * fun migrate5To6() {
     *     helper.createDatabase(TEST_DB, 5).apply {
     *         execSQL("INSERT INTO favourite_tickers (userId, ticker, updatedAt) VALUES ('u', 'BTCUSDT', 1)")
     *         close()
     *     }
     *
     *     val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, *AssetMigrations.loadAll(context))
     *
     *     db.query("SELECT ticker FROM favourite_tickers").use { cursor ->
     *         assertTrue(cursor.moveToFirst())
     *         assertEquals("BTCUSDT", cursor.getString(0))
     *     }
     * }
     */

    private companion object {
        const val TEST_DB = "migration-test.db"
        const val CURRENT_VERSION = 5
    }
}
