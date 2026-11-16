package com.example.presentation.houses

import androidx.compose.runtime.Immutable
import com.example.domain.model.House

/**
 * Representa os 4 estados explícitos da tela de exploração das Casas de Westeros.
 */
@Immutable
sealed interface HousesUiState {

  data object Loading : HousesUiState

  @Immutable
  data class Success(
    val houses: List<HouseUiModel>,
    val selectedRegion: String?,
    val availableRegions: List<String>,
    val searchQuery: String,
    val onlySwornFilter: Boolean,
    val isRefreshing: Boolean = false,
    val isPaginating: Boolean = false,
    val hasMorePages: Boolean = true,
    val nonBlockingError: String? = null,
  ) : HousesUiState

  @Immutable
  data class Empty(
    val reason: EmptyReason,
    val searchQuery: String,
    val selectedRegion: String?,
    val onlySwornFilter: Boolean,
    val availableRegions: List<String>,
  ) : HousesUiState

  @Immutable
  data class Error(
    val message: String,
  ) : HousesUiState
}

enum class EmptyReason {
  NO_SEARCH_RESULTS,
  NO_SWORN_HOUSES,
  EMPTY_CATALOG,
}

@Immutable
data class HouseUiModel(
  val id: String,
  val name: String,
  val region: String,
  val words: String,
  val hasWords: Boolean,
  val coatOfArms: String,
  val hasCoatOfArms: Boolean,
  val titles: List<String>,
  val seats: List<String>,
  val ancestralWeapons: List<String>,
  val isSworn: Boolean,
  val notesCount: Int,
)

fun House.toUiModel(): HouseUiModel {
  return HouseUiModel(
    id = id,
    name = name,
    region = region,
    words = words,
    hasWords = words.isNotBlank(),
    coatOfArms = coatOfArms,
    hasCoatOfArms = coatOfArms.isNotBlank(),
    titles = titles,
    seats = seats,
    ancestralWeapons = ancestralWeapons,
    isSworn = isSworn,
    notesCount = notesCount,
  )
}
