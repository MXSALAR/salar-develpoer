package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode {
  LIGHT,
  DARK,
  LATE_NIGHT_OLED
}

private val LightColorScheme = lightColorScheme(
  primary = MedicalTealPrimary,
  onPrimary = MedicalTealOnPrimary,
  primaryContainer = MedicalTealContainer,
  onPrimaryContainer = MedicalTealOnContainer,
  secondary = MedicalNavySecondary,
  onSecondary = MedicalNavyOnSecondary,
  secondaryContainer = MedicalNavyContainer,
  onSecondaryContainer = MedicalNavyOnContainer,
  tertiary = MedicalAmberTertiary,
  onTertiary = MedicalAmberOnTertiary,
  tertiaryContainer = MedicalAmberContainer,
  onTertiaryContainer = MedicalAmberOnContainer,
  background = LightBackground,
  onBackground = Color(0xFF101C24),
  surface = LightSurface,
  onSurface = Color(0xFF101C24),
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = Color(0xFF404E5A),
  outline = LightOutline
)

private val DarkColorScheme = darkColorScheme(
  primary = DarkTealPrimary,
  onPrimary = DarkTealOnPrimary,
  primaryContainer = DarkTealContainer,
  onPrimaryContainer = DarkTealOnContainer,
  secondary = DarkNavySecondary,
  onSecondary = DarkNavyOnSecondary,
  secondaryContainer = DarkNavyContainer,
  onSecondaryContainer = DarkNavyOnContainer,
  tertiary = DarkAmberTertiary,
  onTertiary = DarkAmberOnTertiary,
  tertiaryContainer = DarkAmberContainer,
  onTertiaryContainer = DarkAmberOnContainer,
  background = DarkBackground,
  onBackground = Color(0xFFE2EDF8),
  surface = DarkSurface,
  onSurface = Color(0xFFE2EDF8),
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = Color(0xFFADC4DE),
  outline = DarkOutline
)

private val OledColorScheme = darkColorScheme(
  primary = DarkTealPrimary,
  onPrimary = Color(0xFF000000),
  primaryContainer = Color(0xFF07271F),
  onPrimaryContainer = DarkTealPrimary,
  secondary = DarkNavySecondary,
  onSecondary = Color(0xFF000000),
  secondaryContainer = Color(0xFF0F1E33),
  onSecondaryContainer = DarkNavySecondary,
  tertiary = DarkAmberTertiary,
  onTertiary = Color(0xFF000000),
  tertiaryContainer = Color(0xFF331D00),
  onTertiaryContainer = DarkAmberTertiary,
  background = OledBackground,
  onBackground = Color(0xFFE6EEF8),
  surface = OledSurface,
  onSurface = Color(0xFFE6EEF8),
  surfaceVariant = OledSurfaceVariant,
  onSurfaceVariant = Color(0xFF8FA8C4),
  outline = OledOutline
)

@Composable
fun MDCATTheme(
  themeMode: AppThemeMode = AppThemeMode.DARK,
  content: @Composable () -> Unit
) {
  val colorScheme: ColorScheme = when (themeMode) {
    AppThemeMode.LIGHT -> LightColorScheme
    AppThemeMode.DARK -> DarkColorScheme
    AppThemeMode.LATE_NIGHT_OLED -> OledColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
