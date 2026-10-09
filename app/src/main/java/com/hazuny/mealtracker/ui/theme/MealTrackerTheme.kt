package com.hazuny.mealtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MealTrackerColors = lightColorScheme(
    primary = Color(0xFF2588B2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F4FB),
    onPrimaryContainer = Color(0xFF194E66),
    secondary = Color(0xFF3C627A),
    onSecondary = Color.White,
    background = Color(0xFFF7FAFC),
    onBackground = Color(0xFF26333D),
    surface = Color.White,
    onSurface = Color(0xFF26333D),
    surfaceVariant = Color(0xFFEEF2F4),
    onSurfaceVariant = Color(0xFF64737D),
    outline = Color(0xFFD5DFE5),
    error = Color(0xFFA84D47),
)

@Composable
fun MealTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MealTrackerColors,
        content = content,
    )
}
