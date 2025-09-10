package com.flux.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.flux.R
import com.flux.data.model.HabitModel
import com.flux.data.model.Space
import com.flux.data.model.WorkspaceModel
import com.flux.data.model.getSpacesList
import com.flux.other.icons
import com.flux.other.workspaceIconList
import com.flux.ui.screens.events.toFormattedTime
import com.flux.ui.state.Settings
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitBottomSheet(
    isEditing: Boolean = false,
    habit: HabitModel? = null,
    isVisible: Boolean,
    sheetState: SheetState,
    settings: Settings,
    onConfirm: (HabitModel, Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (!isVisible) return
    var newHabitTitle by remember { mutableStateOf(habit?.title ?: "") }
    var newHabitDescription by remember { mutableStateOf(habit?.description ?: "") }
    var newHabitTime by remember {
        mutableLongStateOf(
            habit?.startDateTime ?: System.currentTimeMillis()
        )
    }
    var timePickerDialog by remember { mutableStateOf(false) }
    val focusRequesterDesc = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isEditing) Icons.Default.Edit else Icons.Default.Add,
                    null,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    if (isEditing) stringResource(R.string.Edit_Habit) else stringResource(R.string.Add_Habit),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(Modifier.height(4.dp))

            TextField(
                value = newHabitTitle,
                onValueChange = { newHabitTitle = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.Title)) },
                singleLine = true,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.primary,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.primary
                ),
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
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.primary,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.primary
                ),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        timePickerDialog = true
                    }
                )
            )
            Spacer(Modifier.height(4.dp))
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
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = {
                    keyboardController?.hide()
                    onDismissRequest()
                }) { Text(stringResource(R.string.Dismiss)) }

                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = {
                        if (habit == null) {
                            val newHabit = HabitModel(
                                title = newHabitTitle,
                                startDateTime = newHabitTime,
                                description = newHabitDescription
                            )
                            onConfirm(newHabit, getAdjustedTime(newHabitTime))
                        } else {
                            onConfirm(
                                habit.copy(
                                    title = newHabitTitle,
                                    description = newHabitDescription,
                                    startDateTime = newHabitTime
                                ), getAdjustedTime(newHabitTime)
                            )
                        }
                        keyboardController?.hide()
                        onDismissRequest()
                    }, enabled = newHabitTitle.isNotBlank()
                ) { Text(stringResource(R.string.Confirm)) }
            }

            if (timePickerDialog) {
                TimePicker(
                    initialTime = newHabitTime,
                    is24Hour = settings.data.is24HourFormat,
                    onConfirm = {
                        val habitCalendar = Calendar.getInstance().apply {
                            timeInMillis = newHabitTime
                            set(Calendar.HOUR_OF_DAY, it.hour)
                            set(Calendar.MINUTE, it.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        newHabitTime = habitCalendar.timeInMillis
                    }
                ) { timePickerDialog = false }
            }
        }
    }
}

fun getAdjustedTime(time: Long): Long {
    val now = Calendar.getInstance()
    val habitCalendar = Calendar.getInstance().apply { timeInMillis = time }

    // Normalize seconds and milliseconds
    habitCalendar.set(Calendar.SECOND, 0)
    habitCalendar.set(Calendar.MILLISECOND, 0)

    // Keep adding days until it's in the future
    while (habitCalendar.before(now)) {
        habitCalendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    return habitCalendar.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewWorkspaceBottomSheet(
    isEditing: Boolean = false,
    workspace: WorkspaceModel = WorkspaceModel(),
    isVisible: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onConfirm: (WorkspaceModel) -> Unit
) {
    var title by remember { mutableStateOf(workspace.title) }
    var description by remember { mutableStateOf(workspace.description) }
    val focusRequesterDesc = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                keyboardController?.hide()
                onDismiss()
                title = workspace.title
                description = workspace.description
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (isEditing) Icons.Default.Edit else Icons.Default.Add, null)
                    Text(
                        if (isEditing) stringResource(R.string.Edit_Workspace) else stringResource(R.string.Add_Workspace),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 3.dp),
                    placeholder = { Text(stringResource(R.string.Title)) },
                    singleLine = true,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.primary,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusRequesterDesc.requestFocus() })
                )

                TextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesterDesc),
                    placeholder = { Text(stringResource(R.string.Description)) },
                    singleLine = true,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.primary,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            onConfirm(workspace.copy(title = title, description = description))
                            onDismiss()
                            title = workspace.title
                            description = workspace.description
                        }
                    )
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = {
                        keyboardController?.hide()
                        onDismiss()
                        title = workspace.title
                        description = workspace.description
                    }) {
                        Text(stringResource(R.string.Dismiss))
                    }

                    Spacer(Modifier.width(8.dp))

                    FilledTonalButton(
                        enabled = title.isNotBlank(),
                        onClick = {
                            keyboardController?.hide()
                            onConfirm(workspace.copy(title = title, description = description))
                            onDismiss()
                            title = workspace.title
                            description = workspace.description
                        }
                    ) {
                        Text(stringResource(R.string.Confirm))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeIconBottomSheet(
    isVisible: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                items(workspaceIconList) { item ->
                    Text(
                        item.title,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        item.icons.forEach { index ->
                            IconButton({ onConfirm(index) }) { Icon(icons[index], null) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewSpacesBottomSheet(
    isVisible: Boolean,
    sheetState: SheetState,
    selectedSpaces: List<Space>,
    onDismiss: () -> Unit,
    onRemove: (Int) -> Unit,
    onSelect: (Int) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var removeSpace by remember { mutableIntStateOf(-1) }
    val spacesList = getSpacesList()
    if (showDeleteDialog) {
        DeleteAlert(onConfirmation = {
            onRemove(removeSpace)
            removeSpace = -1
            showDeleteDialog = false
        }, onDismissRequest = {
            showDeleteDialog = false
        })
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            LazyColumn(Modifier.fillMaxWidth()) {
                if (selectedSpaces.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.Current),
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                item {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedSpaces.forEach { space ->
                            SpaceCard(space, true, { onSelect(space.id) }, {
                                removeSpace = space.id
                                showDeleteDialog = true
                            })
                        }
                    }
                }
                if (selectedSpaces.size != spacesList.size) {
                    item {
                        Text(
                            stringResource(R.string.Available_Spaces),
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                item {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        spacesList.filterNot { id -> selectedSpaces.contains(id) }
                            .forEach { space ->
                                SpaceCard(space, false, { onSelect(space.id) }, { })
                            }
                    }
                }
            }
        }
    }
}

@Composable
fun SpaceCard(space: Space, isSelected: Boolean, onSelect: () -> Unit, onRemove: () -> Unit) {
    val cardContainerColor =
        if (isSelected) MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp) else MaterialTheme.colorScheme.surfaceContainerHigh
    val cardContentColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val iconContainerColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val iconContentColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.clip(RoundedCornerShape(50)),
        shape = RoundedCornerShape(50),
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor,
            contentColor = cardContentColor
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onSelect,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = iconContainerColor,
                    contentColor = iconContentColor
                )
            ) { Icon(space.icon, null) }
            Text(space.title, modifier = Modifier.padding(end = if (isSelected) 0.dp else 16.dp))
            if (isSelected) {
                IconButton(onRemove) { Icon(Icons.Default.Remove, null) }
            }
        }
    }
}