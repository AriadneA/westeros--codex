package com.example.di

import com.example.data.local.dao.CharacterDao
import com.example.data.local.dao.HouseDao
import com.example.data.local.dao.MaesterNoteDao
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.remote.api.IceAndFireApi
import com.example.data.remote.api.ThronesApi
import com.example.data.repository.HouseRepositoryImpl
import com.example.domain.repository.HouseRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Módulo Hilt que vincula o contrato [HouseRepository] da camada Domain
 * à implementação Offline-First [HouseRepositoryImpl] da camada Data.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

  @Provides
  @Singleton
  fun provideHouseRepository(
    iceAndFireApi: IceAndFireApi,
    thronesApi: ThronesApi,
    houseDao: HouseDao,
    characterDao: CharacterDao,
    maesterNoteDao: MaesterNoteDao,
    syncMetadataDao: SyncMetadataDao,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
  ): HouseRepository {
    return HouseRepositoryImpl(
      iceAndFireApi = iceAndFireApi,
      thronesApi = thronesApi,
      houseDao = houseDao,
      characterDao = characterDao,
      maesterNoteDao = maesterNoteDao,
      syncMetadataDao = syncMetadataDao,
      ioDispatcher = ioDispatcher,
    )
  }
}
