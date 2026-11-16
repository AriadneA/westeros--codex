package com.example.domain.usecase

import com.example.domain.repository.HouseRepository
import javax.inject.Inject

/**
 * Caso de uso para criar ou atualizar uma Anotação do Meistre vinculada a uma Casa.
 */
class SaveMaesterNoteUseCase @Inject constructor(
  private val repository: HouseRepository,
) {
  suspend operator fun invoke(
    houseId: String,
    noteId: Long? = null,
    content: String,
  ): Result<Unit> {
    val normalizedHouseId = houseId.trim()
    val normalizedContent = content.trim()
    if (normalizedHouseId.isEmpty()) {
      return Result.failure(IllegalArgumentException("O identificador da Casa não pode ser vazio."))
    }
    if (normalizedContent.isEmpty()) {
      return Result.failure(IllegalArgumentException("A anotação do Meistre não pode estar vazia."))
    }
    return repository.saveMaesterNote(
      houseId = normalizedHouseId,
      noteId = noteId,
      content = normalizedContent,
    )
  }
}
