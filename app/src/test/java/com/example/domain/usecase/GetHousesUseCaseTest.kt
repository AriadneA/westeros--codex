package com.example.domain.usecase

import com.example.domain.model.House
import com.example.domain.repository.HouseRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
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
  fun `invoke uses default parameters when called without arguments`() = runTest {
    val houses = listOf(sampleHouse(id = "362", name = "House Stark of Winterfell"))
    every { repository.observeHouses(query = "", region = null, onlySworn = false) } returns
      flowOf(houses)

    val result = useCase().first()

    assertEquals(houses, result)
    verify(exactly = 1) { repository.observeHouses(query = "", region = null, onlySworn = false) }
  }

  @Test
  fun `invoke trims query and converts blank region to null`() = runTest {
    val expectedHouses = listOf(sampleHouse(id = "362", name = "House Stark of Winterfell", isSworn = true))
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

  @Test
  fun `invoke propagates reactive SSOT emissions from repository`() = runTest {
    val initial = listOf(sampleHouse(id = "362", name = "House Stark", isSworn = false))
    val updated = listOf(sampleHouse(id = "362", name = "House Stark", isSworn = true, notesCount = 2))
    val ssotFlow = MutableStateFlow(initial)
    every { repository.observeHouses(query = "", region = null, onlySworn = false) } returns ssotFlow

    val collected = mutableListOf<List<House>>()
    val flow = useCase()

    // Emissão inicial e emissão reativa após atualização local no Room
    collected += flow.first()
    ssotFlow.value = updated
    collected += flow.take(1).toList().first()

    assertEquals(listOf(initial, updated), collected)
  }

  private fun sampleHouse(
    id: String,
    name: String,
    isSworn: Boolean = false,
    notesCount: Int = 0,
  ): House {
    return House(
      id = id,
      name = name,
      region = "The North",
      coatOfArms = "A grey direwolf",
      words = "Winter is Coming",
      titles = listOf("King in the North"),
      seats = listOf("Winterfell"),
      ancestralWeapons = listOf("Ice"),
      isSworn = isSworn,
      notesCount = notesCount,
    )
  }
}
