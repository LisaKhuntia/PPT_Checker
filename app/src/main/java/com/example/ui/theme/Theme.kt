package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = SleekPrimary,
    onPrimary = Color.White,
    secondary = SleekSecondaryContainer,
    onSecondary = SleekOnSecondaryContainer,
    background = Color(0xFF121214),
    surface = Color(0xFF1E1E24),
    onBackground = Color.White,
    onSurface = Color.White,
    error = SleekError,
    outline = SleekOutline,
    surfaceVariant = SleekSurfaceVariant,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SleekPrimary,
    onPrimary = Color.White,
    secondary = SleekSecondaryContainer,
    onSecondary = SleekOnSecondaryContainer,
    background = SleekBackground,
    surface = SleekSurface,
    onBackground = SleekText,
    onSurface = SleekText,
    error = SleekError,
    outline = SleekOutline,
    surfaceVariant = SleekSurfaceVariant,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to preserve custom sleek brand colors
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
