package com.focusflow.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.focusflow.app.repository.ThemeMode

// Dark Mode Palette (#0B132B, #1C2541, #3A506B, #5BC0BE)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF5BC0BE),
    onPrimary = Color(0xFF0B132B),
    primaryContainer = Color(0xFF1C2541),
    onPrimaryContainer = Color(0xFF5BC0BE),
    secondary = Color(0xFF3A506B),
    secondaryContainer = Color(0xFF1C2541),
    onSecondaryContainer = Color(0xFF5BC0BE),
    tertiary = Color(0xFF6FFFE9),
    background = Color(0xFF0B132B),
    surface = Color(0xFF1C2541),
    surfaceVariant = Color(0xFF3A506B),
    onBackground = Color(0xFFE6F4F5),
    onSurface = Color(0xFFE6F4F5),
    onSurfaceVariant = Color(0xFFA5C4D4),
    outline = Color(0xFF3A506B)
)

// Light Mode Palette (#E3F2FD, #90CAF9, #2196F3, #0D47A1)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2196F3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFF1565C0),
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = Color(0xFF0D47A1),
    tertiary = Color(0xFF1E88E5),
    background = Color(0xFFF4F9FD),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE3F2FD),
    onBackground = Color(0xFF0D47A1),
    onSurface = Color(0xFF0D47A1),
    onSurfaceVariant = Color(0xFF1976D2),
    outline = Color(0xFF90CAF9)
)

@Composable
fun FocusFlowTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = when {
        (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !useDarkTheme
            insetsController.isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
