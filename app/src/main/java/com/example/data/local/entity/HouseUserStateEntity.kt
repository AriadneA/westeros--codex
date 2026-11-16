package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Armazena o estado local de Juramento de Lealdade (favorito) do usuário.
 * Tabela isolada para garantir preservação integral durante sincronizações remotas.
 */
@Entity(tableName = "house_user_state")
data class HouseUserStateEntity(
  @PrimaryKey val houseId: String,
  val isSworn: Boolean,
  val swornAtMillis: Long?,
)
