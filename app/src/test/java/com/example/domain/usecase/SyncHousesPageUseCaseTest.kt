package com.example.domain.usecase

import com.example.domain.repository.HouseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncHousesPageUseCaseTest {

  private lateinit var repository: HouseRepository
  private lateinit var useCase: SyncHousesPageUseCase

  @Before
  fun setUp() {
    repository = mockk()
    useCase = SyncHousesPageUseCase(repository)
  }

  @Test
  fun `invoke rejects non-positive page or pageSize`() = runTest {
    assertTrue(useCase(page = 0, pageSize = 25).isFailure)
    assertTrue(useCase(page = 1, pageSize = 0).isFailure)
    coVerify(exactly = 0) { repository.syncHousesPage(any(), any()) }
  }

  @Test
  fun `invoke delegates valid pagination request to repository`() = runTest {
    coEvery { repository.syncHousesPage(page = 2, pageSize = 25) } returns Result.success(true)

    val result = useCase(page = 2, pageSize = 25)

    assertTrue(result.isSuccess)
    assertEquals(true, result.getOrThrow())
    coVerify(exactly = 1) { repository.syncHousesPage(page = 2, pageSize = 25) }
  }
}
