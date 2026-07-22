package com.cryptocompare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cryptocompare.data.local.entity.CatalogRemoteKeyEntity

@Dao
interface CatalogRemoteKeyDao {
    @Query("SELECT * FROM catalog_remote_key WHERE id = 0")
    suspend fun get(): CatalogRemoteKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(key: CatalogRemoteKeyEntity)

    @Query("DELETE FROM catalog_remote_key")
    suspend fun clear()
}
