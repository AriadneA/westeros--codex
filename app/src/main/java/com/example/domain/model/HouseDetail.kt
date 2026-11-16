package com.example.domain.model

/**
 * Visão agregada de domínio para a tela de detalhes de uma Casa,
 * reunindo metadados históricos, retratos associados e anotações do Meistre.
 */
data class HouseDetail(
  val house: House,
  val founded: String,
  val diedOut: String,
  val overlordId: String?,
  val characters: List<Character>,
  val notes: List<MaesterNote>,
)
