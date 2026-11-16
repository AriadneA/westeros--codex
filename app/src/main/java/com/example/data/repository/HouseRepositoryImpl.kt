package com.example.data.repository

import com.example.data.local.dao.CharacterDao
import com.example.data.local.dao.HouseDao
import com.example.data.local.dao.MaesterNoteDao
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.local.entity.HouseUserStateEntity
import com.example.data.local.entity.MaesterNoteEntity
import com.example.data.local.entity.SyncMetadataEntity
import com.example.data.mapper.toDomain
import com.example.data.mapper.toDomainCharacters
import com.example.data.mapper.toDomainDetail
import com.example.data.mapper.toEntities
import com.example.data.remote.api.IceAndFireApi
import com.example.data.remote.api.ThronesApi
import com.example.domain.model.House
import com.example.domain.model.HouseDetail
import com.example.domain.repository.HouseRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

/**
 * Implementação Offline-First (SSOT) do [HouseRepository].
 * Toda leitura da UI observa continuamente os [Flow]s do Room; sincronizações
 * com [IceAndFireApi] e [ThronesApi] ocorrem em background sem quebrar a experiência
 * em caso de falha de rede.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HouseRepositoryImpl(
  private val iceAndFireApi: IceAndFireApi,
  private val thronesApi: ThronesApi,
  private val houseDao: HouseDao,
  private val characterDao: CharacterDao,
  private val maesterNoteDao: MaesterNoteDao,
  private val syncMetadataDao: SyncMetadataDao,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
  private val clock: () -> Long = { System.currentTimeMillis() },
) : HouseRepository {

  override fun observeHouses(
    query: String,
    region: String?,
    onlySworn: Boolean,
  ): Flow<List<House>> {
    val sanitizedQuery = query.trim()
    val sanitizedRegion = region?.trim()?.takeIf { it.isNotEmpty() }
    return houseDao
      .observeHouses(
        query = sanitizedQuery,
        region = sanitizedRegion,
        onlySworn = onlySworn,
      )
      .map { rows -> rows.map { it.toDomain() } }
      .flowOn(ioDispatcher)
  }

  override fun observeHouseDetail(houseId: String): Flow<HouseDetail?> {
    return houseDao
      .observeHouseById(houseId)
      .flatMapLatest { houseWithState ->
        if (houseWithState == null) {
          flowOf(null)
        } else {
          val familyKey = houseWithState.house.normalizedFamilyKey
          if (familyKey.isBlank()) {
            flowOf(houseWithState.toDomainDetail(characters = emptyList()))
          } else {
            characterDao.observeCharactersByFamilyKey(familyKey).map { characterEntities ->
              houseWithState.toDomainDetail(characters = characterEntities.toDomainCharacters())
            }
          }
        }
      }
      .flowOn(ioDispatcher)
  }

  override fun observeAvailableRegions(): Flow<List<String>> {
    return houseDao.observeAvailableRegions().flowOn(ioDispatcher)
  }

  override suspend fun syncHousesPage(page: Int, pageSize: Int): Result<Boolean> {
    return withContext(ioDispatcher) {
      if (page < 1 || pageSize <= 0) {
        return@withContext Result.failure(
          IllegalArgumentException("Parâmetros de paginação inválidos: page=$page, pageSize=$pageSize"),
        )
      }
      try {
        val dtos = iceAndFireApi.getHouses(page = page, pageSize = pageSize)
        val entities = dtos.filter { it.name.isNotBlank() }.toEntities(pageIndex = page)
        if (entities.isNotEmpty()) {
          houseDao.upsertHouses(entities)
        }
        val existingMeta = syncMetadataDao.getMetadata(HOUSES_SYNC_KEY)
        val hasMorePages = dtos.size >= pageSize
        val updatedLastPage = maxOf(existingMeta?.lastSyncedPage ?: 0, page)
        syncMetadataDao.upsertMetadata(
          SyncMetadataEntity(
            key = HOUSES_SYNC_KEY,
            lastSyncedPage = updatedLastPage,
            hasMorePages = hasMorePages,
            lastSyncedAtMillis = clock(),
          ),
        )
        Result.success(hasMorePages)
      } catch (cancel: CancellationException) {
        throw cancel
      } catch (error: Throwable) {
        Result.failure(error)
      }
    }
  }

  override suspend fun syncInitialDataIfEmpty(): Result<Unit> {
    return withContext(ioDispatcher) {
      try {
        val hasHouses = houseDao.countHouses() > 0
        val hasCharacters = characterDao.countCharacters() > 0
        if (hasHouses && hasCharacters) {
          return@withContext Result.success(Unit)
        }

        supervisorScope {
          val charactersJob =
            if (!hasCharacters) {
              async { syncCharacters() }
            } else {
              null
            }

          val housesResult =
            if (!hasHouses) {
              syncHousesPage(page = INITIAL_PAGE, pageSize = DEFAULT_PAGE_SIZE).map { Unit }
            } else {
              Result.success(Unit)
            }

          // Falhas na API complementar de personagens não devem invalidar o catálogo principal.
          charactersJob?.await()
          housesResult
        }
      } catch (cancel: CancellationException) {
        throw cancel
      } catch (error: Throwable) {
        Result.failure(error)
      }
    }
  }

  override suspend fun syncCharacters(): Result<Unit> {
    return withContext(ioDispatcher) {
      try {
        val dtos = thronesApi.getCharacters()
        val entities = dtos.filter { it.fullName.isNotBlank() || it.firstName.isNotBlank() }.toEntities()
        if (entities.isNotEmpty()) {
          characterDao.upsertCharacters(entities)
        }
        Result.success(Unit)
      } catch (cancel: CancellationException) {
        throw cancel
      } catch (error: Throwable) {
        Result.failure(error)
      }
    }
  }

  override suspend fun toggleAllegiance(houseId: String): Result<Unit> {
    return withContext(ioDispatcher) {
      try {
        val currentState = houseDao.getUserState(houseId)
        val nextSworn = !(currentState?.isSworn ?: false)
        houseDao.upsertUserState(
          HouseUserStateEntity(
            houseId = houseId,
            isSworn = nextSworn,
            swornAtMillis = if (nextSworn) clock() else null,
          ),
        )
        Result.success(Unit)
      } catch (cancel: CancellationException) {
        throw cancel
      } catch (error: Throwable) {
        Result.failure(error)
      }
    }
  }

  override suspend fun saveMaesterNote(
    houseId: String,
    noteId: Long?,
    content: String,
  ): Result<Unit> {
    return withContext(ioDispatcher) {
      val trimmedContent = content.trim()
      if (trimmedContent.isEmpty()) {
        return@withContext Result.failure(
          IllegalArgumentException("A anotação do Meistre não pode estar vazia."),
        )
      }
      try {
        maesterNoteDao.upsertNote(
          MaesterNoteEntity(
            id = noteId ?: 0L,
            houseId = houseId,
            content = trimmedContent,
            updatedAtMillis = clock(),
          ),
        )
        Result.success(Unit)
      } catch (cancel: CancellationException) {
        throw cancel
      } catch (error: Throwable) {
        Result.failure(error)
      }
    }
  }

  override suspend fun deleteMaesterNote(noteId: Long): Result<Unit> {
    return withContext(ioDispatcher) {
      try {
        maesterNoteDao.deleteNoteById(noteId)
        Result.success(Unit)
      } catch (cancel: CancellationException) {
        throw cancel
      } catch (error: Throwable) {
        Result.failure(error)
      }
    }
  }

  companion object {
    const val HOUSES_SYNC_KEY = "houses_pagination"
    const val INITIAL_PAGE = 1
    const val DEFAULT_PAGE_SIZE = 25
  }
}
