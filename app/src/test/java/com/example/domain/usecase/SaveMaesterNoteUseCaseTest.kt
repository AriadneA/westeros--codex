package com.example.domain.usecase

import com.example.domain.repository.HouseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveMaesterNoteUseCaseTest {

  private lateinit var repository: HouseRepository
  private lateinit var useCase: SaveMaesterNoteUseCase

  @Before
  fun setUp() {
    repository = mockk()
    useCase = SaveMaesterNoteUseCase(repository)
  }

  @Test
  fun `invoke fails when houseId or note content is blank`() = runTest {
    val blankHouse = useCase(houseId = " ", content = "Nota válida")
    val blankContent = useCase(houseId = "362", content = "   ")

    assertTrue(blankHouse.isFailure)
    assertTrue(blankContent.isFailure)
    coVerify(exactly = 0) { repository.saveMaesterNote(any(), any(), any()) }
  }

  @Test
  fun `invoke trims inputs and delegates persistence to repository`() = runTest {
    coEvery {
      repository.saveMaesterNote(houseId = "362", noteId = 10L, content = "O Norte se lembra")
    } returns Result.success(Unit)

    val result =
      useCase(
        houseId = " 362 ",
        noteId = 10L,
        content = "  O Norte se lembra  ",
      )

    assertTrue(result.isSuccess)
    coVerify(exactly = 1) {
      repository.saveMaesterNote(houseId = "362", noteId = 10L, content = "O Norte se lembra")
    }
  }
}
