package com.example.data.mapper

import com.example.data.local.entity.HouseUserStateEntity
import com.example.data.local.entity.HouseWithUserState
import com.example.data.local.entity.MaesterNoteEntity
import com.example.data.remote.dto.HouseDto
import com.example.domain.model.Character
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HouseMappersTest {

  @Test
  fun `toEntity extracts id from url, sanitizes blank lists and normalizes family key`() {
    val dto =
      HouseDto(
        url = "https://anapioficeandfire.com/api/houses/362/",
        name = " House Stark of Winterfell ",
        region = " The North ",
        coatOfArms = " A grey direwolf racing across an ice-white field ",
        words = " Winter is Coming ",
        titles = listOf("King in the North", "", "  "),
        seats = listOf("Winterfell", ""),
        overlord = "https://anapioficeandfire.com/api/houses/16",
        founded = " Age of Heroes ",
        diedOut = "",
        ancestralWeapons = listOf("Ice", ""),
      )

    val entity = dto.toEntity(pageIndex = 1)

    assertEquals("362", entity.id)
    assertEquals("House Stark of Winterfell", entity.name)
    assertEquals("The North", entity.region)
    assertEquals("A grey direwolf racing across an ice-white field", entity.coatOfArms)
    assertEquals("Winter is Coming", entity.words)
    assertEquals(listOf("King in the North"), entity.titles)
    assertEquals(listOf("Winterfell"), entity.seats)
    assertEquals(listOf("Ice"), entity.ancestralWeapons)
    assertEquals("Age of Heroes", entity.founded)
    assertEquals("", entity.diedOut)
    assertEquals("16", entity.overlordId)
    assertEquals(1, entity.pageIndex)
    assertEquals("stark", entity.normalizedFamilyKey)
  }

  @Test
  fun `toEntity handles empty overlord and falls back gracefully when url is blank`() {
    val dto =
      HouseDto(
        url = "",
        name = "House Nymeros Martell of Sunspear",
        region = "Dorne",
        titles = listOf(""),
        seats = listOf(""),
        ancestralWeapons = listOf(""),
        overlord = "",
      )

    val entity = dto.toEntity(pageIndex = 2)

    assertEquals("house_nymeros_martell_of_sunspear", entity.id)
    assertTrue(entity.titles.isEmpty())
    assertTrue(entity.seats.isEmpty())
    assertTrue(entity.ancestralWeapons.isEmpty())
    assertNull(entity.overlordId)
    assertEquals("martell", entity.normalizedFamilyKey)
  }

  @Test
  fun `HouseWithUserState toDomain preserves sworn status and note count`() {
    val entity =
      HouseDto(
        url = "https://anapioficeandfire.com/api/houses/229",
        name = "House Lannister of Casterly Rock",
        region = "The Westerlands",
        coatOfArms = "A roaring lion, gold on crimson",
        words = "Hear Me Roar!",
      ).toEntity(pageIndex = 1)

    val relation =
      HouseWithUserState(
        house = entity,
        userState = HouseUserStateEntity(houseId = "229", isSworn = true, swornAtMillis = 1_000L),
        notes =
          listOf(
            MaesterNoteEntity(id = 1L, houseId = "229", content = "Nota 1", updatedAtMillis = 100L),
            MaesterNoteEntity(id = 2L, houseId = "229", content = "Nota 2", updatedAtMillis = 200L),
          ),
      )

    val domainHouse = relation.toDomain()

    assertEquals("229", domainHouse.id)
    assertTrue(domainHouse.isSworn)
    assertEquals(2, domainHouse.notesCount)
    assertEquals("Hear Me Roar!", domainHouse.words)
  }

  @Test
  fun `HouseWithUserState toDomainDetail sorts notes descending and attaches characters`() {
    val entity =
      HouseDto(
        url = "https://anapioficeandfire.com/api/houses/378",
        name = "House Targaryen of King's Landing",
        region = "The Crownlands",
        words = "Fire and Blood",
        founded = "Aegon's Conquest",
      ).toEntity(pageIndex = 1)

    val relation =
      HouseWithUserState(
        house = entity,
        userState = null,
        notes =
          listOf(
            MaesterNoteEntity(id = 1L, houseId = "378", content = "Antiga", updatedAtMillis = 100L),
            MaesterNoteEntity(id = 2L, houseId = "378", content = "Recente", updatedAtMillis = 500L),
          ),
      )

    val characters =
      listOf(
        Character(
          id = 0,
          firstName = "Daenerys",
          lastName = "Targaryen",
          fullName = "Daenerys Targaryen",
          title = "Mother of Dragons",
          family = "House Targaryen",
          imageUrl = "https://thronesapi.com/assets/images/daenerys.jpg",
        ),
      )

    val detail = relation.toDomainDetail(characters = characters)

    assertFalse(detail.house.isSworn)
    assertEquals("Aegon's Conquest", detail.founded)
    assertEquals(1, detail.characters.size)
    assertEquals("Recente", detail.notes.first().content)
    assertEquals("Antiga", detail.notes.last().content)
  }
}
