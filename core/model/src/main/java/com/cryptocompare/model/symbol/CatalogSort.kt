package com.cryptocompare.model.symbol

/** По какому полю сортируется каталог. */
enum class CatalogSort {
    /** По тикеру, как в алфавите. Порядок по умолчанию. */
    NAME,

    PRICE,

    /** Изменение за 24 часа. */
    CHANGE,

    /** Разброс цены между биржами. */
    SPREAD,

    /** Объём за 24 часа в котируемом активе. */
    VOLUME,
}
