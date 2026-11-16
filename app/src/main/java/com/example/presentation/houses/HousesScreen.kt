package com.example.presentation.houses

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R

@Composable
fun HousesRoute(
  onHouseClick: (HouseUiModel) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: HousesViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  HousesScreen(
    uiState = uiState,
    onSearchQueryChange = viewModel::onSearchQueryChange,
    onRegionSelected = viewModel::onRegionSelected,
    onOnlySwornChanged = viewModel::onOnlySwornChanged,
    onClearFilters = viewModel::onClearFilters,
    onLoadNextPage = viewModel::onLoadNextPage,
    onRefresh = viewModel::onRefresh,
    onRetry = viewModel::onRetry,
    onToggleAllegiance = viewModel::onToggleAllegiance,
    onHouseClick = onHouseClick,
    onDismissError = viewModel::onDismissError,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HousesScreen(
  uiState: HousesUiState,
  onSearchQueryChange: (String) -> Unit,
  onRegionSelected: (String?) -> Unit,
  onOnlySwornChanged: (Boolean) -> Unit,
  onClearFilters: () -> Unit,
  onLoadNextPage: () -> Unit,
  onRefresh: () -> Unit,
  onRetry: () -> Unit,
  onToggleAllegiance: (String) -> Unit,
  onHouseClick: (HouseUiModel) -> Unit,
  onDismissError: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val listState = rememberLazyListState()

  val nonBlockingError = (uiState as? HousesUiState.Success)?.nonBlockingError
  LaunchedEffect(nonBlockingError) {
    if (!nonBlockingError.isNullOrBlank()) {
      snackbarHostState.showSnackbar(message = nonBlockingError)
      onDismissError()
    }
  }

  val isSwornTabSelected =
    when (uiState) {
      is HousesUiState.Success -> uiState.onlySwornFilter
      is HousesUiState.Empty -> uiState.onlySwornFilter
      else -> false
    }

  val searchQuery =
    when (uiState) {
      is HousesUiState.Success -> uiState.searchQuery
      is HousesUiState.Empty -> uiState.searchQuery
      else -> ""
    }

  val selectedRegion =
    when (uiState) {
      is HousesUiState.Success -> uiState.selectedRegion
      is HousesUiState.Empty -> uiState.selectedRegion
      else -> null
    }

  val availableRegions =
    when (uiState) {
      is HousesUiState.Success -> uiState.availableRegions
      is HousesUiState.Empty -> uiState.availableRegions
      else -> emptyList()
    }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    bottomBar = {
      WesterosBottomNavigationBar(
        onlySwornSelected = isSwornTabSelected,
        onOnlySwornChanged = onOnlySwornChanged,
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 680.dp),
      ) {
        CitadelHeaderBanner()

        if (uiState is HousesUiState.Success || uiState is HousesUiState.Empty) {
          SearchAndRegionFilters(
            searchQuery = searchQuery,
            selectedRegion = selectedRegion,
            availableRegions = availableRegions,
            onSearchQueryChange = onSearchQueryChange,
            onRegionSelected = onRegionSelected,
          )
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        ) {
          when (uiState) {
            is HousesUiState.Loading -> HousesLoadingState()
            is HousesUiState.Error ->
              HousesErrorState(
                message = uiState.message,
                onRetry = onRetry,
              )
            is HousesUiState.Empty ->
              HousesEmptyState(
                reason = uiState.reason,
                onClearFilters = onClearFilters,
                onExploreHouses = { onOnlySwornChanged(false) },
                onRetry = onRetry,
              )
            is HousesUiState.Success ->
              HousesSuccessContent(
                state = uiState,
                listState = listState,
                onRefresh = onRefresh,
                onLoadNextPage = onLoadNextPage,
                onToggleAllegiance = onToggleAllegiance,
                onHouseClick = onHouseClick,
              )
          }
        }
      }
    }
  }
}

