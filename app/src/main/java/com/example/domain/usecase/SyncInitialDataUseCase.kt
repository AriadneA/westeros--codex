package com.example.domain.usecase

import com.example.domain.repository.HouseRepository
import javax.inject.Inject

/**
 * Caso de uso que dispara a sincronização inicial paginada (página 1) e de
 * personagens apenas quando o cache local estiver vazio.
 */
class SyncInitialDataUseCase @Inject constructor(
  private val repository: HouseRepository,
) {
  suspend operator fun invoke(): Result<Unit> = repository.syncInitialDataIfEmpty()
}
