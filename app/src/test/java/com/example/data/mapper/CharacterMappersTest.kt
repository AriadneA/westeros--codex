package com.example.data.mapper

import com.example.data.remote.dto.ThronesCharacterDto
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterMappersTest {

  @Test
  fun `toEntity normalizes family names and trims character fields`() {
    val dto =
      ThronesCharacterDto(
        id = 2,
        firstName = " Jon ",
        lastName = " Snow ",
        fullName = "",
        title = " King of the North ",
        family = " House Stark ",
        imageUrl = " https://thronesapi.com/assets/images/jon-snow.jpg ",
      )

    val entity = dto.toEntity()

    assertEquals(2, entity.id)
    assertEquals("Jon Snow", entity.fullName)
    assertEquals("King of the North", entity.title)
    assertEquals("House Stark", entity.family)
    assertEquals("stark", entity.normalizedFamilyKey)
    assertEquals("https://thronesapi.com/assets/images/jon-snow.jpg", entity.imageUrl)

    val domain = entity.toDomain()
    assertEquals(entity.id, domain.id)
    assertEquals("Jon Snow", domain.fullName)
    assertEquals("House Stark", domain.family)
  }

  @Test
  fun `toEntity clears unknown family keys so unrelated characters do not falsely match`() {
    val unknownDto = ThronesCharacterDto(id = 99, fullName = "Varys", family = "Unknown")
    val emptyDto = ThronesCharacterDto(id = 100, fullName = "Bronn", family = "")

    assertEquals("", unknownDto.toEntity().normalizedFamilyKey)
    assertEquals("", emptyDto.toEntity().normalizedFamilyKey)
  }
}
