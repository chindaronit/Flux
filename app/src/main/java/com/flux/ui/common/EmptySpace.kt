package com.flux.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flux.R
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight

@Composable
fun EmptyData() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SearchCancelIllustration(Modifier.size(240.dp, 245.dp))
        Text(stringResource(R.string.empty_no_data_found), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SearchCancelIllustration(
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Canvas(modifier = modifier) {
        // original artwork was authored on a 240x245 viewport; scale to actual size
        val scaleX = size.width / 240f
        val scaleY = size.height / 245f

        withTransform({ scale(scaleX, scaleY, pivot = Offset.Zero) }) {

            // background pills (primary tint)
            drawRoundRect(
                color = scheme.primary,
                topLeft = Offset(47f, 15f),
                size = Size(146f, 54f),
                cornerRadius = CornerRadius(27f, 27f),
                alpha = 0.12f
            )
            drawRoundRect(
                color = scheme.primary,
                topLeft = Offset(37f, 88f),
                size = Size(61f, 54f),
                cornerRadius = CornerRadius(27f, 27f),
                alpha = 0.10f
            )
            drawRoundRect(
                color = scheme.primary,
                topLeft = Offset(47f, 150f),
                size = Size(146f, 54f),
                cornerRadius = CornerRadius(27f, 27f),
                alpha = 0.12f
            )

            // back document
            val backDoc = Path().apply {
                moveTo(108f, 40f)
                lineTo(190f, 40f)
                lineTo(210f, 60f)
                lineTo(210f, 182f)
                quadraticTo(210f, 190f, 202f, 190f)
                lineTo(108f, 190f)
                quadraticTo(100f, 190f, 100f, 182f)
                lineTo(100f, 48f)
                quadraticTo(100f, 40f, 108f, 40f)
                close()
            }
            drawPath(backDoc, color = scheme.inverseSurface)

            val backDocFold = Path().apply {
                moveTo(190f, 40f)
                lineTo(210f, 60f)
                lineTo(190f, 60f)
                close()
            }
            drawPath(backDocFold, color = scheme.tertiary)

            // front document
            val frontDoc = Path().apply {
                moveTo(68f, 55f)
                lineTo(150f, 55f)
                lineTo(172f, 77f)
                lineTo(172f, 200f)
                quadraticTo(172f, 208f, 164f, 208f)
                lineTo(68f, 208f)
                quadraticTo(60f, 208f, 60f, 200f)
                lineTo(60f, 63f)
                quadraticTo(60f, 55f, 68f, 55f)
                close()
            }
            drawPath(frontDoc, color = scheme.surface)
            drawPath(frontDoc, color = scheme.outlineVariant, style = Stroke(width = 1.5f))

            val frontDocFold = Path().apply {
                moveTo(150f, 55f)
                lineTo(172f, 77f)
                lineTo(150f, 77f)
                close()
            }
            drawPath(frontDocFold, color = scheme.tertiary)

            // top text lines
            drawRoundRect(
                color = scheme.outlineVariant,
                topLeft = Offset(78f, 93f),
                size = Size(58f, 4f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            drawRoundRect(
                color = scheme.outlineVariant,
                topLeft = Offset(78f, 103f),
                size = Size(36f, 4f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // face: X eyes
            val eyeStroke = Stroke(width = 6f, cap = StrokeCap.Round)
            drawLine(scheme.primary, Offset(82f, 128f), Offset(102f, 148f), strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(scheme.primary, Offset(102f, 128f), Offset(82f, 148f), strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(scheme.primary, Offset(132f, 128f), Offset(152f, 148f), strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(scheme.primary, Offset(152f, 128f), Offset(132f, 148f), strokeWidth = 6f, cap = StrokeCap.Round)

            // face: mouth + tongue
            val mouth = Path().apply {
                moveTo(96f, 164f)
                quadraticTo(116f, 156f, 136f, 164f)
            }
            drawPath(mouth, color = scheme.onSurfaceVariant, style = Stroke(width = 5f, cap = StrokeCap.Round))
            drawOval(color = scheme.tertiary, topLeft = Offset(109f, 166f), size = Size(14f, 10f))

            // bottom text line
            drawRoundRect(
                color = scheme.outlineVariant,
                topLeft = Offset(78f, 188f),
                size = Size(51f, 4f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // magnifying glass lens
            drawCircle(color = scheme.surface, radius = 28f, center = Offset(72f, 188f))
            drawCircle(
                color = scheme.primary,
                radius = 28f,
                center = Offset(72f, 188f),
                style = Stroke(width = 9f)
            )

            // handle
            drawLine(
                color = scheme.onPrimaryContainer,
                start = Offset(96f, 212f),
                end = Offset(119f, 235f),
                strokeWidth = 12f,
                cap = StrokeCap.Round
            )

            // X mark: nothing found
            drawLine(scheme.onPrimaryContainer, Offset(60f, 176f), Offset(84f, 200f), strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(scheme.onPrimaryContainer, Offset(84f, 176f), Offset(60f, 200f), strokeWidth = 6f, cap = StrokeCap.Round)

            // floating dots
            drawCircle(color = scheme.tertiary, radius = 3f, center = Offset(190f, 93f), alpha = 0.6f)
            drawCircle(color = scheme.tertiary, radius = 3f, center = Offset(155f, 213f), alpha = 0.6f)
        }
    }
}