package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade Room que representa uma Casa armazenada localmente (SSOT).
 * Separada dos dados autorais do usuário ([HouseUserStateEntity] e [MaesterNoteEntity])
 * para que atualizações da API via `@Upsert` jamais sobrescrevam lealdades ou notas.
 */
@Entity(
  tableName = "houses",
  indices = [
    Index(value = ["region"]),
    Index(value = ["normalizedFamilyKey"]),
    Index(value = ["pageIndex"]),
  ],
)
data class HouseEntity(
  @PrimaryKey val id: String,
  val name: String,
  val region: String,
  val coatOfArms: String,
  val words: String,
  val titles: List<String>,
  val seats: List<String>,
  val ancestralWeapons: List<String>,
  val founded: String,
  val diedOut: String,
  val overlordId: String?,
  val pageIndex: Int,
  val normalizedFamilyKey: String,
)
