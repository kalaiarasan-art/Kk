package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BotanicalLightColorScheme = lightColorScheme(
  primary = TerracottaPrimary,
  onPrimary = Color.White,
  primaryContainer = TerracottaContainer,
  onPrimaryContainer = OnTerracottaContainer,
  secondary = SageSecondary,
  onSecondary = Color.White,
  secondaryContainer = SageContainer,
  onSecondaryContainer = OnSageContainer,
  tertiary = OchreTertiary,
  onTertiary = EspressoInk,
  tertiaryContainer = OchreContainer,
  onTertiaryContainer = OnOchreContainer,
  background = ParchmentBackground,
  onBackground = EspressoInk,
  surface = LinenSurface,
  onSurface = EspressoInk,
  surfaceVariant = LinenSurfaceVariant,
  onSurfaceVariant = MutedBark,
  outline = OutlineSand,
  error = CrimsonError,
  onError = Color.White,
)

private val BotanicalDarkColorScheme = darkColorScheme(
  primary = TerracottaLight,
  onPrimary = OnTerracottaContainer,
  primaryContainer = TerracottaDarkContainer,
  onPrimaryContainer = TerracottaContainer,
  secondary = SageLight,
  onSecondary = OnSageContainer,
  secondaryContainer = SageDarkContainer,
  onSecondaryContainer = SageContainer,
  tertiary = OchreLight,
  onTertiary = OnOchreContainer,
  background = DarkForestBackground,
  onBackground = ParchmentInkDark,
  surface = DarkMossSurface,
  onSurface = ParchmentInkDark,
  surfaceVariant = DarkMossSurfaceVariant,
  onSurfaceVariant = OutlineSand,
  outline = MutedBark,
  error = CrimsonError,
  onError = Color.White,
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  highContrast: Boolean = false,
  content: @Composable () -> Unit
) {
  val baseScheme = if (darkTheme) BotanicalDarkColorScheme else BotanicalLightColorScheme
  val colorScheme = if (highContrast) {
    baseScheme.copy(
      background = if (darkTheme) Color.Black else Color.White,
      surface = if (darkTheme) Color(0xFF121212) else Color(0xFFF5F5F5),
      onBackground = if (darkTheme) Color.White else Color.Black,
      onSurface = if (darkTheme) Color.White else Color.Black,
    )
  } else {
    baseScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
