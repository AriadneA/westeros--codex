package com.example.domain.usecase

import com.example.domain.model.House
import com.example.domain.repository.HouseRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso responsável por observar continuamente as Casas de Westeros
 * a partir da fonte única da verdade (Room), com suporte a busca textual,
 * filtro regional e filtro de Casas Juradas.
 */
class GetHousesUseCase @Inject constructor(
  private val repository: HouseRepository,
) {
  operator fun invoke(
    query: String = "",
    region: String? = null,
    onlySworn: Boolean = false,
  ): Flow<List<House>> {
    return repository.observeHouses(
      query = query.trim(),
      region = region?.trim()?.takeIf { it.isNotEmpty() },
      onlySworn = onlySworn,
    )
  }
}
