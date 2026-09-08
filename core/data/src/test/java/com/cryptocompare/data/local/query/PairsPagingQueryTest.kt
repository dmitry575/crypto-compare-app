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
    fun `sorting by name needs no tiebreaker because the name is unique`() {
        val sql = build(sorting = CatalogSorting(CatalogSort.NAME, ascending = true)).sql

        assertTrue(sql.contains("ORDER BY ticker ASC"))
    }

    @Test
    fun `every other field falls back to the ticker so paging stays stable`() {
        // Paging листает через LIMIT/OFFSET: при равных значениях порядок должен
        // быть одинаковым между запросами, иначе строки дублируются и пропадают
        listOf(CatalogSort.PRICE, CatalogSort.CHANGE, CatalogSort.SPREAD, CatalogSort.VOLUME)
            .forEach { field ->
                val sql = build(sorting = CatalogSorting(field, ascending = false)).sql

                assertTrue("нет добора по тикеру для $field", sql.contains(", ticker ASC"))
            }
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
    fun `spread keeps its sign`() {
        val sql = build().sql

        // (продажа - покупка) / покупка, а не модуль разницы: раньше стояло
        // (max - min) / min по обоим полям сразу, и 85% каталога показывали
        // знак наоборот
        assertTrue(sql.contains("(MAX(priceSell) - MIN(priceBuy)) * 100.0 / MIN(priceBuy)"))
        assertFalse("модуль разницы вернулся", sql.contains("MAX(MAX(priceBuy, priceSell))"))
    }

    @Test
    fun `no price means no spread, not a zero`() {
        // ELSE 0 утверждал бы «спреда нет», хотя его просто не из чего считать
        val sql = build().sql
        val spreadCase = sql.substringAfter("CASE").substringBefore("END AS spreadPercent")

        assertFalse("ELSE в выражении спреда", spreadCase.contains("ELSE"))
    }

    @Test
    fun `fields that are never null do not pay for a null check`() {
        listOf(CatalogSort.NAME, CatalogSort.PRICE).forEach { field ->
            val sql = build(sorting = CatalogSorting(field, ascending = true)).sql

            assertFalse("лишняя проверка на NULL для $field", sql.contains("IS NULL),"))
        }
    }

    @Test
    fun `an empty favourites list still produces valid sql`() {
        // IN () — синтаксическая ошибка; IN (NULL) не совпадает ни с чем
        val sql = build(favouriteTickers = emptyList()).sql

        assertTrue(sql.contains("IN (NULL)"))
    }

    @Test
    fun `each favourite gets its own placeholder`() {
        val query = build(favouriteTickers = listOf("BTCUSDT", "ETHUSDT", "SOLUSDT"))

        assertTrue(query.sql.contains("IN (?,?,?)"))
        // запрос, запрос, флаг избранного, три тикера, три имени направления
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
    fun `direction is filtered in having because it is an aggregate`() {
        val sql = build().sql

        // «растёт» — свойство пары целиком, а строки таблицы это отдельные биржи
        assertTrue(sql.contains("HAVING"))
        assertTrue(sql.indexOf("HAVING") > sql.indexOf("GROUP BY"))
    }

    private fun build(
        query: String = "",
        onlyFavourite: Boolean = false,
        favouriteTickers: List<String> = emptyList(),
        direction: CatalogDirection = CatalogDirection.ANY,
        sorting: CatalogSorting = CatalogSorting(),
    ) = PairsPagingQuery.build(
        query = query,
        onlyFavourite = onlyFavourite,
        favouriteTickers = favouriteTickers,
        direction = direction,
        sorting = sorting,
    )
}
