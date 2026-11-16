package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Armazena as Anotações do Meistre escritas offline pelo usuário para cada Casa.
 */
@Entity(
  tableName = "maester_notes",
  indices = [Index(value = ["houseId"])],
)
data class MaesterNoteEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val houseId: String,
  val content: String,
  val updatedAtMillis: Long,
)
