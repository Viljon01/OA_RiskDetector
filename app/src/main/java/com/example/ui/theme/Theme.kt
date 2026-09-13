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

private val LightColorScheme =
  lightColorScheme(
      primary = PrimaryTeal,
      background = BackgroundLight,
      surface = SurfaceWhite,
      surfaceVariant = BackgroundLight,
      onPrimary = Color.White,
      onBackground = OnSurfaceText,
      onSurface = OnSurfaceText,
      onSurfaceVariant = OnSurfaceVariantText
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme // Always use light theme per requirements

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
