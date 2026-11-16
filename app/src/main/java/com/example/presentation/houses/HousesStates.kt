package com.example.presentation.houses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
internal fun HousesLoadingState() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp)
      .testTag("houses_loading_state"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      CircularProgressIndicator(
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 3.dp,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = stringResource(R.string.loading_archives),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
internal fun HousesErrorState(
  message: String,
  onRetry: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp)
      .testTag("houses_error_state"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = Icons.Outlined.CloudOff,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(56.dp),
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = stringResource(R.string.error_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = onRetry,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("houses_retry_button"),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
      ) {
        Text(text = stringResource(R.string.retry_button))
      }
    }
  }
}

@Composable
internal fun HousesEmptyState(
  reason: EmptyReason,
  onClearFilters: () -> Unit,
  onExploreHouses: () -> Unit,
  onRetry: () -> Unit,
) {
  val titleRes =
    when (reason) {
      EmptyReason.NO_SWORN_HOUSES -> R.string.empty_sworn_title
      EmptyReason.NO_SEARCH_RESULTS -> R.string.empty_search_title
      EmptyReason.EMPTY_CATALOG -> R.string.empty_catalog_title
    }
  val messageRes =
    when (reason) {
      EmptyReason.NO_SWORN_HOUSES -> R.string.empty_sworn_message
      EmptyReason.NO_SEARCH_RESULTS -> R.string.empty_search_message
      EmptyReason.EMPTY_CATALOG -> R.string.empty_catalog_message
    }
  val actionLabelRes =
    when (reason) {
      EmptyReason.NO_SWORN_HOUSES -> R.string.explore_houses_button
      EmptyReason.NO_SEARCH_RESULTS -> R.string.clear_filters_button
      EmptyReason.EMPTY_CATALOG -> R.string.retry_button
    }
  val action =
    when (reason) {
      EmptyReason.NO_SWORN_HOUSES -> onExploreHouses
      EmptyReason.NO_SEARCH_RESULTS -> onClearFilters
      EmptyReason.EMPTY_CATALOG -> onRetry
    }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp)
      .testTag("houses_empty_state"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector =
          if (reason == EmptyReason.NO_SWORN_HOUSES) {
            Icons.Outlined.Shield
          } else {
            Icons.Outlined.MenuBook
          },
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(56.dp),
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = stringResource(messageRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(20.dp))
      OutlinedButton(
        onClick = action,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("houses_empty_action_button"),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = MaterialTheme.colorScheme.primary,
        ),
      ) {
        Text(text = stringResource(actionLabelRes))
      }
    }
  }
}

@Composable
internal fun WesterosBottomNavigationBar(
  onlySwornSelected: Boolean,
  onOnlySwornChanged: (Boolean) -> Unit,
) {
  NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
    windowInsets = WindowInsets.navigationBars,
  ) {
    NavigationBarItem(
      selected = !onlySwornSelected,
      onClick = { onOnlySwornChanged(false) },
      modifier = Modifier.testTag("nav_explore"),
      icon = {
        Icon(
          imageVector = if (!onlySwornSelected) Icons.Filled.Explore else Icons.Outlined.Explore,
          contentDescription = null,
        )
      },
      label = { Text(text = stringResource(R.string.nav_explore)) },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
      ),
    )

    NavigationBarItem(
      selected = onlySwornSelected,
      onClick = { onOnlySwornChanged(true) },
      modifier = Modifier.testTag("nav_sworn"),
      icon = {
        Icon(
          imageVector = if (onlySwornSelected) Icons.Filled.Shield else Icons.Outlined.Shield,
          contentDescription = null,
        )
      },
      label = { Text(text = stringResource(R.string.nav_sworn)) },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
      ),
    )
  }
}