@Composable
private fun CitadelHeaderBanner() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(148.dp),
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_citadel_banner),
      contentDescription = stringResource(R.string.codex_banner_description),
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
              MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
              MaterialTheme.colorScheme.background,
            ),
          ),
        ),
    )
    Column(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
      Text(
        text = stringResource(R.string.codex_header_subtitle).uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
    }
  }
}

@Composable
private fun SearchAndRegionFilters(
  searchQuery: String,
  selectedRegion: String?,
  availableRegions: List<String>,
  onSearchQueryChange: (String) -> Unit,
  onRegionSelected: (String?) -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp),
  ) {
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchQueryChange,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .testTag("houses_search_input"),
      placeholder = {
        Text(
          text = stringResource(R.string.search_placeholder),
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      leadingIcon = {
        Icon(
          imageVector = Icons.Filled.Search,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
      },
      trailingIcon = {
        AnimatedVisibility(
          visible = searchQuery.isNotEmpty(),
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          IconButton(
            onClick = { onSearchQueryChange("") },
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("houses_search_clear_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Clear,
              contentDescription = stringResource(R.string.search_clear_description),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      },
      singleLine = true,
      shape = MaterialTheme.shapes.medium,
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
      ),
    )

    if (availableRegions.isNotEmpty()) {
      Spacer(modifier = Modifier.height(10.dp))
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        item(key = "region_all") {
          val selected = selectedRegion == null
          FilterChip(
            selected = selected,
            onClick = { onRegionSelected(null) },
            label = { Text(text = stringResource(R.string.filter_all_regions)) },
            modifier = Modifier.testTag("region_chip_all"),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
              containerColor = MaterialTheme.colorScheme.surface,
              labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = selected,
              borderColor = MaterialTheme.colorScheme.outline,
              selectedBorderColor = MaterialTheme.colorScheme.primary,
            ),
          )
        }

        items(
          items = availableRegions,
          key = { region -> "region_$region" },
        ) { region ->
          val selected = selectedRegion == region
          FilterChip(
            selected = selected,
            onClick = { onRegionSelected(if (selected) null else region) },
            label = { Text(text = region) },
            modifier = Modifier.testTag("region_chip_$region"),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
              containerColor = MaterialTheme.colorScheme.surface,
              labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = selected,
              borderColor = MaterialTheme.colorScheme.outline,
              selectedBorderColor = MaterialTheme.colorScheme.primary,
            ),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HousesSuccessContent(
  state: HousesUiState.Success,
  listState: LazyListState,
  onRefresh: () -> Unit,
  onLoadNextPage: () -> Unit,
  onToggleAllegiance: (String) -> Unit,
  onHouseClick: (HouseUiModel) -> Unit,
) {
  val shouldLoadMore by remember(state.houses.size, state.hasMorePages, state.isPaginating) {
    derivedStateOf {
      val layoutInfo = listState.layoutInfo
      val totalItems = layoutInfo.totalItemsCount
      val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
      state.hasMorePages && !state.isPaginating && totalItems > 0 && lastVisibleIndex >= totalItems - 3
    }
  }

  LaunchedEffect(shouldLoadMore) {
    if (shouldLoadMore) {
      onLoadNextPage()
    }
  }

  PullToRefreshBox(
    isRefreshing = state.isRefreshing,
    onRefresh = onRefresh,
    modifier = Modifier
      .fillMaxSize()
      .testTag("houses_pull_to_refresh"),
  ) {
    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .testTag("houses_lazy_column"),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(
        items = state.houses,
        key = { house -> house.id },
        contentType = { "house_card" },
      ) { house ->
        HouseCard(
          house = house,
          onToggleAllegiance = { onToggleAllegiance(house.id) },
          onClick = { onHouseClick(house) },
        )
      }

      if (state.isPaginating) {
        item(
          key = "pagination_footer",
          contentType = "pagination_loader",
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 16.dp)
              .testTag("houses_pagination_loading"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              strokeWidth = 2.5.dp,
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = stringResource(R.string.loading_more_houses),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
