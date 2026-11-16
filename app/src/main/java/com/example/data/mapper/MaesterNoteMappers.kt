package com.example.data.mapper

import com.example.data.local.entity.MaesterNoteEntity
import com.example.domain.model.MaesterNote

fun MaesterNoteEntity.toDomain(): MaesterNote {
  return MaesterNote(
    id = id,
    houseId = houseId,
    content = content,
    updatedAtMillis = updatedAtMillis,
  )
}
