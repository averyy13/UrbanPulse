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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784), // Sleek, modern pastel green
    onPrimary = Color(0xFF00330C),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFA5D6A7),
    secondaryContainer = Color(0xFF2E4030),
    onSecondaryContainer = Color(0xFFE8F5E9),
    background = Color(0xFF121412), // Sleek dark aesthetic
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF1E211E), // Slightly elevated surface dark tone
    onSurface = Color(0xFFE3E3E3),
    onSurfaceVariant = Color(0xFFB0BEC5)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = SurfaceWhite,
    primaryContainer = LightGreen,
    onPrimaryContainer = DarkGreen,
    secondary = DarkGrey,
    secondaryContainer = LightGrey,
    onSecondaryContainer = DarkGrey,
    background = BackgroundGreen,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We disable dynamic color to strictly follow the Natural Tones design
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
