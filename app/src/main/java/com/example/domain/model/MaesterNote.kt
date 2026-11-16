package com.example.domain.model

/**
 * Modelo de domínio puro que representa uma anotação pessoal do Meistre
 * vinculada a uma Casa específica e persistida localmente.
 */
data class MaesterNote(
  val id: Long,
  val houseId: String,
  val content: String,
  val updatedAtMillis: Long,
)
