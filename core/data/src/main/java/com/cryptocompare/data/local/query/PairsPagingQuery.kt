package com.cryptocompare.data.local.query

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.cryptocompare.model.symbol.CatalogDirection
import com.cryptocompare.model.symbol.CatalogSort
import com.cryptocompare.model.symbol.CatalogSorting

/**
 * Запрос каталога: строка на символ плюс фильтры и сортировка.
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
     * Цены и спред символа.
     *
     * Спред не считается здесь — он приходит с бэкенда готовым, и там же
     * отсеиваются протухшие котировки, которые иначе выигрывали бы сравнение.
     *
     * **Строка на символ, а не на тикер.** Раньше запрос сводил `GROUP BY
     * UPPER(ticker)` все символы тикера в одну строку, а символы — это одна пара
     * в разных наборах сетей: USDC в Ethereum и USDC в Solana — разные активы.
     * Сводная строка брала покупку из одной сети, а спред — из другой. Теперь
     * каждый символ отдельно, а `networkCount` говорит строке, что у тикера есть
     * соседи и её надо пометить сетями. Считается одним подзапросом с группировкой,
     * а не на каждую строку: оконных функций в SQLite до API 30 нет.
     *
     * Нулевой объём — это «биржа не отдала статистику», а не «торгов не было»:
     * на 2026-09-14 так у 18 строк каталога, и у `athbtc` при нуле объёма
     * изменение за сутки +16.67%. Показанный как есть, он рисовался голым «0»,
     * а сортировка по объёму ставила такие пары перед настоящими малыми объёмами.
     */
    private const val SELECT_AND_FROM =
        """
        SELECT
            symbols.id AS symbolId,
            UPPER(symbols.ticker) AS ticker,
            symbols.symbol AS symbol,
            symbols.bestAskPrice AS buyPrice,
            symbols.bestBidPrice AS sellPrice,
            symbols.spreadPercent AS spreadPercent,
            NULLIF(symbols.quoteVolume24h, 0) AS quoteVolume24h,
            symbols.change24h AS change24h,
            symbols.network AS network,
            counts.networkCount AS networkCount
        FROM symbols
        JOIN (
            SELECT UPPER(ticker) AS countedTicker, COUNT(*) AS networkCount
            FROM symbols
            GROUP BY UPPER(ticker)
        ) AS counts ON counts.countedTicker = UPPER(symbols.ticker)
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
            WHERE symbols.ticker IS NOT NULL AND TRIM(symbols.ticker) != ''
                AND (? = '' OR symbols.ticker LIKE '%' || ? || '%')
                AND (? = 0 OR UPPER(symbols.ticker) IN ($favouritePlaceholders))
                AND (
                    ? = 'ANY'
                    OR (? = '${CatalogDirection.GAINERS.name}' AND symbols.change24h > 0)
                    OR (? = '${CatalogDirection.LOSERS.name}' AND symbols.change24h < 0)
                )
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
     * Тикер и символ в хвосте — не украшение: Paging листает через LIMIT/OFFSET,
     * и при одинаковых значениях сортируемого поля порядок между запросами должен
     * быть устойчивым, иначе строки будут дублироваться и пропадать между
     * страницами. Символ нужен и при сортировке по имени: тикер больше не
     * уникален, у ETHUSDC строк столько, сколько наборов сетей.
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
            "$column $order, symbolId ASC"
        } else {
            "$nullsLast$column $order, ticker ASC, symbolId ASC"
        }
    }

    /** Сколько раз имя направления подставляется в HAVING. */
    private const val DIRECTION_ARG_COUNT = 3
}
