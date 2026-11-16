package com.example.data.remote.api

import com.example.data.remote.dto.HouseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Cliente Retrofit para a An API of Ice and Fire,
 * fonte primária de dados históricos e heráldicos das Casas de Westeros.
 */
interface IceAndFireApi {

  @GET("houses")
  suspend fun getHouses(
    @Query("page") page: Int,
    @Query("pageSize") pageSize: Int,
  ): List<HouseDto>

  @GET("houses/{id}")
  suspend fun getHouseById(
    @Path("id") houseId: String,
  ): HouseDto

  companion object {
    const val BASE_URL = "https://anapioficeandfire.com/api/"
  }
}
