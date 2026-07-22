package com.cryptocompare.network.api

import com.cryptocompare.network.dto.apiDTO.historyDTO.GetHistoryResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Сторонний CryptoCompare min-api: история цен по паре.
 *
 * Биржа намеренно не передаётся — берём агрегат CCCAGG, чтобы на экран
 * приходился ровно один запрос. Промежуточные масштабы (15м, 4ч, неделя)
 * получаются параметром [aggregate] поверх минутного/часового/дневного ряда.
 */
interface CryptoCompareHistoryApi {
    @GET("data/v2/histominute")
    suspend fun getMinuteHistory(
        @Query("fsym") fromSymbol: String,
        @Query("tsym") toSymbol: String,
        @Query("limit") limit: Int,
        @Query("aggregate") aggregate: Int,
    ): GetHistoryResponse

    @GET("data/v2/histohour")
    suspend fun getHourHistory(
        @Query("fsym") fromSymbol: String,
        @Query("tsym") toSymbol: String,
        @Query("limit") limit: Int,
        @Query("aggregate") aggregate: Int,
    ): GetHistoryResponse

    @GET("data/v2/histoday")
    suspend fun getDailyHistory(
        @Query("fsym") fromSymbol: String,
        @Query("tsym") toSymbol: String,
        @Query("limit") limit: Int,
        @Query("aggregate") aggregate: Int,
    ): GetHistoryResponse
}
