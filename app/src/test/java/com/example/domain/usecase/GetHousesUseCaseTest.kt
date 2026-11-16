package com.example.domain.usecase

import com.example.domain.model.House
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

class GetHousesUseCaseTest {

  private lateinit var repository: HouseRepository
  private lateinit var useCase: GetHousesUseCase

  @Before
  fun setUp() {
    repository = mockk()
    useCase = GetHousesUseCase(repository)
  }

  @Test
  fun `invoke trims query and blank region before observing repository`() = runTest {
    val expectedHouses =
      listOf(
        House(
          id = "362",
          name = "House Stark of Winterfell",
          region = "The North",
          coatOfArms = "A grey direwolf",
          words = "Winter is Coming",
          titles = listOf("King in the North"),
          seats = listOf("Winterfell"),
          ancestralWeapons = listOf("Ice"),
          isSworn = true,
          notesCount = 1,
        ),
      )
    every {
      repository.observeHouses(query = "Stark", region = null, onlySworn = true)
    } returns flowOf(expectedHouses)

    val result = useCase(query = "  Stark  ", region = "   ", onlySworn = true).first()

    assertEquals(expectedHouses, result)
    verify(exactly = 1) {
      repository.observeHouses(query = "Stark", region = null, onlySworn = true)
    }
  }

  @Test
  fun `invoke preserves valid trimmed region filter`() = runTest {
    every {
      repository.observeHouses(query = "", region = "Dorne", onlySworn = false)
    } returns flowOf(emptyList())

    val result = useCase(query = "", region = " Dorne ", onlySworn = false).first()

    assertEquals(emptyList<House>(), result)
    verify(exactly = 1) {
      repository.observeHouses(query = "", region = "Dorne", onlySworn = false)
    }
  }
}
