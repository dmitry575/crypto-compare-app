package com.cryptocompare.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cryptocompare.data.local.dao.CatalogRemoteKeyDao
import com.cryptocompare.data.local.dao.FavouriteTickerDao
import com.cryptocompare.data.local.dao.PendingFavouriteOperationDao
import com.cryptocompare.data.local.dao.ProviderDao
import com.cryptocompare.data.local.dao.SymbolDao
import com.cryptocompare.data.local.entity.CatalogRemoteKeyEntity
import com.cryptocompare.data.local.entity.FavouriteTickerEntity
import com.cryptocompare.data.local.entity.PendingFavoriteOperationEntity
import com.cryptocompare.data.local.entity.ProviderEntity
import com.cryptocompare.data.local.entity.SymbolEntity

@Database(
    entities = [
        SymbolEntity::class,
        ProviderEntity::class,
        FavouriteTickerEntity::class,
        CatalogRemoteKeyEntity::class,
        PendingFavoriteOperationEntity::class,
    ],
    version = 7,
    // схемы уезжают в core/data/schemas и коммитятся: без них Room не с чем
    // сверять миграцию, а MigrationTestHelper не может собрать старую базу
    exportSchema = true,
)
abstract class CryptoCompareDatabase : RoomDatabase() {
    abstract fun symbolDao(): SymbolDao

    abstract fun providerDao(): ProviderDao

    abstract fun favouriteTickerDao(): FavouriteTickerDao

    abstract fun catalogRemoteKeyDao(): CatalogRemoteKeyDao

    abstract fun pendingFavoriteOperationDao(): PendingFavouriteOperationDao
}
