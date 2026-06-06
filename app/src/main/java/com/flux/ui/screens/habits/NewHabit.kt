package com.flux.ui.screens.habits

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.HabitConfig
import com.flux.data.model.HabitModel
import com.flux.data.model.RecurrenceRule
import com.flux.ui.common.DatePickerModal
import com.flux.ui.common.TimePicker
import com.flux.ui.events.HabitEvents
import com.flux.ui.screens.events.getTextFieldColors
import com.flux.ui.screens.events.toFormattedDate
import com.flux.ui.screens.events.toFormattedTime
import com.flux.ui.state.Settings
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHabit(
    navController: NavController,
    habit: HabitModel,
    settings: Settings,
    onHabitEvents: (HabitEvents) -> Unit
) {
    val context = LocalContext.current
    var newHabitTitle by rememberSaveable { mutableStateOf(habit.title) }
    var newHabitDescription by rememberSaveable { mutableStateOf(habit.description) }
    var newHabitTime by rememberSaveable { mutableLongStateOf(habit.startDateTime) }
    var habitEndsOn by rememberSaveable { mutableLongStateOf(habit.endDateTime) }
    var newHabitConfig by remember { mutableStateOf(habit.habitConfig) }
    var showDatePicker by remember { mutableStateOf(false) }
    var neverEnds by rememberSaveable { mutableStateOf(habit.endDateTime == -1L) }
    val focusRequesterDesc = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val weekdays = listOf(
        stringResource(R.string.monday_short),
        stringResource(R.string.tuesday_short),
        stringResource(R.string.wednesday_short),
        stringResource(R.string.thursday_short),
        stringResource(R.string.friday_short),
        stringResource(R.string.saturday_short),
        stringResource(R.string.sunday_short)
    )
    // Initialize selectedDays from existing habit's recurrence
    val selectedDays = remember {
        mutableStateListOf<Int>().apply {
            addAll((habit.recurrence as RecurrenceRule.Weekly).daysOfWeek)
        }
    }

    // Determine if this is creating a new habit or editing existing
    val isNewHabit = habit.title.isEmpty()
    val topBarTitle = if (isNewHabit) {
        stringResource(R.string.Add_Habit)
    } else {
        stringResource(R.string.Edit_Habit)
    }

    // Cache per-type so toggling back restores original data
    var cachedCountedConfig by remember {
        mutableStateOf(habit.habitConfig as? HabitConfig.Counted ?: HabitConfig.Counted())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.surfaceContainerLow),
                title = { Text(topBarTitle) },
                navigationIcon = {
                    IconButton({ navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(
                        enabled = newHabitTitle.isNotBlank() && selectedDays.isNotEmpty(),
                        onClick = {
                            navController.popBackStack()
                            onHabitEvents(
                                HabitEvents.UpsertHabit(
                                    context,
                                    habit.copy(
                                        title = newHabitTitle,
                                        description = newHabitDescription,
                                        startDateTime = newHabitTime,
                                        endDateTime = habitEndsOn,
                                        recurrence = RecurrenceRule.Weekly(selectedDays.toList()),
                                        habitConfig = newHabitConfig
                                    )
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Default.Check, null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = 64.dp
                )
            ) {
                item {
                    TextField(
                        value = newHabitTitle,
                        onValueChange = { newHabitTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.Title)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        colors = getTextFieldColors(),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusRequesterDesc.requestFocus() })
                    )
                }

                item {
                    TextField(
                        value = newHabitDescription,
                        onValueChange = { newHabitDescription = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .focusRequester(focusRequesterDesc),
                        placeholder = { Text(stringResource(R.string.Description)) },
                        singleLine = true,
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                        colors = getTextFieldColors(),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                    )
                }

                item { HorizontalDivider() }

                item {
                    when (newHabitConfig) {
                        is HabitConfig.Simple -> {
                            SimpleConfigItems(
                                newHabitTime,
                                settings.data.is24HourFormat
                            ) { habitTime, newConfig ->
                                newHabitTime = habitTime
                                newHabitConfig = newConfig
                            }
                        }

                        is HabitConfig.Counted -> {
                            CountedConfigItems(
                                settings.data.is24HourFormat,
                                newHabitConfig as HabitConfig.Counted
                            ) { newHabitConfig = it }
                        }

                        else -> {
//
//                            TimedConfigItems(
//                                newHabitTime,
//                                newHabitConfig as HabitConfig.Timed,
//                                settings.data.is24HourFormat
//                            ) { habitTime, newConfig ->
//                                newHabitTime = habitTime
//                                newHabitConfig = newConfig
//                            }
                        }
                    }
                }


                item { HorizontalDivider() }

                item {
                    Row(
                        Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Repeat, null)
                        Text(stringResource(R.string.repeat))
                    }
                }

                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        weekdays.forEachIndexed { index, day ->
                            val isSelected = selectedDays.contains(index)
                            Card(
                                onClick = {
                                    if (isSelected) {
                                        // Prevent removing all days (must have at least one)
                                        if (selectedDays.size > 1) {
                                            selectedDays.remove(index)
                                        }
                                    } else {
                                        selectedDays.add(index)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceColorAtElevation(
                                        6.dp
                                    ),
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(
                                    text = day,
                                    modifier = Modifier.padding(6.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                item { HorizontalDivider() }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Flag, null)
                            Text(stringResource(R.string.never_ends))
                        }

                        Switch(neverEnds, onCheckedChange = {
                            if (neverEnds) {
                                neverEnds = false
                                habitEndsOn =
                                    max(newHabitTime, System.currentTimeMillis())
                            } else {
                                neverEnds = true
                                habitEndsOn = -1L
                            }
                        })
                    }
                }

                item {
                    if (!neverEnds) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Today,
                                    null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(stringResource(R.string.ends_on))
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(habitEndsOn.toFormattedDate())
                                FilledTonalIconButton({ showDatePicker = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Create,
                                        contentDescription = "Pick Time"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HabitConfigRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                config = newHabitConfig,
                onChangeConfig = { incoming ->
                    // Persist the current config before switching away
                    when (val current = newHabitConfig) {
                        is HabitConfig.Counted -> cachedCountedConfig = current
                        else -> Unit
                    }
                    // Restore from cache when switching back to a known type
                    newHabitConfig = when (incoming) {
                        is HabitConfig.Counted -> cachedCountedConfig
                        else -> incoming
                    }
                }
            )
        }
    }

    if (showDatePicker) {
        DatePickerModal(onDateSelected = {
            if (it != null)
                habitEndsOn = LocalDate
                    .ofEpochDay(it / 86_400_000)
                    .atTime(LocalTime.MAX)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
        }) {
            showDatePicker = false
        }
    }
}

@Composable
fun SimpleConfigItems(startDateTime: Long, is24HourFormat: Boolean, onClick: (Long, HabitConfig.Simple) -> Unit) {
    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        TimePicker(
            initialTime = startDateTime,
            is24Hour = is24HourFormat,
            onConfirm = { onClick(it, HabitConfig.Simple) }
        ) { showTimePicker = false }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AlarmAdd, null)
            Text(stringResource(R.string.reminder_time))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(startDateTime.toFormattedTime(is24HourFormat))
            FilledTonalIconButton({ showTimePicker = true }) {
                Icon(Icons.Default.Create, null)
            }
        }
    }
}

@Composable
fun CountedConfigItems(
    is24HourFormat: Boolean,
    config: HabitConfig.Counted,
    onChange: (HabitConfig.Counted) -> Unit
){
    var goal by remember { mutableIntStateOf(config.goal) }
    var unit by remember { mutableStateOf(config.unit) }
    var intervalMillis by remember { mutableLongStateOf(config.intervalMillis) }
    var activeStartTime by remember { mutableLongStateOf(config.activeStartTime) }
    var activeEndTime by remember { mutableLongStateOf(config.activeEndTime) }
    var showIntervalTimer by remember { mutableStateOf(false) }
    var showActiveTimePicker by remember { mutableStateOf(false) }
    var isChangingActiveStartTime by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val toastLabel = stringResource(R.string.end_time_greater_than_start)

    val count = remember(activeStartTime, activeEndTime, intervalMillis) {
        if (intervalMillis > 0) { ((activeEndTime - activeStartTime) / intervalMillis) + 1 } else 1
    }

    if (showIntervalTimer) {
        TimerDialog(
            durationMillis = intervalMillis,
            onDismiss = { showIntervalTimer = false }
        ) { duration ->
            intervalMillis = duration
            onChange(config.copy(intervalMillis = duration))
        }
    }

    if (showActiveTimePicker) {
        TimePicker(
            initialTime = if(isChangingActiveStartTime) activeStartTime else activeEndTime,
            is24Hour = is24HourFormat,
            onConfirm = {
                if(isChangingActiveStartTime) {
                    onChange(config.copy(activeStartTime = it))
                    activeStartTime = it
                    if(activeEndTime<it){
                        activeEndTime=it
                    }
                }
                else {
                    if(it<activeStartTime){
                        Toast.makeText(context, toastLabel, Toast.LENGTH_SHORT).show()
                    }
                    else {
                        onChange(config.copy(activeEndTime = it))
                        activeEndTime = it
                    }
                }
            }
        ) { showActiveTimePicker = false }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.TrackChanges, null)
                Text(stringResource(R.string.goal))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    {
                        goal -=1
                        onChange(config.copy(goal = goal))
                    },
                    enabled = goal>2,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Remove, null)
                }
                Text(goal.toString())
                IconButton(
                    {
                        goal+=1
                        onChange(config.copy(goal = goal))
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AcUnit, null)
                Text(stringResource(R.string.unit_optional))
            }

            TextField(
                value = unit,
                onValueChange = {
                    unit = it
                    onChange(config.copy(unit=it)) },
                singleLine = true,
                modifier = Modifier.width(100.dp),
                colors = getTextFieldColors(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center)
            )
        }

        HorizontalDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Circle, null)
                Text(stringResource(R.string.from))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(activeStartTime.toFormattedTime(is24HourFormat))
                FilledTonalIconButton({
                    isChangingActiveStartTime=true
                    showActiveTimePicker = true
                }) {
                    Icon(Icons.Default.Create, null)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.StopCircle, null)
                Text(stringResource(R.string.to))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(activeEndTime.toFormattedTime(is24HourFormat))
                FilledTonalIconButton({
                    isChangingActiveStartTime=false
                    showActiveTimePicker = true
                }) { Icon(Icons.Default.Create, null) }
            }
        }

        HorizontalDivider()

        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.NotificationsActive, null)
                Text(stringResource(R.string.remind_every))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(formatDuration(intervalMillis))
                FilledTonalIconButton({ showIntervalTimer = true }) {
                    Icon(Icons.Default.Create, null)
                }
            }
        }

        Card(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
        ){
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(stringResource(R.string.habit_reminder_count, count))
            }
        }
    }
}

