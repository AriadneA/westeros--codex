package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.data.local.entity.MaesterNoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO dedicado ao CRUD offline das Anotações do Meistre.
 */
@Dao
interface MaesterNoteDao {

  @Query("SELECT * FROM maester_notes WHERE houseId = :houseId ORDER BY updatedAtMillis DESC")
  fun observeNotesForHouse(houseId: String): Flow<List<MaesterNoteEntity>>

  @Query("SELECT * FROM maester_notes WHERE id = :noteId LIMIT 1")
  suspend fun getNoteById(noteId: Long): MaesterNoteEntity?

  @Upsert
  suspend fun upsertNote(note: MaesterNoteEntity)

  @Query("DELETE FROM maester_notes WHERE id = :noteId")
  suspend fun deleteNoteById(noteId: Long)
}
