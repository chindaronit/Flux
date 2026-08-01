package com.flux.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flux.R
import com.flux.data.model.WorkspaceModel
import com.flux.other.DataCopyType
import com.flux.other.icons
import com.flux.ui.screens.settings.CircleWrapper
import com.flux.ui.theme.FONTS
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.flux.data.model.SocialModel
import com.flux.ui.screens.events.getTextFieldColors
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    formatter.timeZone = TimeZone.getDefault()
    return formatter.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateOnlyPickerModal(
    initialSelectedDateMillis: Long = System.currentTimeMillis(),
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerInitialMillis =
        Instant.ofEpochMilli(initialSelectedDateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

    val state = rememberDatePickerState(
        initialSelectedDateMillis = pickerInitialMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val result = state.selectedDateMillis?.let { utcMillis ->
                        val localDate = Instant.ofEpochMilli(utcMillis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()

                        localDate
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    }

                    onDateSelected(result)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.Set))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.Cancel))
            }
        }
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    initialSelectedDateMillis: Long = System.currentTimeMillis(),
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis
    )

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
fun DataCopyDialog(
    workspaces: List<WorkspaceModel>,
    onConfirm: (DataCopyType, List<WorkspaceModel>) -> Unit,
    onDismiss: () -> Unit
){
    val selectedWorkspaces = remember { mutableStateListOf<WorkspaceModel>() }
    var selectedType by remember { mutableStateOf(DataCopyType.COPY) }

    AlertDialog(
        icon = {
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 0,
                        count = 2
                    ),
                    onClick = {
                        selectedType = DataCopyType.COPY
                        selectedWorkspaces.clear()
                    },
                    selected = selectedType == DataCopyType.COPY,
                    label = { Text(stringResource(R.string.copy)) }
                )

                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 1,
                        count = 2
                    ),
                    onClick = {
                        selectedType = DataCopyType.MOVE
                        selectedWorkspaces.clear()
                    },
                    selected = selectedType == DataCopyType.MOVE,
                    label = { Text(stringResource(R.string.move)) }
                )
            }
        },
        title = {
            Text(stringResource(R.string.select_workspaces))
        },
        text = {
            LazyColumn (Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)) {
                items(workspaces) { workspace->
                    val isChecked = selectedWorkspaces.contains(workspace)
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .padding(vertical = 2.dp)
                            .clickable {
                                if (isChecked) {
                                    selectedWorkspaces.remove(workspace)
                                } else {
                                    if(selectedType== DataCopyType.MOVE) if(selectedWorkspaces.isNotEmpty()) selectedWorkspaces.clear()
                                    selectedWorkspaces.add(workspace)
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
                                        icons[workspace.icon],
                                        null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Text(
                                    text = workspace.title,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    modifier = Modifier.width(150.dp)
                                )
                            }
                            Checkbox(checked = isChecked, onCheckedChange = {
                                if (isChecked) {
                                    selectedWorkspaces.remove(workspace)
                                } else {
                                    if(selectedType== DataCopyType.MOVE) if(selectedWorkspaces.isNotEmpty()) selectedWorkspaces.clear()
                                    selectedWorkspaces.add(workspace)
                                }
                            })
                        }
                    }
                }
            }

        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(selectedType, selectedWorkspaces.toList())
                onDismiss()
            }) {
                Text(stringResource(R.string.Confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.Dismiss))
            }
        }
    )
}

/** Captures a password string. Used for: initial setup, per-file import prompt, and password change. */
@Composable
fun BackupPasswordEntryDialog(
    title: String,
    onConfirm: (CharArray) -> Unit,
    onDismiss: (() -> Unit)?, // null = non-dismissible (used for the mandatory setup flow)
    supportingText: String? = null
) {
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        title = { Text(title) },
        text = {
            Column {
                supportingText?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.backup_password)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.backup_password_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = password.isNotEmpty(),
                onClick = {
                    val pw = password.toCharArray()
                    password = ""
                    onConfirm(pw)
                }
            ) { Text(stringResource(R.string.set_password)) }
        },
        dismissButton = onDismiss?.let {
            { TextButton(onClick = it) { Text(stringResource(R.string.Dismiss)) } }
        }
    )
}

