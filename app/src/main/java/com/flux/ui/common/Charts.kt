package com.flux.ui.common

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flux.R
import com.flux.ui.screens.settings.shapeManager
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun HeatMapCard(
    radius: Int,
    title: String,
    description: String,
    boxSize: Dp,
    intensityParam: Int,
    lazyListState: LazyListState,
    weekColumns: List<List<LocalDate?>>,
    heatMap: Map<LocalDate, Int>
){
    Card(
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
        shape = shapeManager(radius = radius*2),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )
            if(description.isNotBlank()){
                Text(
                    description,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .width(boxSize)
                        .padding(top = 26.dp, end = 2.dp)
                ) {
                    DayOfWeek.entries.forEach { day ->
                        Box(
                            modifier = Modifier
                                .width(boxSize + 12.dp)
                                .height(boxSize),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(text = day.name.take(3), fontSize = 9.sp)
                        }
                    }
                }

                // Combined month + heatmap
                LazyRow(
                    state = lazyListState,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(weekColumns) { index, columnDates ->
                        val firstDate = columnDates.firstOrNull()
                        val month = firstDate?.month

                        // Show month label if this is the first week of the month
                        val showMonth =
                            month != null && (index == 0 || weekColumns.getOrNull(index - 1)?.firstOrNull()?.month != month)

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Month label on top (only once per month)
                            Box(
                                modifier = Modifier.height(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (showMonth) {
                                    Text(
                                        text = firstDate.month.name.take(3),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Heatmap boxes
                            columnDates.forEach { date ->
                                if (date != null) {
                                    val count = heatMap[date] ?: 0
                                    val intensity = (count / if (intensityParam > 0) intensityParam.toFloat() else 2f).coerceIn(0f, 1f)
                                    val color = lerp(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        MaterialTheme.colorScheme.primary,
                                        intensity
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(boxSize)
                                            .background(color, RoundedCornerShape(3.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            fontSize = 9.sp,
                                            color = if (intensity > 0.5f) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else {
                                    Box(modifier = Modifier.size(boxSize))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyProgressChart(
    radius: Int,
    modifier: Modifier = Modifier,
    percentages: List<Float>
){
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryAlpha = primaryColor.copy(alpha = 0.4f)

    val today = LocalDate.now()
    val startOfWeek = today.with(DayOfWeek.MONDAY)
    val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }

    val dayLabels = listOf(
        stringResource(R.string.monday_short),
        stringResource(R.string.tuesday_short),
        stringResource(R.string.wednesday_short),
        stringResource(R.string.thursday_short),
        stringResource(R.string.friday_short),
        stringResource(R.string.saturday_short),
        stringResource(R.string.sunday_short)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        ),
        onClick = {}
    ) {
        Column(modifier = modifier.padding(12.dp)) {
            Text(
                stringResource(R.string.This_Week),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
                    .height(200.dp)
            ) {
                val leftPadding = 32.dp.toPx()
                val bottomPadding = 32.dp.toPx()
                val widthPerPoint = (size.width - leftPadding) / (weekDays.size - 1)
                val height = size.height - bottomPadding

                // Vertical percentage labels
                for (i in 0..4) {
                    val y = height - i * height / 4
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            "${i * 25}%",
                            leftPadding - 14.dp.toPx(),
                            y,
                            Paint().apply {
                                color = primaryColor.toArgb()
                                textSize = 12.sp.toPx()
                                textAlign = Paint.Align.RIGHT
                            }
                        )
                    }
                }

                // Line path
                val linePath = Path().apply {
                    percentages.forEachIndexed { index, percentage ->
                        val x = leftPadding + index * widthPerPoint
                        val y = height - (percentage / 100f * height)
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }

                // Shadow under-line
                val shadowPath = Path().apply {
                    addPath(linePath)
                    lineTo(leftPadding + (weekDays.size - 1) * widthPerPoint, height)
                    lineTo(leftPadding, height)
                    close()
                }

                drawPath(
                    path = shadowPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryAlpha, primaryColor.copy(alpha = 0f)),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw line
                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw dots
                percentages.forEachIndexed { index, percentage ->
                    val x = leftPadding + index * widthPerPoint
                    val y = height - (percentage / 100f * height)
                    drawCircle(
                        color = primaryColor,
                        radius = 6.dp.toPx(),
                        center = Offset(x, y)
                    )
                }

                // Draw day labels
                dayLabels.forEachIndexed { index, day ->
                    val x = leftPadding + index * widthPerPoint
                    val y = height + 24.dp.toPx()
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            day,
                            x,
                            y,
                            Paint().apply {
                                color = primaryColor.toArgb()
                                textSize = 12.sp.toPx()
                                textAlign = Paint.Align.CENTER
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyAnalytics(
    radius: Int,
    dayStatus: List<Boolean>
) {
    val daysOfWeek = listOf(
        stringResource(R.string.monday_short),
        stringResource(R.string.tuesday_short),
        stringResource(R.string.wednesday_short),
        stringResource(R.string.thursday_short),
        stringResource(R.string.friday_short),
        stringResource(R.string.saturday_short),
        stringResource(R.string.sunday_short)
    )

    val completedCount = dayStatus.count { it }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)),
        onClick = {}
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Text(
                text = stringResource(R.string.This_Week),
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = stringResource(R.string.completed_habits, completedCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEachIndexed { index, day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Card(
                            shape = RoundedCornerShape(50),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = if(dayStatus[index]) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        ) {
                            Icon(
                                imageVector = if (dayStatus[index]) Icons.Default.Verified else Icons.Default.Pending,
                                contentDescription = null,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarChart(
    weekCounts: List<Int>,
    weekLabels: List<String>,
    modifier: Modifier = Modifier
) {
    val maxDaysPerWeek = 7
    val yLabels = (maxDaysPerWeek downTo 1).toList()
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(bottom = 16.dp)
    ) {
        // Y-axis labels
        Box(
            modifier = Modifier
                .width(20.dp)
                .padding(end = 8.dp)
                .fillMaxHeight()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepHeight = size.height / maxDaysPerWeek
                val textPaint = Paint().apply {
                    isAntiAlias = true
                    color = primaryColor.toArgb()
                    textSize = 30f
                    textAlign = Paint.Align.CENTER
                }

                yLabels.forEach { label ->
                    val y = size.height - (stepHeight * label)
                    drawContext.canvas.nativeCanvas.drawText(
                        label.toString(),
                        size.width - 10f,
                        y + 5f,
                        textPaint
                    )
                }
            }
        }

        // Bar chart canvas
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            val barWidth = size.width / (weekCounts.size * 2 + 1)
            val stepHeight = size.height / maxDaysPerWeek

            weekCounts.forEachIndexed { index, count ->
                val barHeight = stepHeight * count
                val x = barWidth + index * (barWidth + barWidth)
                val y = size.height - barHeight

                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(x = 16.dp.toPx(), y = 16.dp.toPx())
                )

                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        isAntiAlias = true
                        color = primaryColor.toArgb()
                        textSize = 30f
                        textAlign = Paint.Align.CENTER
                    }

                    canvas.nativeCanvas.drawText(
                        weekLabels[index],
                        x + barWidth / 2,
                        size.height + 40f,
                        paint
                    )
                }
            }

            for (i in 1..maxDaysPerWeek) {
                val y = size.height - (stepHeight * i)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 0.25.dp.toPx()
                )
            }
        }
    }
}