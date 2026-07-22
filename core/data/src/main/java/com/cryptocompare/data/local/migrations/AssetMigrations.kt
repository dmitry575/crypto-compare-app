package com.cryptocompare.data.local.migrations

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cryptocompare.data.util.DataConstants

/**
 * Room-миграции из sql-файлов в assets: каждая миграция лежит в
 * assets/migrations/migration_<from>_<to>.sql, стейтменты разделяются ";".
 *
 * Ограничение сплиттера: ";" внутри строковых литералов и тел триггеров
 * не поддерживается — такие миграции пишите кодом.
 */
object AssetMigrations {
    private val fileNameRegex = Regex(DataConstants.Migrations.FILE_NAME_PATTERN)

    fun createMigration(
        from: Int,
        to: Int,
        context: Context,
    ): Migration =
        object : Migration(from, to) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val fileName = "${DataConstants.Migrations.ASSETS_DIR}/migration_${from}_$to.sql"
                val sql =
                    runCatching {
                        context.assets
                            .open(fileName)
                            .bufferedReader()
                            .use { it.readText() }
                    }.getOrElse { error ->
                        throw IllegalStateException("Missing migration asset $fileName", error)
                    }

                sql
                    .lineSequence()
                    .filterNot { it.trim().startsWith(DataConstants.Migrations.COMMENT_PREFIX) }
                    .joinToString("\n")
                    .split(DataConstants.Migrations.STATEMENT_SEPARATOR)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { statement -> db.execSQL(statement) }
            }
        }

    // собирает все миграции из assets/migrations по именам файлов —
    // новая миграция подключается добавлением одного sql-файла
    fun loadAll(context: Context): Array<Migration> =
        context.assets
            .list(DataConstants.Migrations.ASSETS_DIR)
            .orEmpty()
            .mapNotNull { fileName ->
                fileNameRegex.matchEntire(fileName)?.destructured?.let { (from, to) ->
                    createMigration(from = from.toInt(), to = to.toInt(), context = context)
                }
            }.toTypedArray()
}
