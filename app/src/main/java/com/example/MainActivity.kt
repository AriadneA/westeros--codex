package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.houses.HouseUiModel
import com.example.presentation.houses.HousesScreen
import com.example.presentation.houses.HousesUiState
import com.example.presentation.houses.HousesViewModel
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.WesterosCodexTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(ObsidianBackground.toArgb()),
      navigationBarStyle = SystemBarStyle.dark(ObsidianBackground.toArgb()),
    )
    setContent {
      WesterosCodexTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background,
        ) {
          val viewModel: HousesViewModel = hiltViewModel()
          val uiState by viewModel.uiState.collectAsStateWithLifecycle()
          var selectedHouseId by remember { mutableStateOf<String?>(null) }

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
            onHouseClick = { house -> selectedHouseId = house.id },
            onDismissError = viewModel::onDismissError,
          )

          val selectedHouse =
            remember(uiState, selectedHouseId) {
              val id = selectedHouseId ?: return@remember null
              (uiState as? HousesUiState.Success)?.houses?.firstOrNull { it.id == id }
            }

          if (selectedHouse != null) {
            ModalBottomSheet(
              onDismissRequest = { selectedHouseId = null },
              containerColor = MaterialTheme.colorScheme.surface,
              contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
              HouseQuickHeraldrySheet(
                house = selectedHouse,
                onToggleAllegiance = { viewModel.onToggleAllegiance(selectedHouse.id) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HouseQuickHeraldrySheet(
  house: HouseUiModel,
  onToggleAllegiance: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp, vertical = 12.dp)
      .padding(bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = house.region.ifBlank { stringResource(R.string.region_unknown) }.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = house.name,
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
      text =
        if (house.hasWords) {
          "“${house.words}”"
        } else {
          stringResource(R.string.words_missing)
        },
      style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
      color = MaterialTheme.colorScheme.primary,
    )

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

    HeraldrySection(
      label = stringResource(R.string.quick_sheet_coat_label),
      value =
        if (house.hasCoatOfArms) {
          house.coatOfArms
        } else {
          stringResource(R.string.coat_of_arms_missing)
        },
    )
    HeraldrySection(
      label = stringResource(R.string.quick_sheet_seats_label),
      value = house.seats.joinToString(", ").ifBlank { stringResource(R.string.quick_sheet_none) },
    )
    HeraldrySection(
      label = stringResource(R.string.quick_sheet_titles_label),
      value = house.titles.joinToString(" • ").ifBlank { stringResource(R.string.quick_sheet_none) },
    )
    HeraldrySection(
      label = stringResource(R.string.quick_sheet_weapons_label),
      value =
        house.ancestralWeapons.joinToString(", ").ifBlank {
          stringResource(R.string.quick_sheet_none)
        },
    )

    Spacer(modifier = Modifier.height(4.dp))

    Button(
      onClick = onToggleAllegiance,
      modifier = Modifier
        .fillMaxWidth()
        .minimumInteractiveComponentSize()
        .testTag("quick_sheet_pledge_button"),
      colors =
        ButtonDefaults.buttonColors(
          containerColor =
            if (house.isSworn) {
              MaterialTheme.colorScheme.secondary
            } else {
              MaterialTheme.colorScheme.primary
            },
          contentColor =
            if (house.isSworn) {
              MaterialTheme.colorScheme.onSecondary
            } else {
              MaterialTheme.colorScheme.onPrimary
            },
        ),
    ) {
      Icon(
        imageVector = if (house.isSworn) Icons.Filled.Shield else Icons.Outlined.Shield,
        contentDescription = null,
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text =
          stringResource(
            if (house.isSworn) R.string.quick_sheet_sworn else R.string.quick_sheet_pledge,
          ),
      )
    }
  }
}

@Composable
private fun HeraldrySection(
  label: String,
  value: String,
) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
