package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

val LuckyLabelDarkGrey = Color(0xFF374151)

val LuckyTextSelectionColors = TextSelectionColors(
    handleColor = Color.Black,
    backgroundColor = LuckyHeader.copy(alpha = 0.22f)
)

val AppInputTextStyle = TextStyle(
    color = Color.Black,
    fontFamily = LuckySansFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    lineHeight = 24.sp
)

@Composable
fun appOutlinedTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    errorTextColor = Color.Black,
    disabledTextColor = Color.Black.copy(alpha = 0.6f),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    errorContainerColor = Color.White,
    disabledContainerColor = Color(0xFFF8FAFC),
    cursorColor = Color.Black,
    errorCursorColor = Color.Black,
    selectionColors = LuckyTextSelectionColors,
    focusedLabelColor = LuckyLabelDarkGrey,
    unfocusedLabelColor = LuckyLabelDarkGrey,
    disabledLabelColor = LuckyLabelDarkGrey.copy(alpha = 0.6f),
    errorLabelColor = LuckyDanger,
    focusedBorderColor = LuckyHeader,
    unfocusedBorderColor = LuckyBorder,
    errorBorderColor = LuckyDanger,
    disabledBorderColor = LuckyBorder.copy(alpha = 0.7f),
    errorSupportingTextColor = LuckyDanger,
    focusedSupportingTextColor = LuckyLabelDarkGrey,
    unfocusedSupportingTextColor = LuckyLabelDarkGrey
)

private val LuckyColorScheme = lightColorScheme(
    primary = LuckyHeader,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCECE7),
    onPrimaryContainer = LuckyBlueDark,
    secondary = LuckySuccess,
    onSecondary = Color.White,
    tertiary = LuckyDanger,
    onTertiary = Color.White,
    background = LuckyBg,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
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
        lineHeight = 33.sp,
        letterSpacing = 0.2.sp,
        color = Color.Black
    ),
    titleMedium = TextStyle(
        fontFamily = LuckySansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 29.sp,
        letterSpacing = 0.15.sp,
        color = Color.Black
    ),
    bodyLarge = AppInputTextStyle,
    bodyMedium = TextStyle(
        fontFamily = LuckySansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        color = Color.Black
    )
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTextSelectionColors provides LuckyTextSelectionColors) {
        MaterialTheme(
            colorScheme = LuckyColorScheme,
            typography = LuckyTypography,
            shapes = LuckyShapes,
            content = content
        )
    }
}

