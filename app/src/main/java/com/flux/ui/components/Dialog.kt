package com.flux.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.LabelImportant
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flux.R
import com.flux.data.model.LabelModel
import com.flux.data.model.Repetition
import com.flux.data.model.WorkspaceModel
import com.flux.ui.screens.events.getRepetitionText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AddLabelDialog(
    initialValue: String,
    onConfirmation: (String)->Unit,
    onDismissRequest: ()-> Unit
){
    var label by remember { mutableStateOf(initialValue) }

    AlertDialog(
        icon = { CircleWrapper(MaterialTheme.colorScheme.primary) { val icon=if(initialValue.isBlank()) Icons.Default.Add else Icons.Default.Edit
            Icon(icon, contentDescription = "Add/Edit Icon", tint = MaterialTheme.colorScheme.onPrimary) } },
        title = { Text(text = if(initialValue.isBlank()) stringResource(R.string.Add_Label) else stringResource(R.string.Edit_Label)) },
        text = { OutlinedTextField(value = label, onValueChange = { label=it }, singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    onConfirmation(label)
                    onDismissRequest()
                }
            )) },
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(
                onClick = { onConfirmation(label)
                    onDismissRequest() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) { Text(stringResource(R.string.Confirm)) }
        },
        dismissButton = { TextButton(onClick = { onDismissRequest() }) { Text(stringResource(R.string.Dismiss)) } }
    )
}

@Composable
fun AddSpacesDialog(
    workspace: WorkspaceModel,
    onDismissRequest: () -> Unit,
    onConfirmation: (WorkspaceModel) -> Unit
) {
    var notesChecked by remember { mutableStateOf(false) }
    var todoChecked by remember { mutableStateOf(false) }
    var calendarChecked by remember { mutableStateOf(false) }
    var habitsChecked by remember { mutableStateOf(false) }
    var eventsChecked by remember { mutableStateOf(false) }
    var analyticsChecked by remember { mutableStateOf(false) }
    var journalChecked by remember { mutableStateOf(false) }

    AlertDialog(
        icon = {
            CircleWrapper(MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Label Icon", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        title = { Text(text = stringResource(R.string.Add_Spaces)) },
        text = {
            LazyColumn(Modifier.heightIn(max=250.dp)) {
                if (!workspace.isNotesAdded) {
                    item {
                        SpaceCheckboxCard(Icons.AutoMirrored.Default.Notes, stringResource(R.string.Notes), notesChecked) {
                            notesChecked = it
                        }
                    }
                }
                if (!workspace.isJournalAdded) {
                    item {
                        SpaceCheckboxCard(Icons.Default.AutoStories, stringResource(R.string.Journal), journalChecked) {
                            journalChecked=it
                        }
                    }
                }
                if (!workspace.isTodoAdded) {
                    item {
                        SpaceCheckboxCard(Icons.Default.Checklist, stringResource(R.string.To_Do), todoChecked) {
                            todoChecked = it
                        }
                    }
                }
                if (!workspace.isEventsAdded) {
                    item {
                        SpaceCheckboxCard(Icons.Default.Event,  stringResource(R.string.Events), eventsChecked) {
                            eventsChecked = it
                        }
                    }
                }
                if (!workspace.isCalenderAdded) {
                    item {
                        SpaceCheckboxCard(Icons.Default.CalendarMonth, stringResource(R.string.Calender), calendarChecked) {
                            calendarChecked = it
                        }
                    }
                }
                if (!workspace.isHabitsAdded) {
                    item {
                        SpaceCheckboxCard(Icons.Default.EventAvailable, stringResource(R.string.Habits), habitsChecked) {
                            habitsChecked = it
                        }
                    }
                }
                if (!workspace.isAnalyticsAdded) {
                    item {
                        SpaceCheckboxCard(Icons.Default.Analytics, stringResource(R.string.Analytics), analyticsChecked) {
                            analyticsChecked=it
                        }
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val updated = workspace.copy(
                        isNotesAdded = workspace.isNotesAdded || notesChecked,
                        isTodoAdded = workspace.isTodoAdded || todoChecked,
                        isCalenderAdded = workspace.isCalenderAdded || calendarChecked,
                        isHabitsAdded = workspace.isHabitsAdded || habitsChecked,
                        isEventsAdded = workspace.isEventsAdded || eventsChecked,
                        isAnalyticsAdded = workspace.isAnalyticsAdded || analyticsChecked,
                        isJournalAdded = workspace.isJournalAdded || journalChecked
                    )
                    onConfirmation(updated)
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.Confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.Dismiss))
            }
        }
    )
}

@Composable
fun SpaceCheckboxCard(
    icon: ImageVector,
    title: String,
    isChecked: Boolean,
    onCheckChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckChange(!isChecked) },
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircleWrapper(color = MaterialTheme.colorScheme.primary) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                Text(title, fontWeight = FontWeight.Bold)
            }

            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckChange
            )
        }
    }
}

