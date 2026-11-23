package com.example.currencyconverter.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.currencyconverter.repository.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE8B86D),
    onPrimary = Color(0xFF211403),
    primaryContainer = Color(0xFF42321C),
    onPrimaryContainer = Color(0xFFFBEED5),
    secondary = Color(0xFF86B39C),
    secondaryContainer = Color(0xFF1E362A),
    onSecondaryContainer = Color(0xFFD4E8DC),
    tertiary = Color(0xFFDF9E56),
    background = Color(0xFF181412),
    surface = Color(0xFF241E1A),
    surfaceVariant = Color(0xFF2E2722),
    onBackground = Color(0xFFF3EFE6),
    onSurface = Color(0xFFF3EFE6),
    onSurfaceVariant = Color(0xFFC2B6A8),
    outline = Color(0xFF3D332C),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4A2E1B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF5EBE1),
    onPrimaryContainer = Color(0xFF331E10),
    secondary = Color(0xFF2D4A3E),
    secondaryContainer = Color(0xFFE2EDE7),
    onSecondaryContainer = Color(0xFF132B21),
    tertiary = Color(0xFFB88014),
    background = Color(0xFFFAF6EE),
    surface = Color(0xFFFFFDF8),
    surfaceVariant = Color(0xFFF2ECE1),
    onBackground = Color(0xFF261C14),
    onSurface = Color(0xFF261C14),
    onSurfaceVariant = Color(0xFF6E5D50),
    outline = Color(0xFFDDD4C7),
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
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()
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
