package com.flux.ui.screens.todo

import android.content.Context
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.TodoInstance
import com.flux.data.model.TodoItem
import com.flux.data.model.TodoModel
import com.flux.data.model.isCompleted
import com.flux.navigation.NavRoutes
import com.flux.ui.common.TimePicker
import com.flux.ui.common.convertMillisToDate
import com.flux.ui.common.convertMillisToTime
import com.flux.ui.events.TodoEvents
import com.flux.ui.screens.analytics.HeatMapCard
import com.flux.ui.screens.events.toFormattedTime
import com.flux.ui.screens.habits.HabitInfoComponent
import com.flux.ui.screens.settings.shapeManager
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
            containerColor = if (todoItem.isChecked) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        onClick = onToggleCheck
    ) {
        Row (verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onToggleCheck, colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (todoItem.isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (todoItem.isChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
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
                    Text("Turn on the reminder to remind about this to-do list!", modifier = Modifier
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

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 1000.dp)
                    .padding(vertical = 12.dp)
            ) {
                item {
                    HabitInfoComponent(
                        Icons.Default.Create,
                        "Created",
                        convertMillisToDate(list.startDateTime)
                    )
                }

                item {
                    HabitInfoComponent(
                        if(isReminderOn) Icons.Default.AlarmOn else Icons.Default.AlarmOff,
                        "Reminder",
                        if (isReminderOn) "On" else "off"
                    )
                }

                if(isReminderOn){
                    item {
                        HabitInfoComponent(
                            Icons.Default.Alarm,
                            "Remind at",
                            convertMillisToTime(list.startDateTime)
                        )
                    }

                    item {
                        HabitInfoComponent(
                            Icons.Default.DateRange,
                            "Scheduled",
                            if(isAllowedToday) "True" else "False"
                        )
                    }
                }

                if((isReminderOn && isAllowedToday) || !isReminderOn){
                    item {
                        HabitInfoComponent(
                            Icons.Outlined.CheckCircle,
                            "Completed",
                            completedNumber.toString()
                        )
                    }

                    item {
                        HabitInfoComponent(
                            Icons.Outlined.Circle,
                            "Remaining",
                            remainingNumber.toString()
                        )
                    }

                    item {
                        HabitInfoComponent(
                            Icons.Outlined.Percent,
                            "Completion",
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