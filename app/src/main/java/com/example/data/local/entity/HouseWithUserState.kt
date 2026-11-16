package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Projeção relacional do Room que combina os metadados da Casa com o estado
 * local de lealdade e as anotações do Meistre.
 */
data class HouseWithUserState(
  @Embedded val house: HouseEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "houseId",
  )
  val userState: HouseUserStateEntity?,
  @Relation(
    parentColumn = "id",
    entityColumn = "houseId",
  )
  val notes: List<MaesterNoteEntity>,
)
