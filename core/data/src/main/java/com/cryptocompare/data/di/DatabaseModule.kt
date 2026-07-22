package com.cryptocompare.data.di

import android.content.Context
import androidx.room.Room
import com.cryptocompare.data.local.CryptoCompareDatabase
import com.cryptocompare.data.local.dao.FavouriteTickerDao
import com.cryptocompare.data.local.dao.ProviderDao
import com.cryptocompare.data.local.dao.SymbolDao
import com.cryptocompare.data.local.migrations.AssetMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideCryptoCompareDatabase(
        @ApplicationContext context: Context,
    ): CryptoCompareDatabase =
        Room
            .databaseBuilder(
                context,
                CryptoCompareDatabase::class.java,
                "crypto_compare.db",
            ).addMigrations(*AssetMigrations.loadAll(context))
            // fallbackToDestructiveMigration здесь намеренно нет: при недостающей
            // миграции Room должен упасть с IllegalStateException, а не молча стереть
            // базу вместе с офлайн-избранным. Порядок действий — core/data/MIGRATIONS.md
            .build()

    @Provides
    fun provideProviderDao(database: CryptoCompareDatabase): ProviderDao = database.providerDao()

    @Provides
    fun provideSymbolDao(database: CryptoCompareDatabase): SymbolDao = database.symbolDao()

    @Provides
    fun provideFavouriteTickerDao(database: CryptoCompareDatabase): FavouriteTickerDao = database.favouriteTickerDao()
}
