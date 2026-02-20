package com.flux.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flux.R
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.Space
import com.flux.data.model.WorkspaceModel
import com.flux.data.model.getSpacesList
import com.flux.other.HeaderNode
import com.flux.other.icons
import com.flux.other.workspaceIconList
import com.flux.ui.screens.events.formatCustom
import com.flux.ui.screens.events.formatMonthly
import com.flux.ui.screens.events.formatOnce
import com.flux.ui.screens.events.formatYearly
import java.util.Calendar

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
            LazyColumn(Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)) {
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

@Composable
fun RecurrenceRule.label(): String = when (this) {
    is RecurrenceRule.Once  -> stringResource(R.string.Once)
    is RecurrenceRule.Custom   -> stringResource(R.string.Custom)
    is RecurrenceRule.Weekly  -> stringResource(R.string.Weekly)
    is RecurrenceRule.Monthly -> stringResource(R.string.Monthly)
    is RecurrenceRule.Yearly  -> stringResource(R.string.Yearly)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceBottomSheet(
    isVisible: Boolean,
    sheetState: SheetState,
    startDateTime: Long,
    onDismiss: () -> Unit,
    currentRule: RecurrenceRule,
    onRuleChange: (RecurrenceRule, Long) -> Unit
) {
    if (!isVisible) return
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateTime by remember { mutableLongStateOf(startDateTime) }
    var tempRule by remember(currentRule) { mutableStateOf(currentRule) }

    val options = listOf(
        RecurrenceRule.Once,
        RecurrenceRule.Weekly(),
        RecurrenceRule.Monthly,
        RecurrenceRule.Yearly,
        RecurrenceRule.Custom()
    )

    if (showDatePicker) {
        DatePickerModal(onDateSelected = { newDateMillis ->
            if (newDateMillis != null) {
                val timeOfDay = selectedDateTime % DateUtils.DAY_IN_MILLIS
                selectedDateTime = newDateMillis + timeOfDay
            }
        }, onDismiss = { showDatePicker = false })
    }

    ModalBottomSheet(
        modifier = Modifier.heightIn(min = 300.dp),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        LazyColumn(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)) {
                item {
                    Row(
                        Modifier.padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Repeat, null)
                        Text(
                            stringResource(R.string.repeat),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(options.size) { index ->
                            val option = options[index]
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = option::class == tempRule::class,
                                    onClick = { tempRule = option }
                                )
                                Text(option.label(), modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }

                item {
                    when (val rule = tempRule) {
                        is RecurrenceRule.Once -> {
                            Row(Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatOnce(selectedDateTime))
                                IconButton({ showDatePicker=true }, colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )) {
                                    Icon(Icons.Default.Edit, null)
                                }
                            }
                        }

                        is RecurrenceRule.Custom -> {
                            OutlinedTextField(
                                value = rule.everyXDays.toString(),
                                onValueChange = { new -> new.toIntOrNull()?.let { tempRule = rule.copy(everyXDays = it) } },
                                modifier = Modifier.padding(start = 8.dp),
                                label = { Text(formatCustom(rule)) },
                                singleLine = true
                            )
                        }

                        is RecurrenceRule.Weekly -> {
                            val weekdays = listOf(
                                stringResource(R.string.monday_short),
                                stringResource(R.string.tuesday_short),
                                stringResource(R.string.wednesday_short),
                                stringResource(R.string.thursday_short),
                                stringResource(R.string.friday_short),
                                stringResource(R.string.saturday_short),
                                stringResource(R.string.sunday_short)
                            )
                            Row (
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                weekdays.forEachIndexed { index, day ->
                                    val isSelected = index in rule.daysOfWeek
                                    Card(
                                        onClick = {
                                            val newDays = rule.daysOfWeek.toMutableList()
                                            if (index in newDays) newDays.remove(index) else newDays.add(index)
                                            tempRule = rule.copy(daysOfWeek = newDays)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 2.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Text(
                                            text = day,
                                            modifier = Modifier
                                                .padding(6.dp)
                                                .fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        is RecurrenceRule.Monthly -> {
                            Row(Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatMonthly(selectedDateTime))
                                IconButton({ showDatePicker=true }, colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
                                )) {
                                    Icon(Icons.Default.Edit, null)
                                }
                            }
                        }

                        is RecurrenceRule.Yearly -> {
                            Row(Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatYearly(selectedDateTime))
                                IconButton({ showDatePicker=true }, colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
                                )) {
                                    Icon(Icons.Default.Edit, null)
                                }
                            }
                        }
                    }
                }

                // Buttons
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = onDismiss) {
                            Text(stringResource(R.string.Dismiss))
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = {
                                onRuleChange(tempRule, adjustStartDate(tempRule, selectedDateTime))
                                onDismiss()
                            }
                        ) {
                            Text(stringResource(R.string.Confirm))
                        }
                    }
                }
            }
        }
    }

    fun adjustStartDate(rule: RecurrenceRule, startDateTime: Long): Long {
        return when (rule) {
            is RecurrenceRule.Once -> startDateTime
            is RecurrenceRule.Monthly -> startDateTime
            is RecurrenceRule.Yearly -> startDateTime
            is RecurrenceRule.Custom -> startDateTime
            is RecurrenceRule.Weekly -> {
                val cal = Calendar.getInstance().apply { timeInMillis = startDateTime }

                // Calendar days: Sunday = 1, Monday = 2, ... Saturday = 7
                val today = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // shift so Monday=0, Sunday=6

                if (today in rule.daysOfWeek) {
                    startDateTime
                } else {
                    // find next closest match
                    var offset = 1
                    while (true) {
                        val nextDay = (today + offset) % 7
                        if (nextDay in rule.daysOfWeek) {
                            cal.add(Calendar.DAY_OF_YEAR, offset)
                            return cal.timeInMillis
                        }
                        offset++
                    }
                }
            }
        } as Long
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlineBottomSheet(
    isVisible: Boolean,
    outline: HeaderNode,
    sheetState: SheetState,
    onHeaderClick: (IntRange) -> Unit,
    onDismiss: () -> Unit
) {

    var isAllExpanded by rememberSaveable { mutableStateOf(true) }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            LazyColumn(Modifier
                .fillMaxWidth()
                .heightIn(300.dp)) {
                item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.Outline),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = { isAllExpanded = !isAllExpanded }
                            ) {
                                Icon(
                                    imageVector = if (isAllExpanded) Icons.Outlined.UnfoldLess
                                    else Icons.Outlined.UnfoldMore,
                                    contentDescription = "fold",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                    HorizontalDivider()
                }
                items(outline.children) { header ->
                    HeaderItem(
                        header = header,
                        depth = 0,
                        onHeaderClick = onHeaderClick,
                        parentExpanded = isAllExpanded
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderItem(
    header: HeaderNode,
    depth: Int,
    parentExpanded: Boolean,
    onHeaderClick: (IntRange) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(parentExpanded) { expanded = parentExpanded }
    Row(
        modifier = Modifier
            .padding(start = (depth * 8).dp)
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .clickable {
                onHeaderClick(header.range)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (header.children.isNotEmpty()) {
            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = {
                    if (header.children.isNotEmpty()) {
                        expanded = !expanded
                    }
                }
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropDown
                    else Icons.AutoMirrored.Filled.ArrowRight,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = null
                )
            }
        } else {
            Spacer(modifier = Modifier.width(32.dp))
        }

        Text(
            text = header.title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
    }

    if (expanded) {
        header.children.forEach { child ->
            HeaderItem(
                header = child,
                depth = depth + 1,
                onHeaderClick = onHeaderClick,
                parentExpanded = parentExpanded
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesInfoBottomSheet(
    words: Int,
    lines: Int,
    wordsWithoutPunctuations: Int,
    paragraph: Int,
    characters: Int,
    lastEdited: String,
    isVisible: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                item {
                    SettingOption(
                        radius = shapeManager(isFirst = true, radius = 32),
                        icon = Icons.Default.Edit,
                        title = stringResource(R.string.Last_Edited),
                        description = lastEdited,
                        actionType = ActionType.None
                    )
                }

                item {
                    SettingOption(
                        radius = shapeManager(radius = 32),
                        icon = Icons.Default.Numbers,
                        title = stringResource(R.string.Word_Count),
                        description = words.toString(),
                        actionType = ActionType.None
                    )
                }

                item {
                    SettingOption(
                        radius = shapeManager(radius = 32),
                        icon = Icons.Default.FormatQuote,
                        title = stringResource(R.string.words_excluding_punctuations),
                        description = wordsWithoutPunctuations.toString(),
                        actionType = ActionType.None
                    )
                }

                item {
                    SettingOption(
                        radius = shapeManager(radius = 32),
                        icon = Icons.Default.Abc,
                        title = stringResource(R.string.Character_Count),
                        description = characters.toString(),
                        actionType = ActionType.None
                    )
                }

                item {
                    SettingOption(
                        radius = shapeManager(radius = 32),
                        icon = Icons.AutoMirrored.Filled.List,
                        title = stringResource(R.string.lines),
                        description = lines.toString(),
                        actionType = ActionType.None
                    )
                }

                item {
                    SettingOption(
                        radius = shapeManager(radius = 32, isLast = true),
                        icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                        title = stringResource(R.string.paragraph),
                        description = paragraph.toString(),
                        actionType = ActionType.None
                    )
                }
            }
        }
    }
}
