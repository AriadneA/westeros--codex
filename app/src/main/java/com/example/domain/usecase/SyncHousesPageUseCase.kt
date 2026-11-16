package com.example.domain.usecase

import com.example.domain.repository.HouseRepository
import javax.inject.Inject

/**
 * Caso de uso para sincronizar uma página específica de Casas sob demanda
 * ou via pull-to-refresh.
 */
class SyncHousesPageUseCase @Inject constructor(
  private val repository: HouseRepository,
) {
  suspend operator fun invoke(
    page: Int,
    pageSize: Int = DEFAULT_PAGE_SIZE,
  ): Result<Boolean> {
    if (page < 1 || pageSize <= 0) {
      return Result.failure(
        IllegalArgumentException("Parâmetros de paginação inválidos: page=$page, pageSize=$pageSize"),
      )
    }
    return repository.syncHousesPage(page = page, pageSize = pageSize)
  }

  companion object {
    const val DEFAULT_PAGE_SIZE = 25
  }
}
