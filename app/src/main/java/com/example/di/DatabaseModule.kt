package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.dao.CharacterDao
import com.example.data.local.dao.HouseDao
import com.example.data.local.dao.MaesterNoteDao
import com.example.data.local.dao.SyncMetadataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt responsável pelo fornecimento do [AppDatabase] (Room SSOT)
 * e de todos os DAOs utilizados pela camada de dados.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  @Provides
  @Singleton
  fun provideAppDatabase(
    @ApplicationContext context: Context,
  ): AppDatabase {
    return Room.databaseBuilder(
      context,
      AppDatabase::class.java,
      AppDatabase.DATABASE_NAME,
    ).build()
  }

  @Provides
  fun provideHouseDao(database: AppDatabase): HouseDao = database.houseDao()

  @Provides
  fun provideCharacterDao(database: AppDatabase): CharacterDao = database.characterDao()

  @Provides
  fun provideMaesterNoteDao(database: AppDatabase): MaesterNoteDao = database.maesterNoteDao()

  @Provides
  fun provideSyncMetadataDao(database: AppDatabase): SyncMetadataDao = database.syncMetadataDao()
}
