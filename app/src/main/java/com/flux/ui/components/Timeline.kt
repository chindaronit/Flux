package com.flux.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TimelineBody(
    isLast: Boolean,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(0.3f),
    thickness: Dp = 2.dp
) {
    Canvas(
        modifier = Modifier
            .width(16.dp)
            .fillMaxHeight()
    ) {
        if (!isLast) {
            val x = size.width / 2f
            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = thickness.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