@Composable
fun TimedConfigItems(
    startDateTime: Long,
    config: HabitConfig.Timed,
    is24HourFormat: Boolean,
    onChange: (Long, HabitConfig.Timed)->Unit
){
    var newStartDateTime by remember { mutableLongStateOf(startDateTime) }
    var durationMillis by remember { mutableLongStateOf(config.durationMillis) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }

    if (showDurationPicker) {
        TimerDialog(
            durationMillis = durationMillis,
            onDismiss = { showDurationPicker = false }
        ) { duration ->
            durationMillis = duration
            onChange(newStartDateTime, config.copy(durationMillis = duration))
        }
    }

    if (showTimePicker) {
        TimePicker(
            initialTime = newStartDateTime,
            is24Hour = is24HourFormat,
            onConfirm = {
                newStartDateTime = it
                onChange(it, config.copy(durationMillis = durationMillis))
            }
        ) { showTimePicker = false }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AlarmAdd, null)
                Text(stringResource(R.string.reminder_time))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(startDateTime.toFormattedTime(is24HourFormat))
                FilledTonalIconButton({ showTimePicker = true }) {
                    Icon(Icons.Default.Create, null)
                }
            }
        }
        HorizontalDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Timer, null)
                Text(stringResource(R.string.duration))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(formatDuration(durationMillis))
                FilledTonalIconButton({ showDurationPicker = true }) {
                    Icon(Icons.Default.Create, null)
                }
            }
        }
    }
}

fun formatDuration(durationMillis: Long): String {
    val totalMinutes = durationMillis / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}