package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val CitadelDarkColorScheme =
  darkColorScheme(
    primary = AntiqueGold,
    onPrimary = ObsidianBackground,
    primaryContainer = DarkGoldContainer,
    onPrimaryContainer = ParchmentGold,
    secondary = RoyalCrimson,
    onSecondary = ParchmentWhite,
    secondaryContainer = DarkCrimsonContainer,
    onSecondaryContainer = CrimsonHighlight,
    tertiary = ParchmentGold,
    onTertiary = ObsidianBackground,
    background = ObsidianBackground,
    onBackground = ParchmentWhite,
    surface = SlateSurface,
    onSurface = ParchmentWhite,
    surfaceVariant = IronSurfaceVariant,
    onSurfaceVariant = SteelGrey,
    surfaceContainerLowest = ObsidianBackground,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = SlateSurface,
    surfaceContainerHigh = IronSurfaceVariant,
    surfaceContainerHighest = IronOutline,
    outline = IronOutline,
    outlineVariant = IronSurfaceVariant,
    error = ErrorCrimson,
    onError = ObsidianBackground,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ParchmentWhite,
  )

private val WesterosShapes =
  Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
  )

@Composable
fun WesterosCodexTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = CitadelDarkColorScheme,
    typography = WesterosTypography,
    shapes = WesterosShapes,
    content = content,
  )
}
