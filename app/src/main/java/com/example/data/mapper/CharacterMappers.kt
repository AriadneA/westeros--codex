package com.example.data.mapper

import com.example.data.local.entity.CharacterEntity
import com.example.data.remote.dto.ThronesCharacterDto
import com.example.domain.model.Character

fun ThronesCharacterDto.toEntity(): CharacterEntity {
  val trimmedFamily = family.trim()
  return CharacterEntity(
    id = id,
    firstName = firstName.trim(),
    lastName = lastName.trim(),
    fullName = fullName.trim().ifEmpty { "${firstName.trim()} ${lastName.trim()}".trim() },
    title = title.trim(),
    family = trimmedFamily,
    imageUrl = imageUrl.trim(),
    normalizedFamilyKey = FamilyNameNormalizer.normalizeFromCharacterFamily(trimmedFamily),
  )
}

fun CharacterEntity.toDomain(): Character {
  return Character(
    id = id,
    firstName = firstName,
    lastName = lastName,
    fullName = fullName,
    title = title,
    family = family,
    imageUrl = imageUrl,
  )
}

fun List<ThronesCharacterDto>.toEntities(): List<CharacterEntity> = map { it.toEntity() }

fun List<CharacterEntity>.toDomainCharacters(): List<Character> = map { it.toDomain() }