/** Non-dismissible: fires whenever auto-backup is on but no password exists (migration, import, or live toggle). */
@Composable
fun AutoBackupNeedsPasswordDialog(
    onSetPasswordClick: () -> Unit,
    onTurnOffAutoBackup: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* force an explicit choice */ },
        title = { Text(stringResource(R.string.backup_password_setting)) },
        text = { Text(stringResource(R.string.auto_backup_requires_password)) },
        confirmButton = {
            TextButton(onClick = onSetPasswordClick) { Text(stringResource(R.string.set_password)) }
        },
        dismissButton = {
            TextButton(onClick = onTurnOffAutoBackup) { Text(stringResource(R.string.turn_off_auto_backup)) }
        }
    )
}

@Composable
fun AutoBackupNeedsPasswordInfoDialog(
    onNavigate: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { /* force an explicit choice */ },
        title = { Text(stringResource(R.string.backup_password_setting)) },
        text = { Text(stringResource(R.string.auto_backup_requires_password)) },
        confirmButton = {
            TextButton(onClick = onNavigate) { Text(stringResource(R.string.go_to_settings)) }
        },
        dismissButton = {  }
    )
}

/** Confirms the destructive consequence of rotating the password. */
@Composable
fun ChangePasswordWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.change_backup_password_title)) },
        text = { Text(stringResource(R.string.change_backup_password_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.Confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.Dismiss)) } }
    )
}

@Composable
fun SocialDialog(
    socialModel: SocialModel?=null,
    onConfirm: (SocialModel) -> Unit,
    onDismiss: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf(socialModel?.title?:"") }
    var link by rememberSaveable { mutableStateOf(socialModel?.link?:"") }
    var selectedCategory by rememberSaveable { mutableIntStateOf(socialModel?.category?:0) }
    val focusRequesterDesc = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Dialog(onDismissRequest = onDismiss){
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Add Social",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                SocialCategoryDropDown(selectedCategory) {
                    selectedCategory=it
                }

                TextField(
                    value = title,
                    onValueChange = { title = it },
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

                TextField(
                    value = link,
                    onValueChange = { link = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .focusRequester(focusRequesterDesc),
                    placeholder = { Text("Link") },
                    singleLine = true,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                    colors = getTextFieldColors(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onDismiss) {
                        Text(stringResource(R.string.Dismiss))
                    }

                    FilledTonalButton(
                        onClick = {
                            onConfirm(
                                SocialModel(
                                    title = title,
                                    link = link,
                                    category = selectedCategory
                                )
                            )
                            onDismiss()
                        }
                    ) { Text(stringResource(R.string.Confirm)) }
                }
            }
        }
    }
}

@Composable
fun SocialCategoryCard(
    title: String,
    cardContainerColor: Long,
    cardContentColor: Long,
    icon: Int,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier.clip(RoundedCornerShape(50))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
            containerColor = Color(cardContainerColor).copy(alpha = 0.75f),
            contentColor = Color(cardContentColor)
        )
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.size(24.dp),
                onClick = onClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(cardContainerColor)
                )
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Unspecified
                )
            }

            Spacer(Modifier.width(6.dp))

            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(cardContentColor)
            )

            Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
fun DiscardChangesDialog(
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
){
    AlertDialog(
        onDismissRequest = onDismiss, // tapping outside = keep editing
        title = { Text("Discard Changes?") }, // e.g. "Discard changes?"
        text = { Text("You\'ve made changes but haven\'t added a title. Add a title to save, or discard your changes.") }, // e.g. "You have unsaved changes. Add a title to save, or discard them."
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text("Discard")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep editing")
            }
        }
    )
}