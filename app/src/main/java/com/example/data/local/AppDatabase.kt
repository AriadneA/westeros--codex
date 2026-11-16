package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.converter.WesterosTypeConverters
import com.example.data.local.dao.CharacterDao
import com.example.data.local.dao.HouseDao
import com.example.data.local.dao.MaesterNoteDao
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.HouseEntity
import com.example.data.local.entity.HouseUserStateEntity
import com.example.data.local.entity.MaesterNoteEntity
import com.example.data.local.entity.SyncMetadataEntity

/**
 * Banco de dados local Room atuando como Single Source of Truth (SSOT) do Westeros Codex.
 */
@Database(
  entities = [
    HouseEntity::class,
    HouseUserStateEntity::class,
    MaesterNoteEntity::class,
    CharacterEntity::class,
    SyncMetadataEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
@TypeConverters(WesterosTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

  abstract fun houseDao(): HouseDao

  abstract fun characterDao(): CharacterDao

  abstract fun maesterNoteDao(): MaesterNoteDao

  abstract fun syncMetadataDao(): SyncMetadataDao

  companion object {
    const val DATABASE_NAME = "westeros_codex.db"
  }
}
