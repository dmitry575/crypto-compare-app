package com.cryptocompare.network.di

import com.cryptocompare.network.BuildConfig
import com.cryptocompare.network.api.CryptoCompareApi
import com.cryptocompare.network.api.CryptoCompareHistoryApi
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    @Named("baseUrl")
    fun provideBaseUrl(): String = BuildConfig.BASE_URL

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    @Named("ioDispatcher")
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .pingInterval(20, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideCryptoCompareApi(
        gson: Gson,
        @Named("baseUrl") baseUrl: String,
    ): CryptoCompareApi =
        Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(CryptoCompareApi::class.java)

    // отдельный клиент под сторонний min-api: другой хост и ключ в заголовке
    @Provides
    @Singleton
    @Named("historyOkHttpClient")
    fun provideHistoryOkHttpClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                val apiKey = BuildConfig.CRYPTOCOMPARE_API_KEY
                val request =
                    if (apiKey.isBlank()) {
                        chain.request()
                    } else {
                        chain
                            .request()
                            .newBuilder()
                            .header("Authorization", "Apikey $apiKey")
                            .build()
                    }
                chain.proceed(request)
            }.build()

    @Provides
    @Singleton
    fun provideCryptoCompareHistoryApi(
        gson: Gson,
        @Named("historyOkHttpClient") okHttpClient: OkHttpClient,
    ): CryptoCompareHistoryApi =
        Retrofit
            .Builder()
            .baseUrl(BuildConfig.HISTORY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(CryptoCompareHistoryApi::class.java)
}
