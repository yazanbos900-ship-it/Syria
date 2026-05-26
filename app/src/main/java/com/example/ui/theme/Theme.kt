package com.example.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode" // "light", "dark", "system"

    val isDarkState = mutableStateOf(false)
    val themeModeState = mutableStateOf("system") // "light", "dark", "system"

    val isDark: Boolean get() = isDarkState.value

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        themeModeState.value = mode
        updateDarkState(context, mode)
    }

    private fun updateDarkState(context: Context, mode: String) {
        isDarkState.value = when (mode) {
            "dark" -> true
            "light" -> false
            else -> {
                val isSystemDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                isSystemDark
            }
        }
    }

    fun setTheme(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        themeModeState.value = mode
        updateDarkState(context, mode)
    }
}

private val LightColorScheme = lightColorScheme(
  primary = BrandPrimary,
  onPrimary = BrandOnPrimary,
  secondary = BrandSecondary,
  tertiary = BrandTertiary,
  background = Color(0xFFF7F7F5), // BrandBackground is dynamic, so use static light value here
  onBackground = Color(0xFF111111),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF111111),
  surfaceVariant = Color(0xFFECECEC),
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
  darkTheme: Boolean = ThemeManager.isDark,
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

