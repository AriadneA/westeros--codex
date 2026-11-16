package com.example.domain.model

/**
 * Modelo de domínio puro que representa um personagem de Westeros
 * utilizado para enriquecer visualmente os detalhes das Casas (ThronesAPI).
 */
data class Character(
  val id: Int,
  val firstName: String,
  val lastName: String,
  val fullName: String,
  val title: String,
  val family: String,
  val imageUrl: String,
)
