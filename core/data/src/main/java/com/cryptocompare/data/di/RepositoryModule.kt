package com.cryptocompare.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.cryptocompare.data.BuildConfig
import com.cryptocompare.data.local.CryptoCompareDatabase
import com.cryptocompare.data.local.dao.FavouriteTickerDao
import com.cryptocompare.data.local.dao.ProviderDao
import com.cryptocompare.data.local.dao.SymbolDao
import com.cryptocompare.data.repository.AuthRepositoryImpl
import com.cryptocompare.data.repository.CryptoCompareRepositoryImpl
import com.cryptocompare.data.repository.FavouriteTickerRepositoryImpl
import com.cryptocompare.data.repository.ThemeRepositoryImpl
import com.cryptocompare.data.repository.TickerStreamRepositoryImpl
import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.domain.repository.FavouriteTickerRepository
import com.cryptocompare.domain.repository.ThemeRepository
import com.cryptocompare.domain.repository.TickerStreamRepository
import com.cryptocompare.network.api.CryptoCompareApi
import com.cryptocompare.network.api.CryptoCompareHistoryApi
import com.cryptocompare.network.websocket.WebSocketClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    @Named("wsUrl")
    fun provideWebSocketUrl(): String = BuildConfig.WS_BASE_URL

    @Provides
    @Singleton
    fun provideCryptoCompareRepository(
        api: CryptoCompareApi,
        historyApi: CryptoCompareHistoryApi,
        database: CryptoCompareDatabase,
        providerDao: ProviderDao,
        symbolDao: SymbolDao,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher,
    ): CryptoCompareRepository =
        CryptoCompareRepositoryImpl(api, historyApi, database, symbolDao, providerDao, ioDispatcher)

    @Provides
    @Singleton
    fun provideAuthRepository(auth: FirebaseAuth): AuthRepository = AuthRepositoryImpl(auth)

    @Provides
    @Singleton
    fun provideThemeRepository(dataStore: DataStore<Preferences>): ThemeRepository = ThemeRepositoryImpl(dataStore)

    @Provides
    @Singleton
    fun provideTickerStreamRepository(
        webSocketClient: WebSocketClient,
        @Named("wsUrl") wsUrl: String,
    ): TickerStreamRepository = TickerStreamRepositoryImpl(webSocketClient, wsUrl)

    @Provides
    @Singleton
    fun provideFavoritePairsRepository(
        favouriteTickerDao: FavouriteTickerDao,
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher,
    ): FavouriteTickerRepository = FavouriteTickerRepositoryImpl(firestore, favouriteTickerDao, auth, ioDispatcher)
}
