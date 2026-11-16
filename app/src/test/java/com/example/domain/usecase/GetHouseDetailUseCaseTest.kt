package com.example.domain.usecase

import com.example.domain.model.House
import com.example.domain.model.HouseDetail
import com.example.domain.repository.HouseRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GetHouseDetailUseCaseTest {

  private lateinit var repository: HouseRepository
  private lateinit var useCase: GetHouseDetailUseCase

  @Before
  fun setUp() {
    repository = mockk()
    useCase = GetHouseDetailUseCase(repository)
  }

  @Test
  fun `invoke trims houseId and emits HouseDetail from repository`() = runTest {
    val detail =
      HouseDetail(
        house =
          House(
            id = "362",
            name = "House Stark of Winterfell",
            region = "The North",
            coatOfArms = "Direwolf",
            words = "Winter is Coming",
            titles = listOf("King in the North"),
            seats = listOf("Winterfell"),
            ancestralWeapons = listOf("Ice"),
            isSworn = true,
            notesCount = 1,
          ),
        founded = "Age of Heroes",
        diedOut = "",
        overlordId = null,
        characters = emptyList(),
        notes = emptyList(),
      )
    every { repository.observeHouseDetail("362") } returns flowOf(detail)

    val result = useCase("  362  ").first()

    assertEquals(detail, result)
    verify(exactly = 1) { repository.observeHouseDetail("362") }
  }

  @Test
  fun `invoke emits null when house does not exist`() = runTest {
    every { repository.observeHouseDetail("999") } returns flowOf(null)

    val result = useCase("999").first()

    assertNull(result)
  }
}
