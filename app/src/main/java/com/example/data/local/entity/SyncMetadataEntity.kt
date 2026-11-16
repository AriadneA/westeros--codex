package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persiste metadados de controle da paginação Offline-First
 * (última página sincronizada, disponibilidade de próximas páginas e timestamp).
 */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
  @PrimaryKey val key: String,
  val lastSyncedPage: Int,
  val hasMorePages: Boolean,
  val lastSyncedAtMillis: Long,
)
