package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LuckyColorScheme = darkColorScheme(
    primary = LuckyLime,
    onPrimary = LuckyBg,
    primaryContainer = LuckyHeader,
    onPrimaryContainer = LuckyTextPrimary,
    secondary = LuckyHeader,
    onSecondary = LuckyTextPrimary,
    background = LuckyBg,
    onBackground = LuckyTextPrimary,
    surface = LuckyCard,
    onSurface = LuckyTextPrimary,
    surfaceVariant = LuckyKeyBg,
    onSurfaceVariant = LuckyTextSecondary,
    outline = LuckyBorder,
    error = LuckyDanger,
    onError = LuckyTextPrimary
)

val LuckyShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LuckyColorScheme,
        typography = Typography,
        shapes = LuckyShapes,
        content = content
    )
}
