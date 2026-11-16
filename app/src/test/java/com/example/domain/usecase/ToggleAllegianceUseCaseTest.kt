package com.example.domain.usecase

import com.example.domain.repository.HouseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToggleAllegianceUseCaseTest {

  private lateinit var repository: HouseRepository
  private lateinit var useCase: ToggleAllegianceUseCase

  @Before
  fun setUp() {
    repository = mockk()
    useCase = ToggleAllegianceUseCase(repository)
  }

  @Test
  fun `invoke fails with IllegalArgumentException when houseId is blank`() = runTest {
    val result = useCase("   ")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    coVerify(exactly = 0) { repository.toggleAllegiance(any()) }
  }

  @Test
  fun `invoke trims houseId and delegates allegiance toggle to repository`() = runTest {
    coEvery { repository.toggleAllegiance("362") } returns Result.success(Unit)

    val result = useCase(" 362 ")

    assertTrue(result.isSuccess)
    coVerify(exactly = 1) { repository.toggleAllegiance("362") }
  }
}
