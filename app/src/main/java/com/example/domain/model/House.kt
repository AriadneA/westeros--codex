package com.example.domain.model

/**
 * Modelo de domínio puro que representa uma Casa Nobre de Westeros.
 * Livre de quaisquer dependências de frameworks Android, Room ou Retrofit.
 */
data class House(
  val id: String,
  val name: String,
  val region: String,
  val coatOfArms: String,
  val words: String,
  val titles: List<String>,
  val seats: List<String>,
  val ancestralWeapons: List<String>,
  val isSworn: Boolean = false,
  val notesCount: Int = 0,
)
