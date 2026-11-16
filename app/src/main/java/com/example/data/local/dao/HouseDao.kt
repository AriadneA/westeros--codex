package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.data.local.entity.HouseEntity
import com.example.data.local.entity.HouseUserStateEntity
import com.example.data.local.entity.HouseWithUserState
import kotlinx.coroutines.flow.Flow

/**
 * DAO responsável pelo acesso reativo às Casas e ao estado de lealdade local.
 * Utiliza `@Upsert` em [HouseEntity] para atualizar metadados remotos sem apagar
 * lealdades ou anotações pessoais vinculadas.
 */
@Dao
interface HouseDao {

  @Upsert
  suspend fun upsertHouses(houses: List<HouseEntity>)

  @Transaction
  @Query(
    """
    SELECT houses.* FROM houses
    LEFT JOIN house_user_state ON houses.id = house_user_state.houseId
    WHERE (:query = '' OR houses.name LIKE '%' || :query || '%' OR houses.words LIKE '%' || :query || '%')
      AND (:region IS NULL OR houses.region = :region)
      AND (:onlySworn = 0 OR house_user_state.isSworn = 1)
    ORDER BY houses.pageIndex ASC, houses.name ASC
    """,
  )
  fun observeHouses(
    query: String,
    region: String?,
    onlySworn: Boolean,
  ): Flow<List<HouseWithUserState>>

  @Transaction
  @Query("SELECT * FROM houses WHERE id = :houseId LIMIT 1")
  fun observeHouseById(houseId: String): Flow<HouseWithUserState?>

  @Query("SELECT DISTINCT region FROM houses WHERE region != '' ORDER BY region ASC")
  fun observeAvailableRegions(): Flow<List<String>>

  @Query("SELECT COUNT(*) FROM houses")
  suspend fun countHouses(): Int

  @Query("SELECT * FROM house_user_state WHERE houseId = :houseId LIMIT 1")
  suspend fun getUserState(houseId: String): HouseUserStateEntity?

  @Upsert
  suspend fun upsertUserState(state: HouseUserStateEntity)
}
