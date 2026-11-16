package com.example.domain.usecase

import com.example.domain.repository.HouseRepository
import javax.inject.Inject

/**
 * Caso de uso para remover uma Anotação do Meistre armazenada localmente.
 */
class DeleteMaesterNoteUseCase @Inject constructor(
  private val repository: HouseRepository,
) {
  suspend operator fun invoke(noteId: Long): Result<Unit> {
    if (noteId <= 0L) {
      return Result.failure(IllegalArgumentException("Identificador de anotação inválido."))
    }
    return repository.deleteMaesterNote(noteId)
  }
}
