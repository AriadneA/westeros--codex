package com.example.domain.usecase

import com.example.domain.repository.HouseRepository
import javax.inject.Inject

/**
 * Caso de uso responsável por Jurar ou Revogar Lealdade (favoritar localmente)
 * a uma Casa específica de Westeros.
 */
class ToggleAllegianceUseCase @Inject constructor(
  private val repository: HouseRepository,
) {
  suspend operator fun invoke(houseId: String): Result<Unit> {
    val normalizedId = houseId.trim()
    if (normalizedId.isEmpty()) {
      return Result.failure(
        IllegalArgumentException("O identificador da Casa não pode ser vazio."),
      )
    }
    return repository.toggleAllegiance(normalizedId)
  }
}
