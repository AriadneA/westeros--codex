package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade Room que armazena personagens da ThronesAPI em cache local
 * com chave normalizada [normalizedFamilyKey] para associação às Casas.
 */
@Entity(
  tableName = "characters",
  indices = [Index(value = ["normalizedFamilyKey"])],
)
data class CharacterEntity(
  @PrimaryKey val id: Int,
  val firstName: String,
  val lastName: String,
  val fullName: String,
  val title: String,
  val family: String,
  val imageUrl: String,
  val normalizedFamilyKey: String,
)
