package com.example.presentation.houses

import com.example.domain.model.House
import com.example.domain.usecase.GetAvailableRegionsUseCase
import com.example.domain.usecase.GetHousesUseCase
import com.example.domain.usecase.SyncHousesPageUseCase
import com.example.domain.usecase.SyncInitialDataUseCase
import com.example.domain.usecase.ToggleAllegianceUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HousesViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var getHousesUseCase: GetHousesUseCase
  private lateinit var getAvailableRegionsUseCase: GetAvailableRegionsUseCase
  private lateinit var syncInitialDataUseCase: SyncInitialDataUseCase
  private lateinit var syncHousesPageUseCase: SyncHousesPageUseCase
  private lateinit var toggleAllegianceUseCase: ToggleAllegianceUseCase

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    getHousesUseCase = mockk()
    getAvailableRegionsUseCase = mockk()
    syncInitialDataUseCase = mockk()
    syncHousesPageUseCase = mockk()
    toggleAllegianceUseCase = mockk()

    every { getAvailableRegionsUseCase() } returns flowOf(listOf("Dorne", "The North"))
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `emits Loading while initial sync is running on empty cache then Success when houses arrive`() =
    runTest(testDispatcher) {
      val housesFlow = MutableStateFlow<List<House>>(emptyList())
      val syncGate = CompletableDeferred<Result<Unit>>()
      every { getHousesUseCase("", null, false) } returns housesFlow
      coEvery { syncInitialDataUseCase() } coAnswers { syncGate.await() }

      val viewModel = createViewModel()
      val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.collect {}
      }

      testScheduler.runCurrent()
      assertTrue(viewModel.uiState.value is HousesUiState.Loading)

      housesFlow.value = listOf(sampleHouse(id = "362", name = "House Stark of Winterfell"))
      syncGate.complete(Result.success(Unit))
      advanceUntilIdle()

      val state = viewModel.uiState.value as HousesUiState.Success
      assertEquals(1, state.houses.size)
      assertEquals("House Stark of Winterfell", state.houses.first().name)
      assertNull(state.nonBlockingError)

      collectJob.cancel()
    }

  @Test
  fun `emits Error when initial sync fails and local cache is empty, then recovers on retry`() =
    runTest(testDispatcher) {
      val housesFlow = MutableStateFlow<List<House>>(emptyList())
      every { getHousesUseCase("", null, false) } returns housesFlow
      coEvery { syncInitialDataUseCase() } returnsMany listOf(
        Result.failure(IOException("Offline")),
        Result.success(Unit),
      )

      val viewModel = createViewModel()
      val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.collect {}
      }

      advanceUntilIdle()
      assertTrue(viewModel.uiState.value is HousesUiState.Error)

      housesFlow.value = listOf(sampleHouse(id = "229", name = "House Lannister"))
      viewModel.onRetry()
      advanceUntilIdle()

      assertTrue(viewModel.uiState.value is HousesUiState.Success)
      coVerify(exactly = 2) { syncInitialDataUseCase() }

      collectJob.cancel()
    }

  @Test
  fun `emits Empty with NO_SEARCH_RESULTS or NO_SWORN_HOUSES depending on active filters`() =
    runTest(testDispatcher) {
      every { getHousesUseCase("", null, false) } returns
        flowOf(listOf(sampleHouse(id = "362", name = "House Stark")))
      every { getHousesUseCase("Valyria", null, false) } returns flowOf(emptyList())
      every { getHousesUseCase("", null, true) } returns flowOf(emptyList())
      coEvery { syncInitialDataUseCase() } returns Result.success(Unit)

      val viewModel = createViewModel()
      val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.collect {}
      }

      advanceUntilIdle()
      assertTrue(viewModel.uiState.value is HousesUiState.Success)

      viewModel.onSearchQueryChange("Valyria")
      advanceUntilIdle()
      val searchEmpty = viewModel.uiState.value as HousesUiState.Empty
      assertEquals(EmptyReason.NO_SEARCH_RESULTS, searchEmpty.reason)

      viewModel.onClearFilters()
      viewModel.onOnlySwornChanged(true)
      advanceUntilIdle()
      val swornEmpty = viewModel.uiState.value as HousesUiState.Empty
      assertEquals(EmptyReason.NO_SWORN_HOUSES, swornEmpty.reason)

      collectJob.cancel()
    }

  @Test
  fun `onLoadNextPage increments page on success and ignores duplicate calls when no more pages`() =
    runTest(testDispatcher) {
      every { getHousesUseCase("", null, false) } returns
        flowOf(listOf(sampleHouse(id = "362", name = "House Stark")))
      coEvery { syncInitialDataUseCase() } returns Result.success(Unit)
      coEvery { syncHousesPageUseCase(page = 2) } returns Result.success(false)

      val viewModel = createViewModel()
      val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.collect {}
      }

      advanceUntilIdle()
      viewModel.onLoadNextPage()
      advanceUntilIdle()

      val state = viewModel.uiState.value as HousesUiState.Success
      assertFalse(state.hasMorePages)
      assertFalse(state.isPaginating)

      viewModel.onLoadNextPage()
      advanceUntilIdle()
      coVerify(exactly = 1) { syncHousesPageUseCase(page = 2) }

      collectJob.cancel()
    }

  @Test
  fun `onRefresh preserves cached houses and surfaces nonBlockingError on failure`() =
    runTest(testDispatcher) {
      every { getHousesUseCase("", null, false) } returns
        flowOf(listOf(sampleHouse(id = "362", name = "House Stark")))
      coEvery { syncInitialDataUseCase() } returns Result.success(Unit)
      coEvery { syncHousesPageUseCase(page = 1) } returns Result.failure(IOException("Timeout"))

      val viewModel = createViewModel()
      val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.collect {}
      }

      advanceUntilIdle()
      viewModel.onRefresh()
      advanceUntilIdle()

      val state = viewModel.uiState.value as HousesUiState.Success
      assertEquals(1, state.houses.size)
      assertNotNull(state.nonBlockingError)

      viewModel.onDismissError()
      advanceUntilIdle()
      assertNull((viewModel.uiState.value as HousesUiState.Success).nonBlockingError)

      collectJob.cancel()
    }

  @Test
  fun `onToggleAllegiance delegates to ToggleAllegianceUseCase`() =
    runTest(testDispatcher) {
      every { getHousesUseCase("", null, false) } returns
        flowOf(listOf(sampleHouse(id = "362", name = "House Stark")))
      coEvery { syncInitialDataUseCase() } returns Result.success(Unit)
      coEvery { toggleAllegianceUseCase("362") } returns Result.success(Unit)

      val viewModel = createViewModel()
      val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.collect {}
      }

      advanceUntilIdle()
      viewModel.onToggleAllegiance("362")
      advanceUntilIdle()

      coVerify(exactly = 1) { toggleAllegianceUseCase("362") }
      collectJob.cancel()
    }

  private fun createViewModel(): HousesViewModel {
    return HousesViewModel(
      getHousesUseCase = getHousesUseCase,
      getAvailableRegionsUseCase = getAvailableRegionsUseCase,
      syncInitialDataUseCase = syncInitialDataUseCase,
      syncHousesPageUseCase = syncHousesPageUseCase,
      toggleAllegianceUseCase = toggleAllegianceUseCase,
    )
  }

  private fun sampleHouse(id: String, name: String): House {
    return House(
      id = id,
      name = name,
      region = "The North",
      coatOfArms = "A grey direwolf",
      words = "Winter is Coming",
      titles = listOf("King in the North"),
      seats = listOf("Winterfell"),
      ancestralWeapons = listOf("Ice"),
      isSworn = false,
      notesCount = 0,
    )
  }
}
