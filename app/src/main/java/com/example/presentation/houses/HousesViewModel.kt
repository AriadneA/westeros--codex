package com.example.presentation.houses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.usecase.GetAvailableRegionsUseCase
import com.example.domain.usecase.GetHousesUseCase
import com.example.domain.usecase.SyncHousesPageUseCase
import com.example.domain.usecase.SyncInitialDataUseCase
import com.example.domain.usecase.ToggleAllegianceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel responsável por gerenciar a tela de listagem e filtragem das Casas de Westeros,
 * combinando o fluxo reativo do SSOT local com sincronização paginada sob demanda.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HousesViewModel
@Inject
constructor(
  private val getHousesUseCase: GetHousesUseCase,
  private val getAvailableRegionsUseCase: GetAvailableRegionsUseCase,
  private val syncInitialDataUseCase: SyncInitialDataUseCase,
  private val syncHousesPageUseCase: SyncHousesPageUseCase,
  private val toggleAllegianceUseCase: ToggleAllegianceUseCase,
) : ViewModel() {

  private data class FilterParams(
    val query: String,
    val region: String?,
    val onlySworn: Boolean,
  ) {
    fun hasActiveFilters(): Boolean =
      query.isNotBlank() || !region.isNullOrBlank() || onlySworn
  }

  private data class SyncStatus(
    val isInitialSyncing: Boolean = false,
    val isRefreshing: Boolean = false,
    val isPaginating: Boolean = false,
    val hasMorePages: Boolean = true,
    val currentPage: Int = 1,
    val initialError: String? = null,
    val nonBlockingError: String? = null,
  )

  private val searchQuery = MutableStateFlow("")
  private val selectedRegion = MutableStateFlow<String?>(null)
  private val onlySwornFilter = MutableStateFlow(false)
  private val syncStatus = MutableStateFlow(SyncStatus())

  private val filterParamsFlow =
    combine(searchQuery, selectedRegion, onlySwornFilter) { query, region, onlySworn ->
        FilterParams(
          query = query,
          region = region,
          onlySworn = onlySworn,
        )
      }
      .distinctUntilChanged()

  private val housesFlow =
    filterParamsFlow.flatMapLatest { filters ->
      getHousesUseCase(
        query = filters.query,
        region = filters.region,
        onlySworn = filters.onlySworn,
      )
    }

  val uiState: StateFlow<HousesUiState> =
    combine(
        housesFlow,
        getAvailableRegionsUseCase(),
        filterParamsFlow,
        syncStatus,
      ) { houses, regions, filters, sync ->
        if (houses.isEmpty()) {
          when {
            sync.isInitialSyncing && !filters.hasActiveFilters() -> HousesUiState.Loading
            sync.initialError != null && !filters.hasActiveFilters() ->
              HousesUiState.Error(message = sync.initialError)
            else ->
              HousesUiState.Empty(
                reason = resolveEmptyReason(filters),
                searchQuery = filters.query,
                selectedRegion = filters.region,
                onlySwornFilter = filters.onlySworn,
                availableRegions = regions,
              )
          }
        } else {
          HousesUiState.Success(
            houses = houses.map { it.toUiModel() },
            selectedRegion = filters.region,
            availableRegions = regions,
            searchQuery = filters.query,
            onlySwornFilter = filters.onlySworn,
            isRefreshing = sync.isRefreshing,
            isPaginating = sync.isPaginating,
            hasMorePages = sync.hasMorePages,
            nonBlockingError = sync.nonBlockingError,
          )
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HousesUiState.Loading,
      )

  init {
    triggerInitialSync()
  }

  fun onSearchQueryChange(query: String) {
    searchQuery.value = query
  }

  fun onRegionSelected(region: String?) {
    selectedRegion.value = region?.takeIf { it.isNotBlank() }
  }

  fun onOnlySwornChanged(onlySworn: Boolean) {
    onlySwornFilter.value = onlySworn
  }

  fun onClearFilters() {
    searchQuery.value = ""
    selectedRegion.value = null
    onlySwornFilter.value = false
  }

  fun onRetry() {
    triggerInitialSync()
  }

  fun onRefresh() {
    if (syncStatus.value.isRefreshing || syncStatus.value.isInitialSyncing) return
    viewModelScope.launch {
      syncStatus.update { it.copy(isRefreshing = true, nonBlockingError = null) }
      val result = syncHousesPageUseCase(page = 1)
      syncStatus.update { current ->
        result.fold(
          onSuccess = { hasMore ->
            current.copy(
              isRefreshing = false,
              initialError = null,
              hasMorePages = current.hasMorePages || hasMore,
            )
          },
          onFailure = { error ->
            current.copy(
              isRefreshing = false,
              nonBlockingError = formatNetworkError(error),
            )
          },
        )
      }
    }
  }

  fun onLoadNextPage() {
    val currentSync = syncStatus.value
    val hasActiveFilter =
      searchQuery.value.isNotBlank() ||
        !selectedRegion.value.isNullOrBlank() ||
        onlySwornFilter.value
    if (
      hasActiveFilter ||
        currentSync.isInitialSyncing ||
        currentSync.isRefreshing ||
        currentSync.isPaginating ||
        !currentSync.hasMorePages
    ) {
      return
    }

    val nextPage = currentSync.currentPage + 1
    viewModelScope.launch {
      syncStatus.update { it.copy(isPaginating = true, nonBlockingError = null) }
      val result = syncHousesPageUseCase(page = nextPage)
      syncStatus.update { current ->
        result.fold(
          onSuccess = { hasMore ->
            current.copy(
              isPaginating = false,
              currentPage = nextPage,
              hasMorePages = hasMore,
            )
          },
          onFailure = { error ->
            current.copy(
              isPaginating = false,
              nonBlockingError = formatNetworkError(error),
            )
          },
        )
      }
    }
  }

  fun onToggleAllegiance(houseId: String) {
    viewModelScope.launch {
      val result = toggleAllegianceUseCase(houseId)
      if (result.isFailure) {
        syncStatus.update {
          it.copy(nonBlockingError = "Não foi possível atualizar o juramento de lealdade.")
        }
      }
    }
  }

  fun onDismissError() {
    syncStatus.update { it.copy(nonBlockingError = null) }
  }

  private fun triggerInitialSync() {
    if (syncStatus.value.isInitialSyncing) return
    viewModelScope.launch {
      syncStatus.update {
        it.copy(
          isInitialSyncing = true,
          initialError = null,
          nonBlockingError = null,
        )
      }
      val result = syncInitialDataUseCase()
      syncStatus.update { current ->
        result.fold(
          onSuccess = {
            current.copy(
              isInitialSyncing = false,
              initialError = null,
            )
          },
          onFailure = { error ->
            val message = formatNetworkError(error)
            current.copy(
              isInitialSyncing = false,
              initialError = message,
              nonBlockingError = message,
            )
          },
        )
      }
    }
  }

  private fun resolveEmptyReason(filters: FilterParams): EmptyReason {
    return when {
      filters.onlySworn && filters.query.isBlank() && filters.region.isNullOrBlank() ->
        EmptyReason.NO_SWORN_HOUSES
      filters.hasActiveFilters() -> EmptyReason.NO_SEARCH_RESULTS
      else -> EmptyReason.EMPTY_CATALOG
    }
  }

  private fun formatNetworkError(error: Throwable): String {
    val detail = error.message?.takeIf { it.isNotBlank() }
    return if (detail != null) {
      "Não foi possível contactar a Cidadela. Verifique sua conexão e tente novamente."
    } else {
      "Falha de comunicação com os arquivos da Cidadela."
    }
  }
}
