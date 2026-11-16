package com.example.domain.usecase

import com.example.domain.repository.HouseRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso que expõe as regiões distintas disponíveis no cache local
 * para preenchimento dos chips de filtro regional.
 */
class GetAvailableRegionsUseCase @Inject constructor(
  private val repository: HouseRepository,
) {
  operator fun invoke(): Flow<List<String>> = repository.observeAvailableRegions()
}
