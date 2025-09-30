package com.flux.ui.screens.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.flux.ui.components.TimePicker
import com.flux.ui.events.HabitEvents
import com.flux.ui.screens.events.getTextFieldColors
import com.flux.ui.screens.events.toFormattedTime
import com.flux.ui.state.Settings

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
    var timePickerDialog by remember { mutableStateOf(false) }
    val focusRequesterDesc = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

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
                    .focusRequester(focusRequesterDesc),
                placeholder = { Text(stringResource(R.string.Description)) },
                singleLine = true,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                colors = getTextFieldColors(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
            )

            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Repeat, null)
                Text("Repeat", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AlarmAdd,
                        contentDescription = "Alarm Icon"
                    )

                    Text(
                        text = newHabitTime.toFormattedTime(settings.data.is24HourFormat),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                FilledTonalIconButton(
                    onClick = { timePickerDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = "Pick Time"
                    )
                }
            }

            if (timePickerDialog) {
                TimePicker(
                    initialTime = newHabitTime,
                    is24Hour = settings.data.is24HourFormat,
                    onConfirm = { newHabitTime = it }
                ) { timePickerDialog = false }
            }
        }
    }
}