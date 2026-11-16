package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.data.local.entity.CharacterEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para persistência e consulta reativa dos personagens da ThronesAPI.
 */
@Dao
interface CharacterDao {

  @Upsert
  suspend fun upsertCharacters(characters: List<CharacterEntity>)

  @Query(
    """
    SELECT * FROM characters
    WHERE normalizedFamilyKey = :familyKey AND normalizedFamilyKey != ''
    ORDER BY id ASC
    """,
  )
  fun observeCharactersByFamilyKey(familyKey: String): Flow<List<CharacterEntity>>

  @Query("SELECT COUNT(*) FROM characters")
  suspend fun countCharacters(): Int
}
