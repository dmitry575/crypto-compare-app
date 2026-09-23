package com.cryptocompare.pairs.viewmodel.mainViewModel

import com.cryptocompare.model.error.AppError
import com.cryptocompare.model.symbol.CatalogDirection
import com.cryptocompare.model.symbol.CatalogSorting
import com.cryptocompare.pairs.util.StreamStatus

data class MainUiState(
    val searchQuery: String = "",
    /** Живые ли цены. На старте — «подключение»: сокет открывается вместе с каталогом. */
    val streamStatus: StreamStatus = StreamStatus.RECONNECTING,
    /**
     * Когда цены последний раз менялись: тик сокета, догонка через REST, а на
     * холодном старте — время последней загрузки каталога из базы.
     */
    val lastUpdateMillis: Long? = null,
    /** Поток лежит дольше, чем стоит списывать на короткий разрыв. */
    val isStale: Boolean = false,
    /**
     * «Обновить» нажали, но ни одна котировка не приехала. Разовое событие:
     * экран показывает снекбар и гасит флаг.
     */
    val refreshFailed: Boolean = false,
    val error: AppError? = null,
    val subscribedTickers: Set<String> = emptySet(),
    /** Избранное — по символам: у тикера их столько, сколько наборов сетей. */
    val favouriteSymbolIds: Set<Long> = emptySet(),
    val onlyFavourite: Boolean = false,
    /**
     * Гость попросил избранное. Экран показывает приглашение войти и гасит
     * флаг: это разовое событие, а не состояние.
     */
    val signInRequired: Boolean = false,
    /**
     * Направление за 24ч. Независимо от [onlyFavourite]: «избранное, которое
     * растёт» — это ровно тот вопрос, ради которого избранное и заводят.
     */
    val direction: CatalogDirection = CatalogDirection.ANY,
    val sorting: CatalogSorting = CatalogSorting(),
)
