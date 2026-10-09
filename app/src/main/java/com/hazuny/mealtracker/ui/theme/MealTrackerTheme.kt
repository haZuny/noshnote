package com.hazuny.mealtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object MealTrackerSpacing {
    val screenHorizontal = 18.dp
    val screenTop = 12.dp
    val screenBottom = 24.dp
    val section = 12.dp
    val cardPadding = 16.dp
}

private val MealTrackerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

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
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFDCE6EB),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFCFD),
    surfaceContainer = Color(0xFFF5F8FA),
    surfaceContainerHigh = Color(0xFFEEF3F6),
    surfaceContainerHighest = Color(0xFFE7EEF2),
    surfaceVariant = Color(0xFFEEF2F4),
    onSurfaceVariant = Color(0xFF64737D),
    outline = Color(0xFFD5DFE5),
    error = Color(0xFFA84D47),
)

@Composable
fun MealTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MealTrackerColors,
        shapes = MealTrackerShapes,
        content = content,
    )
}
