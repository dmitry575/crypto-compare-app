package com.cryptocompare.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cryptocompare.data.local.dao.CatalogRemoteKeyDao
import com.cryptocompare.data.local.dao.FavouriteSymbolDao
import com.cryptocompare.data.local.dao.PendingFavouriteOperationDao
import com.cryptocompare.data.local.dao.PortfolioPositionDao
import com.cryptocompare.data.local.dao.ProviderDao
import com.cryptocompare.data.local.dao.SymbolDao
import com.cryptocompare.data.local.entity.CatalogRemoteKeyEntity
import com.cryptocompare.data.local.entity.FavouriteSymbolEntity
import com.cryptocompare.data.local.entity.PendingFavouriteOperationEntity
import com.cryptocompare.data.local.entity.PortfolioPositionEntity
import com.cryptocompare.data.local.entity.PortfolioQuoteEntity
import com.cryptocompare.data.local.entity.ProviderEntity
import com.cryptocompare.data.local.entity.SymbolEntity

@Database(
    entities = [
        SymbolEntity::class,
        ProviderEntity::class,
        FavouriteSymbolEntity::class,
        CatalogRemoteKeyEntity::class,
        PendingFavouriteOperationEntity::class,
        PortfolioPositionEntity::class,
        PortfolioQuoteEntity::class,
    ],
    version = 12,
    // схемы уезжают в core/data/schemas и коммитятся: без них Room не с чем
    // сверять миграцию, а MigrationTestHelper не может собрать старую базу
    exportSchema = true,
)
abstract class CryptoCompareDatabase : RoomDatabase() {
    abstract fun symbolDao(): SymbolDao

    abstract fun providerDao(): ProviderDao

    abstract fun favouriteSymbolDao(): FavouriteSymbolDao

    abstract fun catalogRemoteKeyDao(): CatalogRemoteKeyDao

    abstract fun pendingFavouriteOperationDao(): PendingFavouriteOperationDao

    abstract fun portfolioPositionDao(): PortfolioPositionDao
}
