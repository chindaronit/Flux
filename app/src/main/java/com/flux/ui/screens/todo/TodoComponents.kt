package com.flux.ui.screens.todo

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.Note
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.ItemConsistency
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.TodoInstance
import com.flux.data.model.TodoItem
import com.flux.data.model.TodoModel
import com.flux.data.model.calculateItemConsistency
import com.flux.data.model.isCompleted
import com.flux.navigation.NavRoutes
import com.flux.other.ConvertType
import com.flux.ui.common.BarChart
import com.flux.ui.common.HeatMapCard
import com.flux.ui.common.TimePicker
import com.flux.ui.common.WeeklyAnalytics
import com.flux.ui.common.WeeklyProgressChart
import com.flux.ui.common.convertMillisToDate
import com.flux.ui.common.convertMillisToTime
import com.flux.ui.events.TodoEvents
import com.flux.ui.screens.events.toFormattedTime
import com.flux.ui.screens.habits.HabitInfoComponent
import com.flux.ui.screens.habits.isDateAllowedForHabit
import com.flux.ui.screens.notes.ExportCard
import com.flux.ui.screens.settings.shapeManager
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoExpandableCard(
    navController: NavController,
    radius: Int,
    context: Context,
    item: TodoModel,
    isExpanded: Boolean,
    workspaceId: String,
    onExpandToggle: (String) -> Unit,
    onTodoEvents: (TodoEvents) -> Unit
) {
    Card(
        modifier = Modifier.padding(top = 4.dp),
        shape = if(isExpanded) shapeManager(isBoth = true, radius=radius) else RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            TodoHeaderRow(
                id = item.id,
                title = item.title,
                isReminderOn = item.recurrence is RecurrenceRule.Weekly,
                onExpandToggle = onExpandToggle,
                onNavigate = {
                    navController.navigate(
                        NavRoutes.TodoDetail.withArgs(workspaceId, item.id)
                    )
                }
            )

            if (isExpanded) {
                TodoItems(
                    context = context,
                    todoList = item,
                    workspaceId = workspaceId,
                    onTodoEvents = onTodoEvents
                )
            }
        }
    }
}

@Composable
private fun TodoHeaderRow(
    id: String,
    title: String,
    isReminderOn: Boolean,
    onExpandToggle: (String) -> Unit,
    onNavigate: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .fillMaxWidth()
            .clickable { onExpandToggle(id) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 20.dp, end = 3.dp),
        )
        Row {
            if(isReminderOn){
                IconButton(onClick = onNavigate) {
                    Icon(Icons.Default.Alarm, null)
                }
            }
            IconButton(onClick = onNavigate) {
                Icon(Icons.Default.Analytics, null)
            }
        }
    }
}

@Composable
private fun TodoItems(
    context: Context,
    todoList: TodoModel,
    workspaceId: String,
    onTodoEvents: (TodoEvents) -> Unit
) {
    fun onToggleCheck (todoItem: TodoItem) {
        val updatedItems = todoList.items.map {
            if (it.id == todoItem.id)
                it.copy(isChecked = !it.isChecked)
            else it
        }

        if (updatedItems != todoList.items) {
            onTodoEvents(
                TodoEvents.UpsertList(
                    context,
                    false,
                    todoList.copy(
                        items = updatedItems,
                        workspaceId = workspaceId
                    )
                )
            )
        }
    }
    val checkedItems = todoList.items.filter { it.isChecked }
    val unCheckedItems = todoList.items.filter { !it.isChecked }
    val allSortedItems = unCheckedItems + checkedItems

    LazyColumn(
        Modifier
            .padding(horizontal = 6.dp)
            .padding(top = 4.dp, bottom = 12.dp)
            .heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(allSortedItems) { todoItem ->
            MaterialListItem(true, todoItem){ onToggleCheck(todoItem) }
        }
    }
}

