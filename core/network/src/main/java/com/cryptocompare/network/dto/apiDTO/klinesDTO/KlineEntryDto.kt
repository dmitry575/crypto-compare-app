package com.cryptocompare.network.dto.apiDTO.klinesDTO

/**
 * Одна свеча из ответа бэкенда. `openTime` приходит ISO-8601 строкой в UTC
 * (`2026-08-23T14:28:19.1007838Z`) — в миллисекунды её переводит маппер слоя
 * данных, DTO хранит сырой вид.
 */
data class KlineEntryDto(
    val openTime: String?,
    val openPrice: Double?,
    val highPrice: Double?,
    val lowPrice: Double?,
    val closePrice: Double?,
    val volume: Double?,
)
