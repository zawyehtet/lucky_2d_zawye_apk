package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

val LuckySansFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold),
    Font(R.font.inter, FontWeight.ExtraBold)
)

private val LuckyColorScheme = lightColorScheme(
    primary = LuckyHeader,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCECE7),
    onPrimaryContainer = LuckyBlueDark,
    secondary = LuckySuccess,
    onSecondary = Color.White,
    background = LuckyBg,
    onBackground = LuckyTextPrimary,
    surface = LuckyCard,
    onSurface = LuckyTextPrimary,
    surfaceVariant = LuckyCardDark,
    onSurfaceVariant = LuckyTextSecondary,
    outline = LuckyBorder,
    error = LuckyDanger,
    onError = Color.White
)

val LuckyShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

val LuckyTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = LuckySansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.2.sp
    ),
    titleMedium = TextStyle(
        fontFamily = LuckySansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = LuckySansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = LuckySansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    )
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LuckyColorScheme,
        typography = LuckyTypography,
        shapes = LuckyShapes,
        content = content
    )
}
