package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.data.local.entity.SyncMetadataEntity

/**
 * DAO para persistência dos metadados de sincronização paginada.
 */
@Dao
interface SyncMetadataDao {

  @Query("SELECT * FROM sync_metadata WHERE key = :key LIMIT 1")
  suspend fun getMetadata(key: String): SyncMetadataEntity?

  @Upsert
  suspend fun upsertMetadata(metadata: SyncMetadataEntity)
}
