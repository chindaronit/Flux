package com.flux.ui.screens.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.HabitModel
import com.flux.data.model.RecurrenceRule
import com.flux.ui.components.DatePickerModal
import com.flux.ui.components.TimePicker
import com.flux.ui.events.HabitEvents
import com.flux.ui.screens.events.getTextFieldColors
import com.flux.ui.screens.events.toFormattedDate
import com.flux.ui.screens.events.toFormattedTime
import com.flux.ui.state.Settings
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHabit(
    navController: NavController,
    habit: HabitModel,
    settings: Settings,
    onHabitEvents: (HabitEvents) -> Unit
){
    var newHabitTitle by remember { mutableStateOf(habit.title) }
    var newHabitDescription by remember { mutableStateOf(habit.description) }
    var newHabitTime by remember { mutableLongStateOf(habit.startDateTime) }
    var habitEndsOn by remember { mutableLongStateOf(habit.endDateTime) }
    var showDatePicker by remember { mutableStateOf(false) }
    var timePickerDialog by remember { mutableStateOf(false) }
    var neverEnds by remember { mutableStateOf(habit.endDateTime==-1L) }
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
    val context = LocalContext.current

    // Determine if this is creating a new habit or editing existing
    val isNewHabit = habit.title.isEmpty()
    val topBarTitle = if (isNewHabit) {
        stringResource(R.string.Add_Habit)
    } else {
        stringResource(R.string.Edit_Habit)
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
                            onHabitEvents(HabitEvents.UpsertHabit(
                                context,
                                habit.copy(
                                    title = newHabitTitle,
                                    description = newHabitDescription,
                                    startDateTime = newHabitTime,
                                    endDateTime = habitEndsOn,
                                    recurrence = RecurrenceRule.Weekly(selectedDays.toList())
                                )
                            ))
                        }
                    ) {
                        Icon(Icons.Default.Check, null)
                    }

                    if (!isNewHabit) {
                        IconButton({
                            navController.popBackStack()
                            onHabitEvents(HabitEvents.DeleteHabit(habit, context))
                        }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextField(
                value = newHabitTitle,
                onValueChange = { newHabitTitle = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.Title)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = getTextFieldColors(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusRequesterDesc.requestFocus() })
            )

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
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
            )

            HorizontalDivider()

            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween){
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AlarmAdd,
                        contentDescription = "Alarm Icon"
                    )
                    Text("Time")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(newHabitTime.toFormattedTime(settings.data.is24HourFormat))
                    FilledTonalIconButton({ timePickerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "Pick Time"
                        )
                    }
                }
            }

            HorizontalDivider()
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Repeat, null)
                Text("Repeat Habit")
            }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                weekdays.forEachIndexed { index, day ->
                    val isSelected = selectedDays.contains(index)
                    Card(
                        onClick = {
                            if (isSelected) {
                                // Prevent removing all days (must have at least one)
                                if (selectedDays.size > 1) { selectedDays.remove(index) }
                            } else { selectedDays.add(index) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                            contentColor = if(isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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

            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically){
                    Icon(Icons.Outlined.Flag, null)
                    Text("Never Ends")
                }

                Switch(neverEnds, onCheckedChange = {
                    if(neverEnds){
                        neverEnds=false
                        habitEndsOn=max(newHabitTime, System.currentTimeMillis())
                    }
                    else{
                        neverEnds=true
                        habitEndsOn=-1L
                    }
                })
            }

            if(!neverEnds){
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween){
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Today, null, modifier = Modifier.size(24.dp))
                        Text("Ends on")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(habitEndsOn.toFormattedDate())
                        FilledTonalIconButton({ showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.Create,
                                contentDescription = "Pick Time"
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            if (timePickerDialog) {
                TimePicker(
                    initialTime = newHabitTime,
                    is24Hour = settings.data.is24HourFormat,
                    onConfirm = { newHabitTime=it }
                ) { timePickerDialog = false }
            }

            if(showDatePicker){
                DatePickerModal(onDateSelected = { if(it!=null) habitEndsOn=it }){
                    showDatePicker=false
                }
            }
        }
    }
}