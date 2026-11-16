package com.example.presentation.houses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
internal fun HouseCard(
  house: HouseUiModel,
  onToggleAllegiance: () -> Unit,
  onClick: () -> Unit,
) {
  val borderColor =
    if (house.isSworn) {
      MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
    } else {
      MaterialTheme.colorScheme.outline
    }

  OutlinedCard(
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("house_card_${house.id}"),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.outlinedCardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
    border = BorderStroke(width = 1.dp, color = borderColor),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = MaterialTheme.shapes.small,
            ) {
              Text(
                text = house.region.ifBlank { stringResource(R.string.region_unknown) },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }

            if (house.isSworn) {
              Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
              ) {
                Text(
                  text = stringResource(R.string.sworn_badge_label),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = house.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }

        IconButton(
          onClick = onToggleAllegiance,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("allegiance_button_${house.id}"),
        ) {
          Icon(
            imageVector = if (house.isSworn) Icons.Filled.Shield else Icons.Outlined.Shield,
            contentDescription =
              stringResource(
                if (house.isSworn) {
                  R.string.revoke_loyalty_description
                } else {
                  R.string.pledge_loyalty_description
                },
              ),
            tint =
              if (house.isSworn) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text =
          if (house.hasWords) {
            "“${house.words}”"
          } else {
            stringResource(R.string.words_missing)
          },
        style = MaterialTheme.typography.bodyMedium.copy(
          fontStyle = FontStyle.Italic,
        ),
        color =
          if (house.hasWords) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          },
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text =
            if (house.hasCoatOfArms) {
              house.coatOfArms
            } else {
              stringResource(R.string.coat_of_arms_missing)
            },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )

        if (house.notesCount > 0) {
          Spacer(modifier = Modifier.width(12.dp))
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text =
                  if (house.notesCount == 1) {
                    stringResource(R.string.notes_badge_singular)
                  } else {
                    stringResource(R.string.notes_badge_format, house.notesCount)
                  },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }
      }
    }
  }
}
