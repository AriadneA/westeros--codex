package com.example.domain.usecase

import com.example.domain.model.HouseDetail
import com.example.domain.repository.HouseRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso para observar os detalhes completos de uma Casa,
 * combinando metadados heráldicos, retratos associados e anotações do Meistre.
 */
class GetHouseDetailUseCase @Inject constructor(
  private val repository: HouseRepository,
) {
  operator fun invoke(houseId: String): Flow<HouseDetail?> {
    return repository.observeHouseDetail(houseId.trim())
  }
}
