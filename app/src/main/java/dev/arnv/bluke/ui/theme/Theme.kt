package dev.arnv.bluke.ui.theme

import androidx.compose.ui.platform.LocalView
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Derives a coherent scheme from one accent so cards, chips and surfaces share its hue. */
private fun accentScheme(accent: Color, dark: Boolean, vibrant: Boolean, trueBlack: Boolean): ColorScheme {
  val muted = lerp(accent, Color(0xFF8A8F98), if (vibrant) 0.15f else 0.55f)
  return if (dark) {
    val base = if (trueBlack) Color.Black else Color(0xFF111318)
    val tone = { amount: Float -> if (trueBlack && amount < 0.05f) Color.Black else lerp(base, accent, amount) }
    darkColorScheme(
      primary = lerp(accent, Color.White, 0.45f),
      onPrimary = lerp(accent, Color.Black, 0.65f),
      primaryContainer = lerp(base, accent, 0.45f),
      onPrimaryContainer = lerp(accent, Color.White, 0.85f),
      secondary = lerp(muted, Color.White, 0.5f),
      onSecondary = lerp(muted, Color.Black, 0.7f),
      secondaryContainer = lerp(base, muted, 0.35f),
      onSecondaryContainer = lerp(muted, Color.White, 0.85f),
      tertiary = Color(0xFF7DD3C0),
      onTertiary = Color(0xFF00382F),
      tertiaryContainer = Color(0xFF1F4F46),
      onTertiaryContainer = Color(0xFFB6F2E3),
      background = tone(0.03f),
      onBackground = Color(0xFFE3E4EA),
      surface = tone(0.03f),
      onSurface = Color(0xFFE3E4EA),
      surfaceVariant = tone(0.16f),
      onSurfaceVariant = Color(0xFFC1C5CF),
      surfaceContainerLowest = base,
      surfaceContainerLow = tone(0.06f),
      surfaceContainer = tone(0.09f),
      surfaceContainerHigh = tone(0.12f),
      surfaceContainerHighest = tone(0.16f),
      outline = Color(0xFF8B909A),
      outlineVariant = tone(0.25f),
    )
  } else {
    val base = Color(0xFFFBFBFE)
    lightColorScheme(
      primary = accent,
      onPrimary = Color.White,
      primaryContainer = lerp(base, accent, 0.18f),
      onPrimaryContainer = lerp(accent, Color.Black, 0.55f),
      secondary = muted,
      onSecondary = Color.White,
      secondaryContainer = lerp(base, muted, 0.16f),
      onSecondaryContainer = lerp(muted, Color.Black, 0.6f),
      tertiary = Color(0xFF006B5B),
      onTertiary = Color.White,
      tertiaryContainer = Color(0xFFB6F2E3),
      onTertiaryContainer = Color(0xFF00201A),
      background = lerp(base, accent, 0.02f),
      onBackground = Color(0xFF1A1C21),
      surface = lerp(base, accent, 0.02f),
      onSurface = Color(0xFF1A1C21),
      surfaceVariant = lerp(base, accent, 0.10f),
      onSurfaceVariant = Color(0xFF44474F),
      surfaceContainerLowest = Color.White,
      surfaceContainerLow = lerp(base, accent, 0.04f),
      surfaceContainer = lerp(base, accent, 0.06f),
      surfaceContainerHigh = lerp(base, accent, 0.085f),
      surfaceContainerHighest = lerp(base, accent, 0.11f),
      outline = Color(0xFF757980),
      outlineVariant = lerp(base, accent, 0.2f),
    )
  }
}

private val BlukeShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val sharedPrefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
  
  val isDynamicColorDefault = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
  var dynamicColor by androidx.compose.runtime.remember { 
      androidx.compose.runtime.mutableStateOf(sharedPrefs.getBoolean("dynamic_color", isDynamicColorDefault)) 
  }
  var accentColorIndex by androidx.compose.runtime.remember {
      androidx.compose.runtime.mutableIntStateOf(sharedPrefs.getInt("accent_color_index", 0))
  }
  var themeMode by androidx.compose.runtime.remember {
      androidx.compose.runtime.mutableIntStateOf(sharedPrefs.getInt("theme_mode", 0))
  }
  var highContrastMode by androidx.compose.runtime.remember {
      androidx.compose.runtime.mutableStateOf(sharedPrefs.getBoolean("high_contrast_mode", false))
  }
  var paletteStyle by androidx.compose.runtime.remember {
      androidx.compose.runtime.mutableStateOf(sharedPrefs.getString("palette_style", "Tonal Spot") ?: "Tonal Spot")
  }
  
  // Listen for changes
  androidx.compose.runtime.DisposableEffect(context) {
      val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
          when (key) {
              "dynamic_color" -> dynamicColor = prefs.getBoolean("dynamic_color", isDynamicColorDefault)
              "accent_color_index" -> accentColorIndex = prefs.getInt("accent_color_index", 0)
              "theme_mode" -> themeMode = prefs.getInt("theme_mode", 0)
              "high_contrast_mode" -> highContrastMode = prefs.getBoolean("high_contrast_mode", false)
              "palette_style" -> paletteStyle = prefs.getString("palette_style", "Tonal Spot") ?: "Tonal Spot"
          }
      }
      sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
      onDispose { sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
  }

  val isMonochrome = paletteStyle == "Monochrome"
  val isVibrant = paletteStyle == "Vibrant"

  val baseColor = if (isMonochrome) {
      if (themeMode == 2 || (themeMode == 0 && darkTheme)) Color(0xFF6E6E6E) else Color(0xFF424242)
  } else {
      AccentColors.getOrElse(accentColorIndex) { AccentColors.first() }
  }

  val useDarkTheme = when (themeMode) {
      1 -> false // Off
      2 -> true  // On
      else -> darkTheme // System
  }

  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val baseScheme = if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        if (useDarkTheme && highContrastMode) {
            baseScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color(0xFF0E0E10),
                surfaceContainer = Color(0xFF141416),
                surfaceContainerHigh = Color(0xFF1C1C1E),
                surfaceContainerHighest = Color(0xFF262628),
                surfaceVariant = Color(0xFF1C1C1E),
            )
        } else baseScheme
      }
      else -> accentScheme(baseColor, useDarkTheme, vibrant = isVibrant, trueBlack = useDarkTheme && highContrastMode)
    }

  // Settings screens are edge-to-edge on Android 15+ without enableEdgeToEdge(), so match bar icons to the theme here.
  val view = LocalView.current
  if (!view.isInEditMode) {
    androidx.compose.runtime.SideEffect {
      val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
      val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
      controller.isAppearanceLightStatusBars = !useDarkTheme
      controller.isAppearanceLightNavigationBars = !useDarkTheme
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, shapes = BlukeShapes, content = content)
}
