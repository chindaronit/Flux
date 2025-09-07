package com.flux.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun DailyViewDateCard(date: Long, day: String, isSelected: Boolean, onClick: () -> Unit) {
    val localDate = LocalDate.ofEpochDay(date)

    val containerColor =
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHighest
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.width(60.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor.copy(alpha = 0.6f),
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(8.dp),
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                day,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraLight),
                modifier = Modifier.padding(top = 4.dp)
            )
            ElevatedCard(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    localDate.dayOfMonth.toString(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun MonthlyViewDateCard(date: Long, isSelected: Boolean, onClick: () -> Unit) {
    val localDate = LocalDate.ofEpochDay(date)

    val containerColor =
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerLow
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(50))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = localDate.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(containerColor)
                        .padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun DailyViewCalendar(
    selectedMonth: YearMonth,
    selectedDate: Long,
    onDateChange: (Long) -> Unit
) {
    val daysInMonth = selectedMonth.lengthOfMonth()
    val dateList = (1..daysInMonth).map { day -> selectedMonth.atDay(day) }
    val listState = rememberLazyListState()

    LaunchedEffect(selectedMonth, selectedDate) {
        val todayIndex = dateList.indexOfFirst { it.toEpochDay() == selectedDate }
        if (todayIndex >= 0) {
            listState.animateScrollToItem(
                index = maxOf(0, todayIndex - 2)
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            state = listState
        ) {
            items(dateList) { date ->
                val dayName = date.dayOfWeek.name
                    .take(3)
                    .lowercase()
                    .replaceFirstChar { it.uppercaseChar() }

                DailyViewDateCard(
                    date = date.toEpochDay(),
                    day = dayName,
                    isSelected = date.toEpochDay() == selectedDate,
                    onClick = { onDateChange(date.toEpochDay()) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyViewCalendar(
    currentMonth: YearMonth,
    selectedDate: Long,
    onMonthChange: (YearMonth) -> Unit,
    onDateChange: (Long) -> Unit
) {
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOffset = firstDayOfMonth.dayOfWeek.value % 7
    val daysInMonth = currentMonth.lengthOfMonth()

    val allDates = buildList {
        repeat(firstDayOffset) { add(null) }
        for (day in 1..daysInMonth) {
            add(currentMonth.atDay(day).toEpochDay())
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentMonth.month.name.lowercase()
                        .replaceFirstChar { it.uppercaseChar() } + ", ${currentMonth.year}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = {
                val prevMonth = currentMonth.minusMonths(1)
                onMonthChange(prevMonth)
                onDateChange(prevMonth.atDay(1).toEpochDay())
            }) {
                Icon(
                    Icons.AutoMirrored.Default.ArrowBackIos,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Previous month",
                    modifier = Modifier.size(18.dp).alpha(0.5f)
                )
            }

            IconButton(onClick = {
                val nextMonth = currentMonth.plusMonths(1)
                onMonthChange(nextMonth)
                onDateChange(nextMonth.atDay(1).toEpochDay())
            }) {
                Icon(
                    Icons.AutoMirrored.Default.ArrowForwardIos,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Next month",
                    modifier = Modifier.size(18.dp).alpha(0.5f)
                )
            }
        }

        // Weekday Row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daysOfWeek.forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // Calendar Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 300.dp),
            userScrollEnabled = false
        ) {
            items(allDates) { date ->
                if (date == null) {
                    Box(modifier = Modifier.size(48.dp))
                } else {
                    MonthlyViewDateCard(
                        date = date,
                        isSelected = selectedDate == date,
                        onClick = { onDateChange(date) }
                    )
                }
            }
        }
    }
}