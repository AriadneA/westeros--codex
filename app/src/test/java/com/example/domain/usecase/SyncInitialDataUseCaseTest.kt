package com.example.domain.usecase

import com.example.domain.repository.HouseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncInitialDataUseCaseTest {

  private lateinit var repository: HouseRepository
  private lateinit var useCase: SyncInitialDataUseCase

  @Before
  fun setUp() {
    repository = mockk()
    useCase = SyncInitialDataUseCase(repository)
  }

  @Test
  fun `invoke delegates initial sync to repository and returns success`() = runTest {
    coEvery { repository.syncInitialDataIfEmpty() } returns Result.success(Unit)

    val result = useCase()

    assertTrue(result.isSuccess)
    coVerify(exactly = 1) { repository.syncInitialDataIfEmpty() }
  }

  @Test
  fun `invoke propagates failure Result when repository sync fails`() = runTest {
    coEvery { repository.syncInitialDataIfEmpty() } returns Result.failure(IOException("Offline"))

    val result = useCase()

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IOException)
  }
}
