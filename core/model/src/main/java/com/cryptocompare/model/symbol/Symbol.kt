package com.cryptocompare.model.symbol

data class Symbol(
    val id: Long,
    val ticker: String?,
    val symbol: String?,
    val providerId: Int,
    val priceSell: Double,
    val priceBuy: Double,
    /** Объём за 24ч в базовом активе: BTC для BTC/USDT. Между парами не сравним. */
    val volume24h: Double? = null,
    /** Объём за 24ч в котируемом активе: USDT для BTC/USDT. Сравним между парами. */
    val quoteVolume24h: Double? = null,
    /** Изменение цены за 24ч в процентах: 2.35 = +2.35 %. */
    val change24h: Double? = null,
    /**
     * Когда биржа отдала эту котировку, в миллисекундах эпохи.
     *
     * Разбивка по биржам приходит **без фильтра свежести**, в отличие от
     * best-выдачи: на btcusdt 2026-09-08 у одной биржи стояла цена двухчасовой
     * давности, и она же была самой выгодной на вид. `null` — бэкенд прислал
     * время, которое не разобрать.
     */
    val quotedAtMillis: Long? = null,
)
