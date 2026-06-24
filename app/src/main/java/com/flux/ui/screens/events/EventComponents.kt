package com.flux.ui.screens.events

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.flux.R
import com.flux.data.model.RecurrenceRule
import com.flux.ui.screens.settings.CircleWrapper
import com.flux.ui.screens.settings.shapeManager
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import java.time.LocalDate
import java.time.YearMonth

// ------------- Dialogs -------------
@Composable
fun EventNotificationDialog(
    currentOffset: Long,
    onChange: (Long) -> Unit,
    onCustomClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val options = listOf(
        0L to stringResource(R.string.On_Time),
        5L to stringResource(R.string.five_minutes_before),
        30L to stringResource(R.string.thirty_minutes_before)
    )

    // Convert to minutes for comparison
    val currentMinutes = currentOffset / 1000 / 60

    Dialog(onDismissRequest = onDismissRequest) {
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircleWrapper(MaterialTheme.colorScheme.primary) {
                    Icon(
                        Icons.Outlined.NotificationsActive,
                        contentDescription = "Notification Icon",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Text(
                    stringResource(R.string.Add_Notification),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.height(16.dp))
                options.forEach { (minutesBefore, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onChange(minutesBefore * 60 * 1000)
                                onDismissRequest()
                            }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label)
                        RadioButton(
                            selected = currentMinutes == minutesBefore,
                            onClick = {
                                onChange(minutesBefore * 60 * 1000)
                                onDismissRequest()
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onCustomClick()
                            onDismissRequest()
                        }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.Custom))
                    RadioButton(
                        selected = options.none { it.first == currentMinutes },
                        onClick = {
                            onCustomClick()
                            onDismissRequest()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomNotificationDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (offsetMillis: Long) -> Unit
) {
    val timeUnits = listOf(
        stringResource(R.string.minutes),
        stringResource(R.string.hours),
        stringResource(R.string.days)
    )
    var selectedUnit by remember { mutableStateOf(timeUnits[0]) }
    var amountText by remember { mutableStateOf("1") }
    val amount = amountText.toIntOrNull()?.coerceAtLeast(1) ?: 1

    val offsetMillis = when (selectedUnit) {
        stringResource(R.string.minutes) -> amount * 60_000L
        stringResource(R.string.hours) -> amount * 60 * 60_000L
        stringResource(R.string.days) -> amount * 24 * 60 * 60_000L
        else -> 0L
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.Custom_Notification),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))

                timeUnits.forEach { unit ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .selectable(
                                selected = selectedUnit == unit,
                                onClick = { selectedUnit = unit }
                            )
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedUnit == unit,
                            onClick = { selectedUnit = unit }
                        )
                        Text(unit, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it },
                    label = { Text(stringResource(R.string.Amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.Cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        onConfirm(offsetMillis)
                        onDismissRequest()
                    }) {
                        Text(stringResource(R.string.Set))
                    }
                }
            }
        }
    }
}

@Composable
fun IconRadioButton(
    modifier: Modifier = Modifier,
    uncheckedTint: Color = MaterialTheme.colorScheme.onSurface,
    checkedTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    selected: Boolean,
    onClick: () -> Unit
) {
    IconButton(modifier = modifier, onClick = onClick) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = checkedTint
            )
        } else {
            Icon(
                imageVector = Icons.Default.RadioButtonUnchecked,
                contentDescription = "Unselected",
                tint = uncheckedTint
            )
        }
    }
}

@Composable
fun EventCard(
    radius: Int,
    is24HourFormat: Boolean,
    isPending: Boolean,
    title: String,
    repeat: RecurrenceRule,
    startDateTime: Long,
    onChangeStatus: () -> Unit,
    onClick: () -> Unit
) {
    val containerColor =
        if (isPending) MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        else MaterialTheme.colorScheme.primaryContainer
    val contentColor =
        if (isPending) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onPrimaryContainer

    val context = LocalContext.current
    val time = startDateTime.toFormattedTime(is24HourFormat)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = shapeManager(radius = radius * 2),
        onClick = onClick
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            IconRadioButton(
                selected = !isPending,
                onClick = onChangeStatus
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${getRecurrenceText(context, repeat, startDateTime)} at $time",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

fun getRecurrenceText(context: Context, repeat: RecurrenceRule, startDateTime: Long): String {
    val localDate = Instant.ofEpochMilli(startDateTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    return when (repeat) {
        is RecurrenceRule.Once -> {
            context.getString(
                R.string.recurrence_once,
                localDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            )
        }

        is RecurrenceRule.Custom -> {
            if (repeat.everyXDays == 1) {
                context.getString(R.string.recurrence_daily)
            } else {
                context.getString(R.string.recurrence_every_x_days, repeat.everyXDays)
            }
        }

        is RecurrenceRule.Weekly -> {
            val days = listOf("M", "T", "W", "T", "F", "S", "S")
            if (repeat.daysOfWeek.size == 7) {
                context.getString(R.string.recurrence_daily)
            } else {
                val daysText = repeat.daysOfWeek.sorted().joinToString(", ") { days[it] }
                context.getString(R.string.recurrence_weekly_on, daysText)
            }
        }

        is RecurrenceRule.Monthly -> {
            context.getString(R.string.recurrence_monthly_on, localDate.dayOfMonth)
        }

        is RecurrenceRule.Yearly -> {
            context.getString(
                R.string.recurrence_yearly_on,
                localDate.format(DateTimeFormatter.ofPattern("MMM dd"))
            )
        }

        else -> ""
    }
}

fun Long.toFormattedTime(is24Hour: Boolean = false): String {
    val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(Date(this))
}

fun Long.toFormattedDate(): String {
    val date = Date(this)
    val format = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
    return format.format(date)
}

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
fun MonthlyViewDateCard(date: Long, count: Int, maxCount: Int = 0, isSelected: Boolean, onClick: () -> Unit) {
    val localDate = LocalDate.ofEpochDay(date)
    val fraction = if (maxCount > 0 && count > 0) count.toFloat() / maxCount.toFloat() else 0f
    val containerColor =
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerLow
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(50))
            .drawBehind {
                if (fraction > 0f && !isSelected) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.35f * fraction),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension / 2f
                        )
                    )
                }
            }
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
    monthlyJournalCount:  Map<LocalDate, Int> = emptyMap(),
    onMonthChange: (YearMonth) -> Unit,
    onDateChange: (Long) -> Unit
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
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOffset = (firstDayOfMonth.dayOfWeek.value - 1) % 7
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
                    modifier = Modifier
                        .size(18.dp)
                        .alpha(0.5f)
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
                    modifier = Modifier
                        .size(18.dp)
                        .alpha(0.5f)
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
                        count = monthlyJournalCount[LocalDate.ofEpochDay(date)] ?: 0,
                        maxCount = monthlyJournalCount.values.maxOrNull() ?: 0,
                        onClick = { onDateChange(date) }
                    )
                }
            }
        }
    }
}