package com.cryptocompare.data.local.query

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.cryptocompare.model.symbol.CatalogDirection
import com.cryptocompare.model.symbol.CatalogSort
import com.cryptocompare.model.symbol.CatalogSorting

/**
 * Запрос каталога: агрегат по тикеру плюс фильтры и сортировка.
 *
 * Собирается строкой, а не живёт в `@Query`, потому что Room не подставляет
 * `ORDER BY` параметром — поле сортировки это часть синтаксиса, а не значение.
 * Плата за это — потеря проверки SQL на компиляции, поэтому построитель покрыт
 * тестами, а вся подстановка идёт через `?`: наружу попадают только имена
 * колонок, выбранные из [CatalogSort], пользовательский ввод в текст запроса
 * не склеивается никогда.
 */
internal object PairsPagingQuery {
    /**
     * Изменение за 24ч: наибольшее по модулю среди бирж, знак сохраняется.
     *
     * Не среднее — биржа с протухшими котировками весила бы в AVG столько же,
     * сколько основной рынок, и гасила бы реальное движение.
     */
    private const val CHANGE_EXPRESSION =
        """
        CASE
            WHEN ABS(MAX(change24h)) >= ABS(MIN(change24h)) THEN MAX(change24h)
            ELSE MIN(change24h)
        END
        """

    /**
     * Цены и спред пары.
     *
     * Спред больше не считается здесь — он приходит с бэкенда готовым, и там же
     * отсеиваются протухшие котировки, которые иначе выигрывали бы сравнение.
     * Приложение только выбирает лучшее по тикеру.
     *
     * Агрегаты нужны из-за `GROUP BY UPPER(ticker)`: одна пара может торговаться
     * в нескольких сетях, и строк на тикер бывает больше одной. Для единственной
     * строки все три `MIN`/`MAX` возвращают её собственные значения.
     */
    private const val SELECT_AND_FROM =
        """
        SELECT
            UPPER(ticker) AS ticker,
            MIN(bestAskPrice) AS buyPrice,
            MAX(bestBidPrice) AS sellPrice,
            MAX(spreadPercent) AS spreadPercent,
            SUM(quoteVolume24h) AS quoteVolume24h,
            $CHANGE_EXPRESSION AS change24h
        FROM symbols
        """

    fun build(
        query: String,
        onlyFavourite: Boolean,
        favouriteTickers: List<String>,
        direction: CatalogDirection,
        sorting: CatalogSorting,
    ): SupportSQLiteQuery {
        val args = mutableListOf<Any>()

        args += query
        args += query
        args += if (onlyFavourite) 1 else 0

        // IN () — синтаксическая ошибка, поэтому пустой список превращается в NULL:
        // такой IN не совпадает ни с чем, что и требуется
        val favouritePlaceholders =
            if (favouriteTickers.isEmpty()) {
                "NULL"
            } else {
                favouriteTickers.joinToString(separator = ",") { "?" }
            }
        args.addAll(favouriteTickers)

        repeat(DIRECTION_ARG_COUNT) { args += direction.name }

        val sql =
            """
            $SELECT_AND_FROM
            WHERE ticker IS NOT NULL AND TRIM(ticker) != ''
                AND (? = '' OR ticker LIKE '%' || ? || '%')
                AND (? = 0 OR UPPER(ticker) IN ($favouritePlaceholders))
            GROUP BY UPPER(ticker)
            -- направление отбирается в HAVING: «растёт» это свойство пары целиком,
            -- а строки таблицы — отдельные биржи. Выражение повторено дословно, потому
            -- что имя change24h здесь означало бы колонку, а не результат агрегата
            HAVING ? = 'ANY'
                OR (? = '${CatalogDirection.GAINERS.name}' AND $CHANGE_EXPRESSION > 0)
                OR (? = '${CatalogDirection.LOSERS.name}' AND $CHANGE_EXPRESSION < 0)
            ORDER BY ${orderBy(sorting)}
            """.trimIndent()

        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }

    /**
     * В ORDER BY, в отличие от HAVING, SQLite сначала ищет имя среди псевдонимов
     * выходных колонок — поэтому здесь достаточно имени, повторять выражение не нужно.
     *
     * Нулевые значения всегда уезжают в конец: `NULLS LAST` появился в SQLite 3.30,
     * то есть с API 30, а minSdk у нас 26. Отдельное выражение `(x IS NULL)` работает
     * везде.
     *
     * Тикер в хвосте — не украшение: Paging листает через LIMIT/OFFSET, и при
     * одинаковых значениях сортируемого поля порядок между запросами должен быть
     * устойчивым, иначе строки будут дублироваться и пропадать между страницами.
     */
    private fun orderBy(sorting: CatalogSorting): String {
        val order = if (sorting.ascending) "ASC" else "DESC"

        val column =
            when (sorting.field) {
                CatalogSort.NAME -> "ticker"
                CatalogSort.PRICE -> "buyPrice"
                CatalogSort.CHANGE -> "change24h"
                CatalogSort.SPREAD -> "spreadPercent"
                CatalogSort.VOLUME -> "quoteVolume24h"
            }

        val nullsLast =
            when (sorting.field) {
                CatalogSort.CHANGE, CatalogSort.VOLUME, CatalogSort.SPREAD -> "($column IS NULL), "
                else -> ""
            }

        return if (sorting.field == CatalogSort.NAME) {
            "$column $order"
        } else {
            "$nullsLast$column $order, ticker ASC"
        }
    }

    /** Сколько раз имя направления подставляется в HAVING. */
    private const val DIRECTION_ARG_COUNT = 3
}
