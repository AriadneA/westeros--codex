package com.example.data.repository

import com.example.data.local.dao.CharacterDao
import com.example.data.local.dao.HouseDao
import com.example.data.local.dao.MaesterNoteDao
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.HouseEntity
import com.example.data.local.entity.HouseUserStateEntity
import com.example.data.local.entity.HouseWithUserState
import com.example.data.local.entity.MaesterNoteEntity
import com.example.data.local.entity.SyncMetadataEntity
import com.example.data.remote.api.IceAndFireApi
import com.example.data.remote.api.ThronesApi
import com.example.data.remote.dto.HouseDto
import com.example.data.remote.dto.ThronesCharacterDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HouseRepositoryImplTest {

  private lateinit var iceAndFireApi: IceAndFireApi
  private lateinit var thronesApi: ThronesApi
  private lateinit var houseDao: HouseDao
  private lateinit var characterDao: CharacterDao
  private lateinit var maesterNoteDao: MaesterNoteDao
  private lateinit var syncMetadataDao: SyncMetadataDao

  private val testDispatcher = StandardTestDispatcher()
  private var fixedTimeMillis = 1_700_000_000_000L

  private lateinit var repository: HouseRepositoryImpl

  @Before
  fun setUp() {
    iceAndFireApi = mockk()
    thronesApi = mockk()
    houseDao = mockk()
    characterDao = mockk()
    maesterNoteDao = mockk()
    syncMetadataDao = mockk()

    repository =
      HouseRepositoryImpl(
        iceAndFireApi = iceAndFireApi,
        thronesApi = thronesApi,
        houseDao = houseDao,
        characterDao = characterDao,
        maesterNoteDao = maesterNoteDao,
        syncMetadataDao = syncMetadataDao,
        ioDispatcher = testDispatcher,
        clock = { fixedTimeMillis },
      )
  }

  @Test
  fun `observeHouses maps SSOT rows from DAO and trims query and region`() =
    runTest(testDispatcher) {
      val row =
        HouseWithUserState(
          house = sampleHouseEntity(id = "362", name = "House Stark of Winterfell", region = "The North"),
          userState = HouseUserStateEntity(houseId = "362", isSworn = true, swornAtMillis = 123L),
          notes = emptyList(),
        )
      every {
        houseDao.observeHouses(query = "Stark", region = "The North", onlySworn = true)
      } returns flowOf(listOf(row))

      val result =
        repository
          .observeHouses(query = "  Stark  ", region = "  The North ", onlySworn = true)
          .first()

      assertEquals(1, result.size)
      assertEquals("362", result.first().id)
      assertTrue(result.first().isSworn)
    }

  @Test
  fun `observeHouseDetail combines house relation with matching characters`() =
    runTest(testDispatcher) {
      val houseRow =
        HouseWithUserState(
          house = sampleHouseEntity(id = "362", name = "House Stark of Winterfell", familyKey = "stark"),
          userState = null,
          notes = listOf(MaesterNoteEntity(id = 7L, houseId = "362", content = "O Norte se lembra", updatedAtMillis = 500L)),
        )
      val character =
        CharacterEntity(
          id = 2,
          firstName = "Jon",
          lastName = "Snow",
          fullName = "Jon Snow",
          title = "King of the North",
          family = "House Stark",
          imageUrl = "https://thronesapi.com/assets/images/jon-snow.jpg",
          normalizedFamilyKey = "stark",
        )
      every { houseDao.observeHouseById("362") } returns flowOf(houseRow)
      every { characterDao.observeCharactersByFamilyKey("stark") } returns flowOf(listOf(character))

      val detail = repository.observeHouseDetail("362").first()

      assertEquals("362", detail?.house?.id)
      assertEquals(1, detail?.characters?.size)
      assertEquals("Jon Snow", detail?.characters?.first()?.fullName)
      assertEquals(1, detail?.notes?.size)
    }

  @Test
  fun `observeHouseDetail emits null when house does not exist in Room`() =
    runTest(testDispatcher) {
      every { houseDao.observeHouseById("999") } returns flowOf(null)

      val detail = repository.observeHouseDetail("999").first()

      assertNull(detail)
    }

  @Test
  fun `syncHousesPage persists entities and updates sync metadata when API succeeds`() =
    runTest(testDispatcher) {
      val dtos =
        listOf(
          HouseDto(
            url = "https://anapioficeandfire.com/api/houses/362",
            name = "House Stark of Winterfell",
            region = "The North",
            words = "Winter is Coming",
          ),
        )
      val housesSlot = slot<List<HouseEntity>>()
      val metaSlot = slot<SyncMetadataEntity>()

      coEvery { iceAndFireApi.getHouses(page = 1, pageSize = 25) } returns dtos
      coEvery { houseDao.upsertHouses(capture(housesSlot)) } just runs
      coEvery { syncMetadataDao.getMetadata(HouseRepositoryImpl.HOUSES_SYNC_KEY) } returns null
      coEvery { syncMetadataDao.upsertMetadata(capture(metaSlot)) } just runs

      val result = repository.syncHousesPage(page = 1, pageSize = 25)

      assertTrue(result.isSuccess)
      assertFalse(result.getOrThrow())
      assertEquals("362", housesSlot.captured.single().id)
      assertEquals(1, metaSlot.captured.lastSyncedPage)
      assertFalse(metaSlot.captured.hasMorePages)
      assertEquals(fixedTimeMillis, metaSlot.captured.lastSyncedAtMillis)
    }

  @Test
  fun `syncHousesPage returns failure gracefully on IOException without crashing`() =
    runTest(testDispatcher) {
      coEvery { iceAndFireApi.getHouses(page = 1, pageSize = 25) } throws IOException("Sem rede")

      val result = repository.syncHousesPage(page = 1, pageSize = 25)

      assertTrue(result.isFailure)
      assertTrue(result.exceptionOrNull() is IOException)
      coVerify(exactly = 0) { houseDao.upsertHouses(any()) }
    }

  @Test
  fun `syncInitialDataIfEmpty skips network when local cache is already populated`() =
    runTest(testDispatcher) {
      coEvery { houseDao.countHouses() } returns 25
      coEvery { characterDao.countCharacters() } returns 53

      val result = repository.syncInitialDataIfEmpty()

      assertTrue(result.isSuccess)
      coVerify(exactly = 0) { iceAndFireApi.getHouses(any(), any()) }
      coVerify(exactly = 0) { thronesApi.getCharacters() }
    }

  @Test
  fun `syncInitialDataIfEmpty succeeds even when complementary ThronesApi fails`() =
    runTest(testDispatcher) {
      val dtos =
        listOf(
          HouseDto(
            url = "https://anapioficeandfire.com/api/houses/229",
            name = "House Lannister of Casterly Rock",
            region = "The Westerlands",
          ),
        )
      coEvery { houseDao.countHouses() } returns 0
      coEvery { characterDao.countCharacters() } returns 0
      coEvery { iceAndFireApi.getHouses(page = 1, pageSize = 25) } returns dtos
      coEvery { houseDao.upsertHouses(any()) } just runs
      coEvery { syncMetadataDao.getMetadata(any()) } returns null
      coEvery { syncMetadataDao.upsertMetadata(any()) } just runs
      coEvery { thronesApi.getCharacters() } throws IOException("ThronesAPI offline")

      val result = repository.syncInitialDataIfEmpty()

      assertTrue(result.isSuccess)
      coVerify(exactly = 1) { houseDao.upsertHouses(any()) }
    }

  @Test
  fun `syncCharacters persists valid characters from ThronesApi`() =
    runTest(testDispatcher) {
      val dto =
        ThronesCharacterDto(
          id = 0,
          firstName = "Daenerys",
          lastName = "Targaryen",
          fullName = "Daenerys Targaryen",
          family = "House Targaryen",
          imageUrl = "https://thronesapi.com/assets/images/daenerys.jpg",
        )
      val slot = slot<List<CharacterEntity>>()
      coEvery { thronesApi.getCharacters() } returns listOf(dto)
      coEvery { characterDao.upsertCharacters(capture(slot)) } just runs

      val result = repository.syncCharacters()

      assertTrue(result.isSuccess)
      assertEquals("targaryen", slot.captured.single().normalizedFamilyKey)
    }

  @Test
  fun `toggleAllegiance switches from unsworn to sworn and back`() =
    runTest(testDispatcher) {
      val capturedStates = mutableListOf<HouseUserStateEntity>()
      coEvery { houseDao.getUserState("362") } returnsMany listOf(
        null,
        HouseUserStateEntity(houseId = "362", isSworn = true, swornAtMillis = fixedTimeMillis),
      )
      coEvery { houseDao.upsertUserState(capture(capturedStates)) } just runs

      val firstToggle = repository.toggleAllegiance("362")
      val secondToggle = repository.toggleAllegiance("362")

      assertTrue(firstToggle.isSuccess)
      assertTrue(secondToggle.isSuccess)
      assertTrue(capturedStates[0].isSworn)
      assertEquals(fixedTimeMillis, capturedStates[0].swornAtMillis)
      assertFalse(capturedStates[1].isSworn)
      assertNull(capturedStates[1].swornAtMillis)
    }

  @Test
  fun `saveMaesterNote rejects blank content and persists trimmed valid content`() =
    runTest(testDispatcher) {
      val blankResult = repository.saveMaesterNote(houseId = "362", noteId = null, content = "   ")
      assertTrue(blankResult.isFailure)
      coVerify(exactly = 0) { maesterNoteDao.upsertNote(any()) }

      val noteSlot = slot<MaesterNoteEntity>()
      coEvery { maesterNoteDao.upsertNote(capture(noteSlot)) } just runs

      val validResult =
        repository.saveMaesterNote(houseId = "362", noteId = null, content = "  Anotação importante  ")

      assertTrue(validResult.isSuccess)
      assertEquals("Anotação importante", noteSlot.captured.content)
      assertEquals("362", noteSlot.captured.houseId)
      assertEquals(fixedTimeMillis, noteSlot.captured.updatedAtMillis)
    }

  @Test
  fun `deleteMaesterNote delegates to MaesterNoteDao`() =
    runTest(testDispatcher) {
      coEvery { maesterNoteDao.deleteNoteById(42L) } just runs

      val result = repository.deleteMaesterNote(42L)

      assertTrue(result.isSuccess)
      coVerify(exactly = 1) { maesterNoteDao.deleteNoteById(42L) }
    }

  private fun sampleHouseEntity(
    id: String,
    name: String,
    region: String = "The North",
    familyKey: String = "stark",
  ): HouseEntity {
    return HouseEntity(
      id = id,
      name = name,
      region = region,
      coatOfArms = "Direwolf",
      words = "Winter is Coming",
      titles = listOf("Lord of Winterfell"),
      seats = listOf("Winterfell"),
      ancestralWeapons = listOf("Ice"),
      founded = "Age of Heroes",
      diedOut = "",
      overlordId = null,
      pageIndex = 1,
      normalizedFamilyKey = familyKey,
    )
  }
}
