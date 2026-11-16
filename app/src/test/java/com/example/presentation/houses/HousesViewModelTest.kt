package com.example.presentation.houses

import com.example.domain.model.House
import com.example.domain.usecase.GetAvailableRegionsUseCase
import com.example.domain.usecase.GetHousesUseCase
import com.example.domain.usecase.SyncHousesPageUseCase
import com.example.domain.usecase.SyncInitialDataUseCase
import com.example.domain.usecase.ToggleAllegianceUseCase
import com.example.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HousesViewModelTest {

  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private lateinit var getHousesUseCase: GetHousesUseCase
  private lateinit var getAvailableRegionsUseCase: GetAvailableRegionsUseCase
  private lateinit var syncInitialDataUseCase: SyncInitialDataUseCase
  private lateinit var syncHousesPageUseCase: SyncHousesPageUseCase
  private lateinit var toggleAllegianceUseCase: ToggleAllegianceUseCase

  @Before
  fun setUp() {
    getHousesUseCase = mockk()
    getAvailableRegionsUseCase = mockk()
    syncInitialDataUseCase = mockk()
    syncHousesPageUseCase = mockk()
    toggleAllegianceUseCase = mockk()

    every { getAvailableRegionsUseCase() } returns flowOf(listOf("Dorne", "The North"))
  }

  @Test
  fun `stateFlow emits Loading first and transitions to Success when initial sync populates cache`() =
    runTest(mainDispatcherRule.testDispatcher) {
      val housesFlow = MutableStateFlow<List<House>>(emptyList())
      val syncGate = CompletableDeferred<Result<Unit>>()
      every { getHousesUseCase("", null, false) } returns housesFlow
      coEvery { syncInitialDataUseCase() } coAnswers { syncGate.await() }

      val viewModel = createViewModel()
      val emittedStates = mutableListOf<HousesUiState>()
      val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.collect { emittedStates.add(it) }
      }

      testScheduler.runCurrent()
      assertTrue(emittedStates.last() is HousesUiState.Loading)

      housesFlow.value = listOf(sampleHouse(id = "362", name = "House Stark of Winterfell"))
      syncGate.complete(Result.success(Unit))
      advanceUntilIdle()

      assertTrue(emittedStates.any { it is HousesUiState.Loading })
      val finalState = emittedStates.last() as HousesUiState.Success
      assertEquals(1, finalState.houses.size)
      assertEquals("House Stark of Winterfell", finalState.houses.first().name)
      assertNull(finalState.nonBlockingError)

      collectJob.cancel()
    }

  @Test
  fun `stateFlow emits Loading then Error on empty cache sync failure, and recovers to Success on retry`() =
    runTest(mainDispatcherRule.testDispatcher) {
      val housesFlow = MutableStateFlow<List<House>>(emptyList())
      val retryGate = CompletableDeferred<Result<Unit>>()
      every { getHousesUseCase("", null, false) } returns housesFlow
      coEvery { syncInitialDataUseCase() } returns Result.failure(IOException("Offline"))

      val viewModel = createViewModel()
      val emittedStates = mutableListOf<HousesUiState>()
      val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.collect { emittedStates.add(it) }
      }

      advanceUntilIdle()
      assertTrue(emittedStates.first() is HousesUiState.Loading)
      assertTrue(emittedStates.last() is HousesUiState.Error)

      coEvery { syncInitialDataUseCase() } coAnswers { retryGate.await() }
      viewModel.onRetry()
      testScheduler.runCurrent()
      assertTrue(emittedStates.last() is HousesUiState.Loading)

      housesFlow.value = listOf(sampleHouse(id = "229", name = "House Lannister"))
      retryGate.complete(Result.success(Unit))
      advanceUntilIdle()

      assertTrue(emittedStates.last() is HousesUiState.Success)
      coVerify(exactly = 2) { syncInitialDataUseCase() }

      collectJob.cancel()
    }

  @Test
  fun `offline cache stays visible as Success with nonBlockingError even when sync fails`() =
    runTest(mainDispatcherRule.testDispatcher) {
      val cachedHouses = listOf(sampleHouse(id = "362", name = "House Stark"))
      every { getHousesUseCase("", null, false) } returns flowOf(cachedHouses)
      coEvery { syncInitialDataUseCase() } returns Result.failure(IOException("Sem rede"))

      val viewModel = createViewModel()
      val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.collect {}
      }

      advanceUntilIdle()

      val state = viewModel.uiState.value as HousesUiState.Success
      assertEquals(1, state.houses.size)
      assertNotNull(state.nonBlockingError)

      collectJob.cancel()
    }

  @Test
  fun `emits Empty for search and sworn filters and restores Success when cleared`() =
    runTest(mainDispatcherRule.testDispatcher) {
      every { getHousesUseCase("", null, false) } returns
        flowOf(listOf(sampleHouse(id = "362", name = "House Stark")))
      every { getHousesUseCase("Valyria", null, false) } returns flowOf(emptyList())
      every { getHousesUseCase("", "Dorne", false) } returns
        flowOf(listOf(sampleHouse(id = "285", name = "House Martell", region = "Dorne")))
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
      viewModel.onRegionSelected("Dorne")
      advanceUntilIdle()
      val dorneState = viewModel.uiState.value as HousesUiState.Success
      assertEquals("Dorne", dorneState.selectedRegion)
      assertEquals("House Martell", dorneState.houses.single().name)

      viewModel.onClearFilters()
      viewModel.onOnlySwornChanged(true)
      advanceUntilIdle()
      val swornEmpty = viewModel.uiState.value as HousesUiState.Empty
      assertEquals(EmptyReason.NO_SWORN_HOUSES, swornEmpty.reason)

      collectJob.cancel()
    }

  @Test
  fun `onLoadNextPage advances pages sequentially and skips when filtered or exhausted`() =
    runTest(mainDispatcherRule.testDispatcher) {
      every { getHousesUseCase("", null, false) } returns
        flowOf(listOf(sampleHouse(id = "362", name = "House Stark")))
      every { getHousesUseCase("", "The North", false) } returns
        flowOf(listOf(sampleHouse(id = "362", name = "House Stark")))
      coEvery { syncInitialDataUseCase() } returns Result.success(Unit)
      coEvery { syncHousesPageUseCase(page = 2) } returns Result.success(true)
      coEvery { syncHousesPageUseCase(page = 3) } returns Result.success(false)

      val viewModel = createViewModel()
      val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.collect {}
      }

      advanceUntilIdle()

      // Com filtro regional ativo, paginação não deve ser disparada
      viewModel.onRegionSelected("The North")
      advanceUntilIdle()
      viewModel.onLoadNextPage()
      advanceUntilIdle()
      coVerify(exactly = 0) { syncHousesPageUseCase(any()) }

      // Sem filtros, avança para página 2 e depois página 3
      viewModel.onRegionSelected(null)
      advanceUntilIdle()
      viewModel.onLoadNextPage()
      advanceUntilIdle()
      assertTrue((viewModel.uiState.value as HousesUiState.Success).hasMorePages)

      viewModel.onLoadNextPage()
      advanceUntilIdle()
      val exhaustedState = viewModel.uiState.value as HousesUiState.Success
      assertFalse(exhaustedState.hasMorePages)
      assertFalse(exhaustedState.isPaginating)

      // Após fim do catálogo remoto, não chama página 4
      viewModel.onLoadNextPage()
      advanceUntilIdle()
      coVerify(exactly = 1) { syncHousesPageUseCase(page = 2) }
      coVerify(exactly = 1) { syncHousesPageUseCase(page = 3) }

      collectJob.cancel()
    }

  @Test
  fun `onRefresh preserves cached houses and surfaces nonBlockingError on failure`() =
    runTest(mainDispatcherRule.testDispatcher) {
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
  fun `onToggleAllegiance delegates to ToggleAllegianceUseCase and exposes error on failure`() =
    runTest(mainDispatcherRule.testDispatcher) {
      every { getHousesUseCase("", null, false) } returns
        flowOf(listOf(sampleHouse(id = "362", name = "House Stark")))
      coEvery { syncInitialDataUseCase() } returns Result.success(Unit)
      coEvery { toggleAllegianceUseCase("362") } returnsMany listOf(
        Result.success(Unit),
        Result.failure(IllegalStateException("Erro no banco")),
      )

      val viewModel = createViewModel()
      val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.collect {}
      }

      advanceUntilIdle()
      viewModel.onToggleAllegiance("362")
      advanceUntilIdle()
      assertNull((viewModel.uiState.value as HousesUiState.Success).nonBlockingError)

      viewModel.onToggleAllegiance("362")
      advanceUntilIdle()
      assertNotNull((viewModel.uiState.value as HousesUiState.Success).nonBlockingError)
      coVerify(exactly = 2) { toggleAllegianceUseCase("362") }

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

  private fun sampleHouse(
    id: String,
    name: String,
    region: String = "The North",
  ): House {
    return House(
      id = id,
      name = name,
      region = region,
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
