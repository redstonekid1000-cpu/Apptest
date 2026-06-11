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
    primary = EditorialPurpleLight,
    onPrimary = EditorialPrimaryDark,
    secondary = EditorialPurpleCard,
    onSecondary = EditorialPrimaryDark,
    tertiary = Color.White,
    onTertiary = EditorialPrimaryDark,
    background = Color(0xFF1A1715),
    onBackground = Color(0xFFFFFDF9),
    surface = Color(0xFF231F1D),
    onSurface = Color(0xFFFFFDF9),
    surfaceVariant = Color(0xFF2D2723),
    onSurfaceVariant = EditorialPurpleLight,
    outline = Color(0xFF5C524A)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EditorialPrimaryDark, // walnut-espresso
    onPrimary = Color.White,
    primaryContainer = EditorialPurpleLight, // beautiful warm cream highlight
    onPrimaryContainer = EditorialPrimaryDark,
    secondary = EditorialPrimaryPurple, // sepia brown
    onSecondary = Color.White,
    secondaryContainer = EditorialPurpleCard, // warm linen base
    onSecondaryContainer = EditorialPrimaryDark,
    tertiary = EditorialPurpleLight,
    onTertiary = EditorialPrimaryDark,
    background = EditorialBackground, // soft ivory-cream base
    onBackground = EditorialTextDark, // carbon-charcoal
    surface = Color.White,
    onSurface = EditorialTextDark,
    surfaceVariant = EditorialPurpleCard,
    onSurfaceVariant = EditorialPrimaryDark,
    outline = EditorialBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
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
