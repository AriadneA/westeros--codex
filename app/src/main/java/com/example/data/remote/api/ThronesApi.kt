package com.example.data.remote.api

import com.example.data.remote.dto.ThronesCharacterDto
import retrofit2.http.GET

/**
 * Cliente Retrofit para a ThronesAPI,
 * fonte complementar de retratos e títulos de personagens no MVP.
 */
interface ThronesApi {

  @GET("Characters")
  suspend fun getCharacters(): List<ThronesCharacterDto>

  companion object {
    const val BASE_URL = "https://thronesapi.com/api/v2/"
  }
}
