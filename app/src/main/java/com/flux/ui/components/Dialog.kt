package com.flux.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.LabelImportant
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import com.flux.other.ExportType
import com.flux.ui.theme.FONTS
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AddLabelDialog(
    initialValue: String,
    onConfirmation: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var label by remember { mutableStateOf(initialValue) }

    AlertDialog(
        icon = {
            CircleWrapper(MaterialTheme.colorScheme.primary) {
                val icon = if (initialValue.isBlank()) Icons.Default.Add else Icons.Default.Edit
                Icon(
                    icon,
                    contentDescription = "Add/Edit Icon",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        title = {
            Text(
                text = if (initialValue.isBlank()) stringResource(R.string.Add_Label) else stringResource(
                    R.string.Edit_Label
                )
            )
        },
        text = {
            OutlinedTextField(
                value = label, onValueChange = { label = it }, singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onConfirmation(label)
                        onDismissRequest()
                    }
                ))
        },
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation(label)
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text(stringResource(R.string.Confirm)) }
        },
        dismissButton = { TextButton(onClick = { onDismissRequest() }) { Text(stringResource(R.string.Dismiss)) } }
    )
}

@Composable
fun SelectLabelDialog(
    currNoteLabels: List<LabelModel>,
    labels: List<LabelModel>,
    onAddLabel: () -> Unit,
    onConfirmation: (List<LabelModel>) -> Unit,
    onDismissRequest: () -> Unit
) {
    val selectedLabel = remember {
        mutableStateListOf<LabelModel>().apply {
            addAll(currNoteLabels)
        }
    }

    AlertDialog(
        icon = {
            CircleWrapper(MaterialTheme.colorScheme.primary) {
                Icon(
                    Icons.AutoMirrored.Filled.Label,
                    contentDescription = "Label Icon",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        title = { Text(text = stringResource(R.string.Select_Label)) },
        text = {
            LabelCheckBoxList(
                selectedLabel,
                labels,
                onChecked = { selectedLabel.add(it) },
                onAddLabel = {
                    onAddLabel()
                    onDismissRequest()
                },
                onUnChecked = { selectedLabel.remove(it) })
        },
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
    onChecked: (LabelModel) -> Unit,
    onUnChecked: (LabelModel) -> Unit,
    onAddLabel: () -> Unit
) {

    LazyColumn(
        modifier = Modifier.heightIn(max = 250.dp)
    ) {
        item {
            Card(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .padding(vertical = 2.dp)
                    .clickable(onClick = onAddLabel),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircleWrapper(color = MaterialTheme.colorScheme.primary) {
                            Icon(
                                Icons.Default.NewLabel,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = stringResource(R.string.Add_Label),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onAddLabel) { }
                }
            }
        }
        items(labels) { label ->
            val isChecked = checkedLabel.contains(label)
            Card(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .padding(vertical = 2.dp)
                    .clickable {
                        if (isChecked) {
                            onUnChecked(label)
                        } else {
                            onChecked(label)
                        }
                    },
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    Modifier
                        .padding(vertical = 6.dp, horizontal = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircleWrapper(color = MaterialTheme.colorScheme.primary) {
                            Icon(
                                Icons.AutoMirrored.Outlined.LabelImportant,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = label.value,
                            fontWeight = FontWeight.Bold,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            modifier = Modifier.width(150.dp)
                        )
                    }
                    Checkbox(checked = isChecked, onCheckedChange = {
                        if (isChecked) {
                            onUnChecked(label)
                        } else {
                            onChecked(label)
                        }
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

fun convertMillisToTime(millis: Long, is24Hour: Boolean = false): String {
    val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePicker(
    initialTime: Long,
    is24Hour: Boolean = false,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentTime = Calendar.getInstance().apply { timeInMillis = initialTime }
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = is24Hour,
    )
    var showDial by remember { mutableStateOf(true) }
    val toggleIcon = if (showDial) { Icons.Filled.EditCalendar } else { Icons.Filled.AccessTime }

    TimePickerDialog(
        onDismiss = onDismiss,
        onConfirm = {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = initialTime
                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                set(Calendar.MINUTE, timePickerState.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onConfirm(calendar.timeInMillis)
            onDismiss()
        },
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
            TimePicker(timePickerState)
        } else {
            TimeInput(timePickerState)
        }
    }
}

// Remove the onDismiss() call from TimePickerDialog's onConfirm button
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
                    TextButton(onClick = onConfirm) { Text(stringResource(R.string.Set)) }
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
        30L to stringResource(R.string.thirty_minutes_before)
    )

    // Convert to minutes for comparison
    val currentMinutes = currentOffset / 1000 / 60

    Dialog(onDismissRequest = onDismissRequest) {
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
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
fun SetPasskeyDialog(onConfirmRequest: (String) -> Unit, onDismissRequest: () -> Unit) {
    var passKey by remember { mutableStateOf("") }

    Dialog(onDismissRequest) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.enterPasskey),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = passKey,
                    singleLine = true,
                    onValueChange = { passKey = it },
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
fun DeleteAlert(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String = stringResource(R.string.deleteDialogTitle),
    dialogText: String = stringResource(R.string.deleteDialogText),
    icon: ImageVector = Icons.Default.DeleteOutline,
) {
    AlertDialog(
        icon = {
            Icon(
                icon,
                contentDescription = "Delete Icon",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(text = dialogTitle) },
        text = { Text(text = dialogText) },
        onDismissRequest = { onDismissRequest() },
        confirmButton = { TextButton(onClick = { onConfirmation() }) { Text(stringResource(R.string.Confirm)) } },
        dismissButton = { TextButton(onClick = { onDismissRequest() }) { Text(stringResource(R.string.Dismiss)) } }
    )
}

@Composable
fun ShareNoteDialog(
    isSharing: Boolean,
    onConfirm: (ExportType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if(isSharing) "Share as" else stringResource(R.string.Export_As),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Text Export
                Card(
                    onClick = { onConfirm(ExportType.TXT) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TextFields, null)
                        Text( stringResource(R.string.Plain_Text))
                    }
                }

                // Markdown Export
                Card(
                    onClick = { onConfirm(ExportType.MARKDOWN) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FilePresent, null)
                        Text( stringResource(R.string.Markdown))
                    }
                }

                // Image Export
                Card(
                    onClick = { onConfirm(ExportType.IMAGE) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, null)
                        Text( "Image")
                    }
                }

                // HTML Export
                Card(
                    onClick = { onConfirm(ExportType.HTML) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, null)
                        Text( "HTML")
                    }
                }
            }
        }
    }
}

@Composable
fun FontDialog(
    selectedFont: Int,
    onSelectFont: (Int)->Unit,
    onDismissRequest: () -> Unit
){
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.FontDownload, null, modifier = Modifier.size(36.dp))
                Text(stringResource(R.string.Fonts), style = MaterialTheme.typography.titleLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FONTS.forEachIndexed { index, font ->
                        val containerColor =
                            if (selectedFont == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                        val contentColor =
                            if (selectedFont == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        Card(
                            onClick = {
                                onSelectFont(index)
                                onDismissRequest()
                            },
                            shape =
                                if (selectedFont == index) RoundedCornerShape(50)
                                else RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = containerColor,
                                contentColor = contentColor
                            )
                        ) {
                            Text(font, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LinkDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (name: String, uri: String) -> Unit
) {

    var name by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }

    val linkError by remember {
        derivedStateOf {
            if (link.isNotEmpty()) {
                // Email is considered erroneous until it completely matches EMAIL_ADDRESS.
                !android.util.Patterns.WEB_URL.matcher(link).matches()
            } else {
                false
            }
        }
    }

    AlertDialog(
        title = { Text("Link") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    singleLine = true,
                    isError = linkError,
                    label = { Text("Url") },
                    placeholder = { Text("https://www.google.com") },
                    supportingText = { if (linkError) { Text(text = "Incorrect Text") } },
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = true,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    if (!linkError) {
                        name = name.trim()
                        link = link.trim()
                        if (!link.startsWith("http://") && !link.startsWith("https://")
                            && link.startsWith("www.")
                        ) {
                            link = "https://$link"
                        }
                        onConfirm(name, link)
                        onDismissRequest()
                    }
                }
            ) {
                Text(stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    )
}

@Composable
fun TableDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (row: Int, column: Int) -> Unit
) {

    var row by remember { mutableStateOf("") }
    var column by remember { mutableStateOf("") }

    var rowError by remember { mutableStateOf(false) }
    var columnError by remember { mutableStateOf(false) }

    AlertDialog(
        title = { Text("Table") },
        text = {
            Column {
                OutlinedTextField(
                    value = row,
                    onValueChange = {
                        row = if (it.length > 3) it.substring(0, 3) else it
                        rowError = !row.all { char -> char.isDigit() }
                    },
                    isError = rowError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    label = { Text("Row") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = column,
                    onValueChange = {
                        column = if (it.length > 3) it.substring(0, 3) else it
                        columnError = !column.all { char -> char.isDigit() }
                    },
                    isError = columnError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    label = { Text("Column") }
                )
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    if (row.isBlank()) { rowError = true }
                    if (column.isBlank()) { columnError = true }
                    if (!rowError && !columnError) {
                        row = row.trim()
                        column = column.trim()
                        onConfirm(row.toInt(), column.toInt())
                        onDismissRequest()
                    }
                }
            ) {
                Text(stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(text = stringResource(id = android.R.string.cancel)) } }
    )
}

@Composable
fun ListDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (list: List<String>) -> Unit
) {

    val list = remember { mutableStateListOf("") }
    var ordered by remember { mutableStateOf(false) }

    AlertDialog(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("List")

                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        onClick = {
                            ordered = false
                        },
                        selected = !ordered
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                                contentDescription = "Unordered"
                            )
                        }
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        onClick = {
                            ordered = true
                        },
                        selected = ordered
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FormatListNumbered,
                                contentDescription = "Ordered"
                            )
                        }
                    }
                }

                FilledTonalIconButton(onClick = {
                    list.add("")
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Add list item"
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                list.forEachIndexed { index, str ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = str,
                            onValueChange = {
                                list[index] = it
                            },
                            prefix = { Text(text = if (ordered) "${(index + 1)}. " else "- ") },
                            singleLine = true
                        )

                        IconButton(onClick = { list.removeAt(index) }) {
                            Icon(
                                Icons.Outlined.RemoveCircleOutline,
                                contentDescription = "Remove list item"
                            )
                        }
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    val result = list.mapIndexed { index, it -> if (ordered) "$index. $it" else "- $it" }
                    onConfirm(result)
                    onDismissRequest()
                }
            ) { Text(stringResource(id = android.R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(text = stringResource(id = android.R.string.cancel)) } }
    )
}

@Serializable
data class TaskItem(
    var task: String = "",
    var checked: Boolean = false
)

@Composable
fun TaskDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (taskList: List<TaskItem>) -> Unit
) {

    val taskList = remember {
        mutableStateListOf(TaskItem("", false))
    }

    AlertDialog(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Task")

                FilledTonalIconButton(onClick = {
                    taskList.add(TaskItem("", false))
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Add Task"
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                taskList.forEachIndexed { index, taskState ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Checkbox(
                            checked = taskState.checked,
                            onCheckedChange = {
                                taskList[index] = taskList[index].copy(checked = it)
                            }
                        )

                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = taskState.task,
                            onValueChange = {
                                taskList[index] = taskList[index].copy(task = it)
                            },
                            singleLine = true
                        )

                        IconButton(onClick = { taskList.removeAt(index) }) {
                            Icon(
                                Icons.Outlined.RemoveCircleOutline,
                                contentDescription = "Remove Task"
                            )
                        }
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(taskList)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    )
}