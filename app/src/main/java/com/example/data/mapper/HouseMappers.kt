package com.example.data.mapper

import com.example.data.local.entity.HouseEntity
import com.example.data.local.entity.HouseWithUserState
import com.example.data.remote.dto.HouseDto
import com.example.domain.model.Character
import com.example.domain.model.House
import com.example.domain.model.HouseDetail

/**
 * Converte [HouseDto] recebido via Retrofit/Moshi em [HouseEntity] persistível no Room,
 * sanitizando arrays vazios (como `[""]`) e extraindo o identificador canônico da URL.
 */
fun HouseDto.toEntity(pageIndex: Int): HouseEntity {
  val trimmedName = name.trim()
  return HouseEntity(
    id = extractResourceId(url) ?: fallbackHouseId(trimmedName),
    name = trimmedName,
    region = region.trim(),
    coatOfArms = coatOfArms.trim(),
    words = words.trim(),
    titles = titles.cleanNonBlankItems(),
    seats = seats.cleanNonBlankItems(),
    ancestralWeapons = ancestralWeapons.cleanNonBlankItems(),
    founded = founded.trim(),
    diedOut = diedOut.trim(),
    overlordId = extractResourceId(overlord),
    pageIndex = pageIndex,
    normalizedFamilyKey = FamilyNameNormalizer.normalizeFromHouseName(trimmedName),
  )
}

fun List<HouseDto>.toEntities(pageIndex: Int): List<HouseEntity> =
  map { it.toEntity(pageIndex = pageIndex) }

fun HouseEntity.toDomain(
  isSworn: Boolean = false,
  notesCount: Int = 0,
): House {
  return House(
    id = id,
    name = name,
    region = region,
    coatOfArms = coatOfArms,
    words = words,
    titles = titles,
    seats = seats,
    ancestralWeapons = ancestralWeapons,
    isSworn = isSworn,
    notesCount = notesCount,
  )
}

fun HouseWithUserState.toDomain(): House {
  return house.toDomain(
    isSworn = userState?.isSworn ?: false,
    notesCount = notes.size,
  )
}

fun HouseWithUserState.toDomainDetail(characters: List<Character>): HouseDetail {
  return HouseDetail(
    house = toDomain(),
    founded = house.founded,
    diedOut = house.diedOut,
    overlordId = house.overlordId,
    characters = characters,
    notes = notes.sortedByDescending { it.updatedAtMillis }.map { it.toDomain() },
  )
}

private fun List<String>.cleanNonBlankItems(): List<String> =
  asSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

private fun extractResourceId(resourceUrl: String): String? {
  val trimmed = resourceUrl.trim().trimEnd('/')
  if (trimmed.isEmpty()) return null
  val segment = trimmed.substringAfterLast('/')
  return segment.takeIf { it.isNotEmpty() && it != trimmed } ?: segment.takeIf { it.all(Char::isDigit) }
}

private fun fallbackHouseId(houseName: String): String {
  return houseName
    .lowercase()
    .replace(Regex("[^a-z0-9]+"), "_")
    .trim('_')
    .ifEmpty { "unknown_house" }
}
