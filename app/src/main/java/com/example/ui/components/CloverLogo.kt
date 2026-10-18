package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CloverBg
import com.example.ui.theme.CloverDarkGreen
import com.example.ui.theme.CloverLightGreen
import com.example.ui.theme.CloverMidGreen

private val PIXEL_ROWS = listOf(
    "..dddd....dddd..",
    ".dgllgd..dgllgd.",
    "dgllllgddgllllgd",
    "dgllgggddgggllgd",
    "dggggggddggggggd",
    ".dgggggddgggggd.",
    "..dddddddddddd..",
    "..dddddddddddd..",
    ".dgggggddgggggd.",
    "dggggggddggggggd",
    "dgllgggddgggllgd",
    "dgllllgddgllllgd",
    ".dgllgd..dgllgd.",
    "..dddd.dd.dddd..",
    ".......dd.......",
    "......dd........"
)

@Composable
fun CloverLogo(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasW = this.size.width
            val canvasH = this.size.height

            // Background circle
            drawCircle(
                color = CloverBg,
                radius = canvasW / 2f,
                center = Offset(canvasW / 2f, canvasH / 2f)
            )

            // Draw 16x16 pixel art centered
            val pixelSize = (canvasW * 0.72f) / 16f
            val startX = (canvasW - (pixelSize * 16f)) / 2f
            val startY = (canvasH - (pixelSize * 16f)) / 2f

            for (row in 0 until 16) {
                val rowStr = PIXEL_ROWS[row]
                for (col in 0 until 16) {
                    val char = rowStr[col]
                    val color = when (char) {
                        'd' -> CloverDarkGreen
                        'g' -> CloverMidGreen
                        'l' -> CloverLightGreen
                        else -> null
                    }
                    if (color != null) {
                        drawRect(
                            color = color,
                            topLeft = Offset(startX + col * pixelSize, startY + row * pixelSize),
                            size = Size(pixelSize + 0.5f, pixelSize + 0.5f)
                        )
                    }
                }
            }
        }
    }
}