@Composable
fun SelectLabelDialog(
    currNoteLabels: List<LabelModel>,
    labels: List<LabelModel>,
    onAddLabel: () -> Unit,
    onConfirmation: (List<LabelModel>)->Unit,
    onDismissRequest: ()-> Unit
){
    val selectedLabel = remember { mutableStateListOf<LabelModel>().apply {
        addAll(currNoteLabels)
    } }

    AlertDialog(
        icon = { CircleWrapper(MaterialTheme.colorScheme.primary) { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Label Icon", tint = MaterialTheme.colorScheme.onPrimary) } },
        title = { Text(text = stringResource(R.string.Select_radius)) },
        text = { LabelCheckBoxList(selectedLabel, labels, onChecked = {selectedLabel.add(it)}, onAddLabel = {
            onAddLabel()
            onDismissRequest()
        }, onUnChecked = { selectedLabel.remove(it)} )  },
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation(selectedLabel)
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.Confirm))
            }
        },
        dismissButton = { TextButton(onClick = { onDismissRequest() }) { Text(stringResource(R.string.Dismiss)) } }
    )
}

@Composable
fun LabelCheckBoxList(
    checkedLabel: List<LabelModel>,
    labels: List<LabelModel>,
    onChecked: (LabelModel)-> Unit,
    onUnChecked: (LabelModel) -> Unit,
    onAddLabel: ()->Unit
){

    LazyColumn(
        modifier = Modifier.heightIn(max=250.dp)
    ) {
        item {
            Card(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .padding(vertical = 2.dp).clickable(onClick = onAddLabel),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircleWrapper(color = MaterialTheme.colorScheme.primary) {
                            Icon(Icons.Default.NewLabel, null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Text(text = stringResource(R.string.Add_Label), fontWeight = FontWeight.Bold)
                    }
                    IconButton(onAddLabel) { }
                }
            }
        }
        items(labels){ label->
            val isChecked=checkedLabel.contains(label)
            Card(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .padding(vertical = 2.dp).clickable {
                        if (isChecked) {
                            onUnChecked(label)
                        } else {
                            onChecked(label)
                        }
                    },
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    Modifier.padding(vertical = 6.dp, horizontal = 8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircleWrapper(color = MaterialTheme.colorScheme.primary) {
                            Icon(Icons.AutoMirrored.Outlined.LabelImportant, null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Text(text = label.value, fontWeight = FontWeight.Bold, overflow = TextOverflow.Ellipsis, maxLines = 1, modifier = Modifier.width(150.dp))
                    }
                    Checkbox(checked = isChecked, onCheckedChange = {
                        if(isChecked) { onUnChecked(label) }
                        else { onChecked(label) }
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text(stringResource(R.string.Set))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.Cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

fun convertMillisToTime(millis: Long): String {
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return formatter.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePicker(
    initialTime: Long,
    onConfirm: (TimePickerState) -> Unit,
    onDismiss: () -> Unit,
) {

    val currentTime = Calendar.getInstance().apply { timeInMillis=initialTime}

    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = false,
    )

    var showDial by remember { mutableStateOf(true) }

    /** The icon used for the icon button that switches from dial to input */
    val toggleIcon = if (showDial) {
        Icons.Filled.EditCalendar
    } else {
        Icons.Filled.AccessTime
    }

    TimePickerDialog(
        onDismiss = { onDismiss() },
        onConfirm = { onConfirm(timePickerState) },
        toggle = {
            IconButton(onClick = { showDial = !showDial }) {
                Icon(
                    imageVector = toggleIcon,
                    contentDescription = "Time picker type toggle",
                )
            }
        },
    ) {
        if (showDial) {
            TimePicker(
                state = timePickerState,
            )
        } else {
            TimeInput(
                state = timePickerState,
            )
        }
    }
}

@Composable
fun TimePickerDialog(
    title: String = stringResource(R.string.Select_Time),
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    toggle: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier =
                Modifier
                    .width(IntrinsicSize.Min)
                    .height(IntrinsicSize.Min)
                    .background(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surface
                    ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.labelMedium
                )
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    toggle()
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.Cancel)) }
                    TextButton(onClick = { onConfirm()
                    onDismiss()}) { Text(stringResource(R.string.Set)) }
                }
            }
        }
    }
}

@Composable
fun TaskRepetitionDialog(
    repeatPeriod: Repetition,
    onChange: (Repetition) -> Unit,
    onDismissRequest: () -> Unit
) {
    val options = listOf(Repetition.NONE, Repetition.DAILY, Repetition.WEEKLY, Repetition.MONTHLY, Repetition.YEARLY)

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
                        Icons.Default.Repeat,
                        contentDescription = "Repeat Icon",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.Repeat_Task),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.height(16.dp))

                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (repeatPeriod != option) {
                                    onChange(option)
                                }
                                onDismissRequest()
                            }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(getRepetitionText(option))
                        RadioButton(
                            selected = repeatPeriod == option,
                            onClick = {
                                if (repeatPeriod != option) {
                                    onChange(option)
                                }
                                onDismissRequest()
                            }
                        )
                    }
                }
            }
        }
    }
}

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
        10L to stringResource(R.string.ten_minutes_before),
        15L to stringResource(R.string.fifteen_minutes_before),
        30L to stringResource(R.string.thirty_minutes_before),
        60L to stringResource(R.string.one_hour_before)
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
fun SetPasskeyDialog(onConfirmRequest: (String)->Unit, onDismissRequest: () -> Unit){
    var passKey by remember { mutableStateOf("") }

    Dialog(onDismissRequest) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.enterPasskey), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = passKey,
                    singleLine = true,
                    onValueChange = { passKey=it },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onConfirmRequest(passKey)
                            onDismissRequest()
                        }
                    )
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onDismissRequest) { Text(stringResource(R.string.Dismiss)) }
                    TextButton(
                        onClick = {
                            onConfirmRequest(passKey)
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors()
                    ) { Text(stringResource(R.string.Confirm)) }
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
    val timeUnits = listOf(stringResource(R.string.minutes), stringResource(R.string.hours), stringResource(R.string.days))
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
        Card(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.Custom_Notification), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                timeUnits.forEach { unit ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedUnit == unit,
                                onClick = { selectedUnit = unit }
                            )
                            .padding(8.dp),
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
                    TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.Custom)) }
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
fun DeleteAlert(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String=stringResource(R.string.deleteDialogTitle),
    dialogText: String=stringResource(R.string.deleteDialogText),
    icon: ImageVector=Icons.Default.DeleteOutline,
) {
    AlertDialog(
        icon = { Icon(icon, contentDescription = "Delete Icon", tint = MaterialTheme.colorScheme.error) },
        title = { Text(text = dialogTitle) },
        text = { Text(text = dialogText) },
        onDismissRequest = { onDismissRequest() },
        confirmButton = { TextButton(onClick = { onConfirmation() }) { Text(stringResource(R.string.Confirm)) } },
        dismissButton = { TextButton(onClick = { onDismissRequest() }) { Text(stringResource(R.string.Dismiss)) } }
    )
}
