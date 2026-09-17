package com.cryptocompare.data.local.query

import com.cryptocompare.model.symbol.CatalogDirection
import com.cryptocompare.model.symbol.CatalogSort
import com.cryptocompare.model.symbol.CatalogSorting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Запрос каталога собирается строкой, поэтому Room больше не проверяет его на
 * компиляции — эти тесты и есть замена той проверке.
 */
class PairsPagingQueryTest {
    @Test
    fun `sorting by name falls back to the symbol because a ticker has several networks`() {
        // у ETHUSDC строк столько, сколько наборов сетей: без добора по символу
        // их порядок между страницами не определён
        val sql = build(sorting = CatalogSorting(CatalogSort.NAME, ascending = true)).sql

        assertTrue(sql.contains("ORDER BY ticker ASC, symbolId ASC"))
    }

    @Test
    fun `every other field falls back to ticker and symbol so paging stays stable`() {
        // Paging листает через LIMIT/OFFSET: при равных значениях порядок должен
        // быть одинаковым между запросами, иначе строки дублируются и пропадают
        listOf(CatalogSort.PRICE, CatalogSort.CHANGE, CatalogSort.SPREAD, CatalogSort.VOLUME)
            .forEach { field ->
                val sql = build(sorting = CatalogSorting(field, ascending = false)).sql

                assertTrue("нет добора для $field", sql.contains(", ticker ASC, symbolId ASC"))
            }
    }

    @Test
    fun `a row per symbol, not per ticker`() {
        val sql = build().sql

        // сводка по тикеру смешивала сети: покупку из одной, спред из другой
        assertFalse("каталог снова сводит символы по тикеру", sql.contains("GROUP BY UPPER(ticker)\n            ORDER"))
        assertTrue(sql.contains("symbols.id AS symbolId"))
        assertTrue(sql.contains("counts.networkCount AS networkCount"))
        assertTrue(sql.contains("symbols.network AS network"))
    }

    @Test
    fun `direction flips the order`() {
        val ascending = build(sorting = CatalogSorting(CatalogSort.PRICE, ascending = true)).sql
        val descending = build(sorting = CatalogSorting(CatalogSort.PRICE, ascending = false)).sql

        assertTrue(ascending.contains("buyPrice ASC"))
        assertTrue(descending.contains("buyPrice DESC"))
    }

    @Test
    fun `nullable fields push empty values to the end in both directions`() {
        // NULLS LAST появился в SQLite 3.30, то есть с API 30, а minSdk у нас 26
        listOf(
            CatalogSort.CHANGE to "change24h",
            CatalogSort.VOLUME to "quoteVolume24h",
            CatalogSort.SPREAD to "spreadPercent",
        ).forEach { (field, column) ->
            listOf(true, false).forEach { ascending ->
                val sql = build(sorting = CatalogSorting(field, ascending)).sql

                assertTrue("$field/$ascending", sql.contains("($column IS NULL),"))
                assertFalse("NULLS LAST недоступен на minSdk 26", sql.contains("NULLS LAST"))
            }
        }
    }

    @Test
    fun `zero volume is treated as missing so it shows a dash and sorts last`() {
        val sql = build(sorting = CatalogSorting(CatalogSort.VOLUME, ascending = true)).sql

        // без NULLIF пары с нулём вставали бы в начало сортировки по возрастанию
        // перед настоящими малыми объёмами, а в строке рисовался бы голый «0»
        assertTrue(sql.contains("NULLIF(symbols.quoteVolume24h, 0) AS quoteVolume24h"))
    }

    @Test
    fun `spread is taken from the column, not recomputed`() {
        val sql = build().sql

        // считает бэкенд: там же отсеиваются протухшие котировки, которые
        // иначе выигрывали бы сравнение и рисовали арбитраж на пустом месте
        assertTrue(sql.contains("symbols.spreadPercent AS spreadPercent"))
        assertFalse("спред снова считается в SQL", sql.contains("* 100.0 /"))
    }

    @Test
    fun `prices keep their sides`() {
        val sql = build().sql

        // покупка это минимальный ask, продажа — максимальный bid;
        // перепутанные местами, они переворачивают знак спреда
        assertTrue(sql.contains("symbols.bestAskPrice AS buyPrice"))
        assertTrue(sql.contains("symbols.bestBidPrice AS sellPrice"))
    }

    @Test
    fun `an empty favourites list still produces valid sql`() {
        // IN () — синтаксическая ошибка; IN (NULL) не совпадает ни с чем
        val sql = build(favouriteSymbolIds = emptyList()).sql

        assertTrue(sql.contains("IN (NULL)"))
    }

    @Test
    fun `each favourite gets its own placeholder`() {
        val query = build(favouriteSymbolIds = listOf(1L, 2L, 3L))

        assertTrue(query.sql.contains("IN (?,?,?)"))
        // запрос, запрос, флаг избранного, три id символов, три имени направления
        assertEquals(9, query.argCount)
    }

    @Test
    fun `nothing but placeholders carries user input`() {
        val sql = build(query = "'; DROP TABLE symbols; --").sql

        assertFalse("пользовательский ввод склеен в текст запроса", sql.contains("DROP TABLE"))
    }

    @Test
    fun `direction is compared against the enum names the model actually has`() {
        val sql = build(direction = CatalogDirection.GAINERS).sql

        assertTrue(sql.contains("'${CatalogDirection.GAINERS.name}'"))
        assertTrue(sql.contains("'${CatalogDirection.LOSERS.name}'"))
    }

    @Test
    fun `direction is filtered on the symbol row itself`() {
        val sql = build(direction = CatalogDirection.GAINERS).sql

        // строка теперь один символ: агрегата и HAVING больше нет
        assertFalse(sql.contains("HAVING"))
        assertTrue(sql.contains("symbols.change24h > 0"))
        assertTrue(sql.contains("symbols.change24h < 0"))
    }

    private fun build(
        query: String = "",
        onlyFavourite: Boolean = false,
        favouriteSymbolIds: List<Long> = emptyList(),
        direction: CatalogDirection = CatalogDirection.ANY,
        sorting: CatalogSorting = CatalogSorting(),
    ) = PairsPagingQuery.build(
        query = query,
        onlyFavourite = onlyFavourite,
        favouriteSymbolIds = favouriteSymbolIds,
        direction = direction,
        sorting = sorting,
    )
}
