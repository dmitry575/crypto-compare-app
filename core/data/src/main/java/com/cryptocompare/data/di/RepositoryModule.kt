package com.cryptocompare.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.cryptocompare.data.BuildConfig
import com.cryptocompare.data.local.CryptoCompareDatabase
import com.cryptocompare.data.local.dao.FavouriteTickerDao
import com.cryptocompare.data.local.dao.PendingFavouriteOperationDao
import com.cryptocompare.data.local.dao.ProviderDao
import com.cryptocompare.data.local.dao.SymbolDao
import com.cryptocompare.data.repository.AuthRepositoryImpl
import com.cryptocompare.data.repository.CryptoCompareRepositoryImpl
import com.cryptocompare.data.repository.FavouriteTickerRepositoryImpl
import com.cryptocompare.data.repository.FirebaseCrashReporter
import com.cryptocompare.data.repository.LanguageRepositoryImpl
import com.cryptocompare.data.repository.OnboardingRepositoryImpl
import com.cryptocompare.data.repository.ThemeRepositoryImpl
import com.cryptocompare.data.repository.TickerStreamRepositoryImpl
import com.cryptocompare.data.transactionrunner.DatabaseTransactionRunner
import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.domain.repository.CrashReporter
import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.domain.repository.FavouriteTickerRepository
import com.cryptocompare.domain.repository.LanguageRepository
import com.cryptocompare.domain.repository.OnboardingRepository
import com.cryptocompare.domain.repository.ThemeRepository
import com.cryptocompare.domain.repository.TickerStreamRepository
import com.cryptocompare.network.api.CryptoCompareApi
import com.cryptocompare.network.api.CryptoCompareHistoryApi
import com.cryptocompare.network.websocket.WebSocketClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
    fun provideAuthRepository(
        auth: FirebaseAuth,
        crashReporter: CrashReporter,
    ): AuthRepository = AuthRepositoryImpl(auth, crashReporter)

    @Provides
    @Singleton
    fun provideOnboardingRepository(dataStore: DataStore<Preferences>): OnboardingRepository =
        OnboardingRepositoryImpl(dataStore)

    @Provides
    @Singleton
    fun provideFirebaseCrashlytics(): FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    @Provides
    @Singleton
    fun provideCrashReporter(crashlytics: FirebaseCrashlytics): CrashReporter = FirebaseCrashReporter(crashlytics)

    @Provides
    @Singleton
    fun provideThemeRepository(dataStore: DataStore<Preferences>): ThemeRepository = ThemeRepositoryImpl(dataStore)

    @Provides
    @Singleton
    fun provideLanguageRepository(dataStore: DataStore<Preferences>): LanguageRepository =
        LanguageRepositoryImpl(dataStore)

    @Provides
    @Singleton
    fun provideTickerStreamRepository(
        webSocketClient: WebSocketClient,
        @Named("wsUrl") wsUrl: String,
        crashReporter: CrashReporter,
    ): TickerStreamRepository = TickerStreamRepositoryImpl(webSocketClient, wsUrl, crashReporter)

    @Provides
    @Singleton
    fun provideFavoritePairsRepository(
        favouriteTickerDao: FavouriteTickerDao,
        pendingFavouriteOperationDao: PendingFavouriteOperationDao,
        transactionRunner: DatabaseTransactionRunner,
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher,
    ): FavouriteTickerRepository =
        FavouriteTickerRepositoryImpl(
            firestore = firestore,
            favouriteTickerDao = favouriteTickerDao,
            pendingFavouriteOperationDao = pendingFavouriteOperationDao,
            transactionRunner = transactionRunner,
            auth = auth,
            ioDispatcher = ioDispatcher,
        )
}
