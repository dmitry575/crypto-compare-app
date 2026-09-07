package com.cryptocompare.model.symbol

/**
 * Сортировка каталога: поле и направление.
 *
 * Пара, а не два отдельных параметра: направление без поля бессмысленно, и
 * протаскивать их порознь через четыре слоя значило бы каждый раз проверять,
 * что они согласованы.
 */
data class CatalogSorting(
    val field: CatalogSort = CatalogSort.NAME,
    val ascending: Boolean = true,
)