@Composable
fun MaterialListItem(
    enabled: Boolean = true,
    todoItem: TodoItem,
    onToggleCheck: () -> Unit
){
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
            containerColor = if (todoItem.isChecked) MaterialTheme.colorScheme.primaryContainer.copy(0.5f) else MaterialTheme.colorScheme.primaryContainer.copy(0.7f)
        ),
        onClick = onToggleCheck
    ) {
        Row (verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onToggleCheck, colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                if (enabled) { Icon(if (todoItem.isChecked) Icons.Default.Verified else Icons.Outlined.Circle, null) }
                else { Icon(Icons.Default.Circle, null, tint = MaterialTheme.colorScheme.primary) }
            }

            Text(
                text = todoItem.value,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun TodoReminderDialog(
    is24HourFormat: Boolean,
    reminderTime: Long,
    recurrence: RecurrenceRule,
    onDismiss: () -> Unit,
    onConfirm: (RecurrenceRule, Long) -> Unit
){
    var newReminderTime by remember { mutableLongStateOf(reminderTime) }
    var currentRecurrence by remember(recurrence) { mutableStateOf(recurrence) }
    val selectedDays = remember { mutableStateListOf<Int>() }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(currentRecurrence) {
        selectedDays.clear()

        if (currentRecurrence is RecurrenceRule.Weekly) {
            selectedDays.addAll(
                (currentRecurrence as RecurrenceRule.Weekly).daysOfWeek
            )
        }
    }

    val weekdays = listOf(
        stringResource(R.string.monday_short),
        stringResource(R.string.tuesday_short),
        stringResource(R.string.wednesday_short),
        stringResource(R.string.thursday_short),
        stringResource(R.string.friday_short),
        stringResource(R.string.saturday_short),
        stringResource(R.string.sunday_short)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(min = 180.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Repeat, null)
                        Text(stringResource(R.string.repeat), fontWeight = FontWeight.SemiBold)
                    }

                    Switch(
                        modifier = Modifier.scale(0.8f),
                        checked = currentRecurrence !is RecurrenceRule.NONE, onCheckedChange = {
                            currentRecurrence = if(it) {
                                newReminderTime = if(recurrence is RecurrenceRule.NONE) System.currentTimeMillis()
                                else { reminderTime }

                                recurrence as? RecurrenceRule.Weekly ?: RecurrenceRule.Weekly()
                            }
                            else { RecurrenceRule.NONE }
                    })
                }

                if(currentRecurrence is RecurrenceRule.Weekly){
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        weekdays.forEachIndexed { index, day ->
                            val isSelected = index in selectedDays

                            Card(
                                onClick = {
                                    if (isSelected) { if (selectedDays.size > 1) { selectedDays.remove(index) } }
                                    else { selectedDays.add(index) }
                                },
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(40.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor =
                                        if (isSelected) { MaterialTheme.colorScheme.primary }
                                        else { MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp) },
                                    contentColor =
                                        if (isSelected) { MaterialTheme.colorScheme.onPrimary }
                                        else { MaterialTheme.colorScheme.onSurface }
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ){ Text(day) }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AlarmAdd, null)
                        Text(stringResource(R.string.reminder_time), fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(newReminderTime.toFormattedTime(is24HourFormat))
                        FilledTonalIconButton({ showTimePicker = true }) {
                            Icon(Icons.Default.Create, null)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        FilledTonalButton(onDismiss) {
                            Text(stringResource(R.string.Cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton({
                            if(currentRecurrence is RecurrenceRule.Weekly){
                                onConfirm(RecurrenceRule.Weekly(selectedDays), newReminderTime)
                            }
                            else { onConfirm(RecurrenceRule.NONE, -1L) }

                            onDismiss()
                        }) {
                            Text(stringResource(R.string.Confirm))
                        }
                    }
                }
                else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.turn_on_reminder), modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        FilledTonalButton(onDismiss) {
                            Text(stringResource(R.string.Cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton({
                            onConfirm(RecurrenceRule.NONE, reminderTime)
                            onDismiss()
                        }) {
                            Text(stringResource(R.string.Confirm))
                        }
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        TimePicker(
            initialTime = newReminderTime,
            is24Hour = is24HourFormat,
            onConfirm = { newReminderTime = it }
        ) { showTimePicker = false }
    }
}

@Composable
fun TodoDetailedInfo(
    radius: Int,
    list: TodoModel,
    isReminderOn: Boolean = false,
    isAllowedToday: Boolean = false,
    todayInstance: TodoInstance? = null
){
    val items = if(isReminderOn) todayInstance?.items ?: list.items else list.items
    val completedNumber = items.filter { it.isChecked }.size
    val remainingNumber = items.filter { !it.isChecked }.size
    val completedPercentage = (completedNumber * 100f / items.size.coerceAtLeast(1)).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)),
        onClick = {}
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Checklist, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = list.title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val windowInfo = LocalWindowInfo.current
            val density = LocalDensity.current

            val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }

            val columns = when {
                density.fontScale > 1.5f -> 1

                screenWidthDp < 360.dp -> 1
                screenWidthDp < 480.dp -> 2

                else -> 3
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 1000.dp)
                    .padding(vertical = 8.dp)
            ) {
                item {
                    HabitInfoComponent(
                        Icons.Default.Create,
                        stringResource(R.string.created),
                        convertMillisToDate(list.startDateTime)
                    )
                }

                item {
                    HabitInfoComponent(
                        if(isReminderOn) Icons.Default.AlarmOn else Icons.Default.AlarmOff,
                        stringResource(R.string.reminder),
                        if (isReminderOn) stringResource(R.string.on) else stringResource(R.string.off)
                    )
                }

                if(isReminderOn){
                    item {
                        HabitInfoComponent(
                            Icons.Default.Alarm,
                            stringResource(R.string.remind_at),
                            convertMillisToTime(list.startDateTime)
                        )
                    }

                    item {
                        HabitInfoComponent(
                            Icons.Default.DateRange,
                            stringResource(R.string.scheduled),
                            if(isAllowedToday) stringResource(R.string.true_text) else stringResource(R.string.false_text)
                        )
                    }
                }

                if((isReminderOn && isAllowedToday) || !isReminderOn){
                    item {
                        HabitInfoComponent(
                            Icons.Outlined.CheckCircle,
                            stringResource(R.string.completed),
                            completedNumber.toString()
                        )
                    }

                    item {
                        HabitInfoComponent(
                            Icons.Outlined.Circle,
                            stringResource(R.string.remaining),
                            remainingNumber.toString()
                        )
                    }

                    item {
                        HabitInfoComponent(
                            Icons.Outlined.Percent,
                            stringResource(R.string.completion),
                            "$completedPercentage%"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodoHeatMap(radius: Int, todoModel: TodoModel, instances: List<TodoInstance>){
    val today = LocalDate.now()
    val yearStart = LocalDate.of(today.year, 1, 1)

    // Calculate the offset from Monday for January 1st
    val jan1DayOfWeek = yearStart.dayOfWeek.value // Monday = 1, Sunday = 7
    val offsetFromMonday = jan1DayOfWeek - 1 // 0 for Monday, 6 for Sunday

    val totalDays = ChronoUnit.DAYS.between(yearStart, today).toInt() + 1
    val allDates = (0 until totalDays).map { yearStart.plusDays(it.toLong()) }

    val heatMap = remember(instances, todoModel) {
        instances
            .groupBy { LocalDate.ofEpochDay(it.instanceDate) }
            .mapValues { (_, instancesForDay) ->
                instancesForDay.count { instance ->
                    instance.isCompleted()
                }
            }
    }

    // Create week columns with proper day alignment
    val weekColumns = mutableListOf<List<LocalDate?>>()
    var currentWeek = MutableList<LocalDate?>(7) { null }

    // Fill the first week with nulls for days before January 1st
    for (i in 0 until offsetFromMonday) {
        currentWeek[i] = null
    }

    // Add all dates starting from the correct day of week
    allDates.forEachIndexed { index, date ->
        val dayIndex = (offsetFromMonday + index) % 7
        currentWeek[dayIndex] = date

        // When we complete a week (reach Sunday) or it's the last date
        if (dayIndex == 6 || index == allDates.size - 1) {
            weekColumns.add(currentWeek.toList())
            currentWeek = MutableList(7) { null }
        }
    }
    val boxSize = 24.dp
    val lazyListState = rememberLazyListState()

    // Calculate the index of the current month's first week
    val currentMonthStartIndex = remember(weekColumns) {
        val currentMonth = today.month
        weekColumns.indexOfFirst { week ->
            week.any { date -> date?.month == currentMonth }
        }.takeIf { it != -1 } ?: 0
    }

    // Auto-scroll to current month on first composition
    LaunchedEffect(currentMonthStartIndex) {
        if (currentMonthStartIndex > 0) {
            lazyListState.scrollToItem(index = maxOf(0, currentMonthStartIndex - 2))
        }
    }

    HeatMapCard(
        radius,
        stringResource(R.string.this_year),
        "",
        boxSize,
        2,
        lazyListState,
        weekColumns,
        heatMap.toMap()
    )
}

@Composable
fun ConvertTODODialog(
    onConfirm: (ConvertType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.convert),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ExportCard(Icons.AutoMirrored.Outlined.Note, stringResource(R.string.convert_to_note)) { onConfirm(ConvertType.NOTE) }
                ExportCard(Icons.Outlined.AutoStories, stringResource(R.string.convert_to_journal)) { onConfirm(ConvertType.JOURNAL) }
            }
        }
    }
}

@Composable
fun WeeklyTodoAnalytics(
    radius: Int,
    instances: List<TodoInstance>
) {
    val today = LocalDate.now()
    val startOfWeek = today.with(DayOfWeek.MONDAY)

    val instanceMap = remember(instances) {
        instances.associateBy { it.instanceDate }
    }

    val daysOfWeek = listOf(
        stringResource(R.string.monday_short),
        stringResource(R.string.tuesday_short),
        stringResource(R.string.wednesday_short),
        stringResource(R.string.thursday_short),
        stringResource(R.string.friday_short),
        stringResource(R.string.saturday_short),
        stringResource(R.string.sunday_short)
    )

    val dayStatus = List(daysOfWeek.size) { index ->
        val date = startOfWeek.plusDays(index.toLong())
        val epoch = date.toEpochDay()

        val instance = instanceMap[epoch]

        instance?.isCompleted()==true
    }

    WeeklyAnalytics(radius, dayStatus)
}

@Composable
fun TodoWeeklyProgressChart(
    radius: Int,
    instances: List<TodoInstance>,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val startOfWeek = today.with(DayOfWeek.MONDAY)
    val weekDays = remember(today) { (0..6).map { startOfWeek.plusDays(it.toLong()) } }
    val instanceMap = remember(instances) { instances.associateBy { it.instanceDate } }

    val percentages = weekDays.map { day ->

        val instance = instanceMap[day.toEpochDay()]
        if (instance == null || instance.items.isEmpty()) return@map 0f

        val completed = instance.items.count { it.isChecked }
        val total = instance.items.size
        (completed.toFloat() / total) * 100f
    }

    WeeklyProgressChart(
        radius = radius,
        modifier = modifier,
        percentages = percentages
    )
}

@Composable
fun MonthlyHabitAnalyticsCard(
    radius: Int,
    list: TodoModel,
    instances: List<TodoInstance>
) {
    val today = LocalDate.now()
    val currentYearMonth = YearMonth.of(today.year, today.month)
    val daysInMonth = currentYearMonth.lengthOfMonth()

    val instanceMap = remember(instances) {
        instances.associateBy { it.instanceDate }
    }

    val weekRanges = remember(daysInMonth) {
        val ranges = mutableListOf<IntRange>()
        var start = 1
        while (start <= daysInMonth) {
            val end = minOf(start + 6, daysInMonth)
            ranges.add(start..end)
            start = end + 1
        }
        ranges
    }

    val weekCounts = remember(instances, list) {
        val counts = MutableList(weekRanges.size) { 0 }

        for (day in 1..daysInMonth) {
            val date = currentYearMonth.atDay(day)
            val epoch = date.toEpochDay()
            val instance = instanceMap[epoch]
            val isCompleted = instance?.isCompleted() == true

            if (isCompleted) {
                weekRanges.forEachIndexed { index, range ->
                    if (day in range) {
                        counts[index]++
                        return@forEachIndexed
                    }
                }
            }
        }

        counts
    }

    val completedDays = weekCounts.sum()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        ),
        onClick = {}
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = stringResource(R.string.This_Month),
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = stringResource(R.string.completed_habits, completedDays),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            BarChart(
                weekCounts = weekCounts,
                weekLabels = weekRanges.map { "${it.first}-${it.last}" }
            )
        }
    }
}

@Composable
fun ItemConsistencyCard(
    radius: Int,
    list: TodoModel,
    instances: List<TodoInstance>,
    modifier: Modifier = Modifier
) {
    val consistencyList = remember(list.items, instances) {
        list.calculateItemConsistency(instances)
    }
    val (activeItems, removedItems) = remember(consistencyList) {
        consistencyList.partition { !it.isRemoved }
    }
    val primaryColor = MaterialTheme.colorScheme.primary

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
                stringResource(R.string.item_consistency),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 8.dp)
            )

            if (consistencyList.isEmpty()) {
                Text(
                    stringResource(R.string.no_data_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            } else {
                activeItems.forEach { item ->
                    ConsistencyRow(item = item, primaryColor = primaryColor)
                }

                if (removedItems.isNotEmpty()) {
                    Text(
                        stringResource(R.string.removed_items),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 4.dp)
                    )
                    removedItems.forEach { item ->
                        ConsistencyRow(
                            item = item,
                            primaryColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsistencyRow(item: ItemConsistency, primaryColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.isRemoved)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.width(8.dp))

        LinearProgressIndicator(
            progress = { item.percentage },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = primaryColor,
            trackColor = primaryColor.copy(alpha = 0.15f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${(item.percentage * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 36.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun TodoCalendarCard(
    radius: Int,
    list: TodoModel,
    instances: List<TodoInstance>,
    onTodoEvents: (TodoEvents) -> Unit
) {
    val context = LocalContext.current
    val startDateTime = list.startDateTime
    val recurrence = list.recurrence

    val todoStartMonth =
        Instant.ofEpochMilli(startDateTime).atZone(ZoneId.systemDefault()).toLocalDate()
            .let { YearMonth.of(it.year, it.month) }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val currentYearMonth = YearMonth.now()
    val endOfYear = YearMonth.of(currentYearMonth.year, 12)

    val canGoBack = currentMonth > todoStartMonth
    val canGoForward = currentMonth < endOfYear

    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7
    val dates = (1..daysInMonth).map { currentMonth.atDay(it) }

    val daysOfWeek = listOf(
        stringResource(R.string.sunday_short),
        stringResource(R.string.monday_short),
        stringResource(R.string.tuesday_short),
        stringResource(R.string.wednesday_short),
        stringResource(R.string.thursday_short),
        stringResource(R.string.friday_short),
        stringResource(R.string.saturday_short)
    )
    val today = LocalDate.now()
    val todoStartDate = Instant.ofEpochMilli(startDateTime).atZone(ZoneId.systemDefault()).toLocalDate()
    val locale = LocalLocale.current.platformLocale
    var selectedDate by remember { mutableStateOf(today) }
    val selectedInstance = remember(selectedDate, instances) {
        instances.find { it.instanceDate == selectedDate.toEpochDay() }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {},
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            // Month navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentMonth.month.getDisplayName(TextStyle.FULL, locale) +
                            " ${currentMonth.year}",
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    IconButton(
                        onClick = { if (canGoBack) currentMonth = currentMonth.minusMonths(1) },
                        enabled = canGoBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBackIos,
                            contentDescription = "Previous Month",
                            modifier = Modifier
                                .alpha(if (canGoBack) 0.8f else 0.3f)
                                .size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { if (canGoForward) currentMonth = currentMonth.plusMonths(1) },
                        enabled = canGoForward
                    ) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowForwardIos,
                            contentDescription = "Next Month",
                            modifier = Modifier
                                .alpha(if (canGoForward) 0.8f else 0.3f)
                                .size(16.dp)
                        )
                    }
                }
            }

            // Days of week row
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                daysOfWeek.forEach {
                    Text(
                        text = it,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            // Calendar grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max=330.dp)
            ) {
                // Empty cells before 1st day
                items(firstDayOfWeek) { Box(modifier = Modifier.size(32.dp)) }

                items(dates) { date ->
                    val epochDay = date.toEpochDay()
                    val instance = instances.find { it.instanceDate == epochDay }
                    val isMarked = instance?.isCompleted() ?: false
                    val isBeforeStart = epochDay < todoStartDate.toEpochDay()
                    val isAfterToday = epochDay > today.toEpochDay()
                    val isAllowedByRecurrence = isDateAllowedForHabit(recurrence, epochDay)

                    val backgroundColor = when {
                        isMarked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        isAllowedByRecurrence && !isBeforeStart -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else -> Color.Transparent
                    }

                    val textColor = when {
                        isMarked -> MaterialTheme.colorScheme.onPrimary
                        isAllowedByRecurrence && !isBeforeStart -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    }

                    val dateAlpha = when {
                        isBeforeStart -> 0.2f
                        isAfterToday -> 0.4f
                        !isAllowedByRecurrence -> 0.4f
                        else -> 1f
                    }

                    val cantMarkFutureDate = stringResource(R.string.cannot_mark_future_dates)
                    val habitStartsLater = stringResource(R.string.tracking_starts_later)
                    val notInSchedule = stringResource(R.string.date_not_in_schedule)

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(backgroundColor)
                            .alpha(dateAlpha)
                            .clickable {
                                when {
                                    isAfterToday -> Toast.makeText(
                                        context,
                                        cantMarkFutureDate,
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    isBeforeStart -> Toast.makeText(
                                        context,
                                        habitStartsLater,
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    !isAllowedByRecurrence -> {
                                        selectedDate=date
                                        Toast.makeText(
                                            context,
                                            notInSchedule,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    else -> {
                                        selectedDate = date
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(date.dayOfMonth.toString(), color = textColor)
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Checklist, null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.list_items), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Text(DateTimeFormatter.ofPattern("d MMM").format(selectedDate), style = MaterialTheme.typography.bodyMedium)
            }

            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (selectedInstance == null) {
                    item {
                        Text(
                            stringResource(R.string.no_checklist_for_date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(selectedInstance.items) { todoItem ->
                        MaterialListItem(true, todoItem) {
                            val updatedItems = selectedInstance.items.map {
                                if (it.id == todoItem.id)
                                    it.copy(isChecked = !it.isChecked)
                                else
                                    it
                            }

                            onTodoEvents(
                                TodoEvents.UpsertInstance(
                                    selectedInstance.copy(items = updatedItems)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
