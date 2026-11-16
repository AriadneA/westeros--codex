package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO correspondente ao contrato JSON do endpoint `GET /api/v2/Characters`
 * da ThronesAPI.
 */
@JsonClass(generateAdapter = true)
data class ThronesCharacterDto(
  @Json(name = "id") val id: Int = 0,
  @Json(name = "firstName") val firstName: String = "",
  @Json(name = "lastName") val lastName: String = "",
  @Json(name = "fullName") val fullName: String = "",
  @Json(name = "title") val title: String = "",
  @Json(name = "family") val family: String = "",
  @Json(name = "image") val image: String = "",
  @Json(name = "imageUrl") val imageUrl: String = "",
)
