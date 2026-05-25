package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
  primary = BrandPrimary,
  onPrimary = BrandOnPrimary,
  secondary = BrandSecondary,
  tertiary = BrandTertiary,
  background = BrandBackground,
  onBackground = BrandOnBackground,
  surface = BrandSurface,
  onSurface = BrandOnSurface,
  surfaceVariant = BrandSoftGray,
  error = BrandError
)

private val DarkColorScheme = darkColorScheme(
  primary = BrandPrimary,
  onPrimary = BrandOnPrimary,
  secondary = BrandSecondary,
  tertiary = BrandTertiary,
  background = Color(0xFF141413),
  onBackground = Color(0xFFECECEC),
  surface = Color(0xFF1E1E1D),
  onSurface = Color(0xFFECECEC),
  surfaceVariant = Color(0xFF2E2E2D),
  error = BrandError
)

@Composable
fun WasetPlusTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to preserve the luxury corporate identity
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

// Backward compatibility alias to prevent breaking existing tests/activities
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  WasetPlusTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

