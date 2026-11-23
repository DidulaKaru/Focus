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

// "Oceanic Midnight & Luminous Ice Blue" (Dark Palette)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7EB8F0),
    onPrimary = Color(0xFF081D33),
    primaryContainer = Color(0xFF173352),
    onPrimaryContainer = Color(0xFFDDEEFE),
    secondary = Color(0xFF72C5D3),
    secondaryContainer = Color(0xFF153B44),
    onSecondaryContainer = Color(0xFFCFEFF5),
    tertiary = Color(0xFF94B5D6),
    background = Color(0xFF0B131E),
    surface = Color(0xFF121C2B),
    surfaceVariant = Color(0xFF1A2638),
    onBackground = Color(0xFFECF2F8),
    onSurface = Color(0xFFECF2F8),
    onSurfaceVariant = Color(0xFFA1B3C6),
    outline = Color(0xFF27384C)
)

// "Nordic Frost & Deep Slate" (Light Palette)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E3A5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1EAF5),
    onPrimaryContainer = Color(0xFF0D2340),
    secondary = Color(0xFF3B6978),
    secondaryContainer = Color(0xFFE0EDF2),
    onSecondaryContainer = Color(0xFF122C34),
    tertiary = Color(0xFF4A6984),
    background = Color(0xFFF6F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDF1F5),
    onBackground = Color(0xFF111A24),
    onSurface = Color(0xFF111A24),
    onSurfaceVariant = Color(0xFF526375),
    outline = Color(0xFFD8E0E8)
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
