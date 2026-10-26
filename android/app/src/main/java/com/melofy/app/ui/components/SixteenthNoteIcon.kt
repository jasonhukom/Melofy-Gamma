package com.melofy.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom vector icon rendering a slanted beamed sixteenth note (♬ with double slanted top beams).
 */
@Composable
fun SixteenthNoteIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Note 1 (Left)
        val note1Center = Offset(w * 0.28f, h * 0.78f)
        val note1Radius = w * 0.12f

        // Note 2 (Right)
        val note2Center = Offset(w * 0.72f, h * 0.68f)
        val note2Radius = w * 0.12f

        // Draw note heads (tilted filled ovals)
        drawCircle(color = tint, radius = note1Radius, center = note1Center)
        drawCircle(color = tint, radius = note2Radius, center = note2Center)

        // Stems
        val stem1X = note1Center.x + note1Radius * 0.7f
        val stem2X = note2Center.x + note2Radius * 0.7f

        val stem1TopY = h * 0.22f
        val stem2TopY = h * 0.12f // Slanted higher on right

        val strokeWidth = w * 0.08f

        // Left Stem
        drawLine(
            color = tint,
            start = Offset(stem1X, note1Center.y),
            end = Offset(stem1X, stem1TopY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Right Stem
        drawLine(
            color = tint,
            start = Offset(stem2X, note2Center.y),
            end = Offset(stem2X, stem2TopY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Top Slanted Beam 1
        val beam1Path = Path().apply {
            moveTo(stem1X - strokeWidth / 2, stem1TopY)
            lineTo(stem2X + strokeWidth / 2, stem2TopY)
            lineTo(stem2X + strokeWidth / 2, stem2TopY + h * 0.09f)
            lineTo(stem1X - strokeWidth / 2, stem1TopY + h * 0.09f)
            close()
        }
        drawPath(path = beam1Path, color = tint)

        // Second Slanted Beam 2 (Sixteenth note double beam)
        val beam2Offset = h * 0.13f
        val beam2Path = Path().apply {
            moveTo(stem1X - strokeWidth / 2, stem1TopY + beam2Offset)
            lineTo(stem2X + strokeWidth / 2, stem2TopY + beam2Offset)
            lineTo(stem2X + strokeWidth / 2, stem2TopY + beam2Offset + h * 0.08f)
            lineTo(stem1X - strokeWidth / 2, stem1TopY + beam2Offset + h * 0.08f)
            close()
        }
        drawPath(path = beam2Path, color = tint)
    }
}
