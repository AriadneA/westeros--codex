package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO correspondente ao contrato JSON do endpoint `GET /api/houses`
 * da An API of Ice and Fire.
 */
@JsonClass(generateAdapter = true)
data class HouseDto(
  @Json(name = "url") val url: String = "",
  @Json(name = "name") val name: String = "",
  @Json(name = "region") val region: String = "",
  @Json(name = "coatOfArms") val coatOfArms: String = "",
  @Json(name = "words") val words: String = "",
  @Json(name = "titles") val titles: List<String> = emptyList(),
  @Json(name = "seats") val seats: List<String> = emptyList(),
  @Json(name = "currentLord") val currentLord: String = "",
  @Json(name = "heir") val heir: String = "",
  @Json(name = "overlord") val overlord: String = "",
  @Json(name = "founded") val founded: String = "",
  @Json(name = "founder") val founder: String = "",
  @Json(name = "diedOut") val diedOut: String = "",
  @Json(name = "ancestralWeapons") val ancestralWeapons: List<String> = emptyList(),
  @Json(name = "cadetBranches") val cadetBranches: List<String> = emptyList(),
  @Json(name = "swornMembers") val swornMembers: List<String> = emptyList(),
)
