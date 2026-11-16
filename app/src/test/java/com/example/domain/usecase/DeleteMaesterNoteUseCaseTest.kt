package com.example.domain.usecase

import com.example.domain.repository.HouseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteMaesterNoteUseCaseTest {

  private lateinit var repository: HouseRepository
  private lateinit var useCase: DeleteMaesterNoteUseCase

  @Before
  fun setUp() {
    repository = mockk()
    useCase = DeleteMaesterNoteUseCase(repository)
  }

  @Test
  fun `invoke rejects zero or negative noteId`() = runTest {
    assertTrue(useCase(0L).isFailure)
    assertTrue(useCase(-5L).isFailure)
    coVerify(exactly = 0) { repository.deleteMaesterNote(any()) }
  }

  @Test
  fun `invoke delegates valid positive noteId deletion to repository`() = runTest {
    coEvery { repository.deleteMaesterNote(12L) } returns Result.success(Unit)

    val result = useCase(12L)

    assertTrue(result.isSuccess)
    coVerify(exactly = 1) { repository.deleteMaesterNote(12L) }
  }
}
