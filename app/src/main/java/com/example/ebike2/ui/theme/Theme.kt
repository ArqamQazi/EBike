package com.example.ebike2.ui.theme

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

// --- Everforest Dark Colors ---
val EverforestDarkBackground = Color(0xFF2B3339)
val EverforestDarkSurface = Color(0xFF323C41)
val EverforestDarkText = Color(0xFFD3C6AA)
val EverforestDarkGreen = Color(0xFFA6B465)
val EverforestDarkBlue = Color(0xFF7FBBB3)
val EverforestDarkRed = Color(0xFFE67E80)
val EverforestDarkYellow = Color(0xFFDBBC7F)

// --- Everforest Light Colors ---
val EverforestLightBackground = Color(0xFFFDF6E3)
val EverforestLightSurface = Color(0xFFF4F0D9)
val EverforestLightText = Color(0xFF5C6A72)
val EverforestLightGreen = Color(0xFF8DA101)
val EverforestLightBlue = Color(0xFF3A94C5)
val EverforestLightRed = Color(0xFFF85552)
val EverforestLightYellow = Color(0xFFDFA000)

private val DarkColorScheme = darkColorScheme(
    background = EverforestDarkBackground,
    surface = EverforestDarkSurface,
    primary = EverforestDarkGreen,
    secondary = EverforestDarkBlue,
    error = EverforestDarkRed,
    onBackground = EverforestDarkText,
    onSurface = EverforestDarkText
)

private val LightColorScheme = lightColorScheme(
    background = EverforestLightBackground,
    surface = EverforestLightSurface,
    primary = EverforestLightGreen,
    secondary = EverforestLightBlue,
    error = EverforestLightRed,
    onBackground = EverforestLightText,
    onSurface = EverforestLightText
)

@Composable
fun EBike2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We set this to false so Android doesn't override Everforest with wallpaper colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Ensure you have your Typography defined in Type.kt
        content = content
    )
}
