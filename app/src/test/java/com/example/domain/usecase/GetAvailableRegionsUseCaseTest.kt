package com.example.domain.usecase

import com.example.domain.repository.HouseRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetAvailableRegionsUseCaseTest {

  private lateinit var repository: HouseRepository
  private lateinit var useCase: GetAvailableRegionsUseCase

  @Before
  fun setUp() {
    repository = mockk()
    useCase = GetAvailableRegionsUseCase(repository)
  }

  @Test
  fun `invoke returns distinct regions flow from repository`() = runTest {
    val regions = listOf("Dorne", "The North", "The Westerlands")
    every { repository.observeAvailableRegions() } returns flowOf(regions)

    val result = useCase().first()

    assertEquals(regions, result)
    verify(exactly = 1) { repository.observeAvailableRegions() }
  }
}
