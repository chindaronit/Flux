package com.flux.ui.screens.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.EventModel
import com.flux.data.model.RecurrenceRule
import com.flux.ui.components.CustomNotificationDialog
import com.flux.ui.components.EventNotificationDialog
import com.flux.ui.components.RecurrenceBottomSheet
import com.flux.ui.components.TimePicker
import com.flux.ui.components.convertMillisToTime
import com.flux.ui.components.label
import com.flux.ui.events.TaskEvents
import com.flux.ui.state.Settings
import java.time.ZoneId
import java.time.Instant
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetails(
    navController: NavController,
    workspaceId: String,
    event: EventModel,
    isPending: Boolean,
    settings: Settings,
    onTaskEvents: (TaskEvents) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description) }
    var pendingStatus by remember { mutableStateOf(isPending) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCustomNotificationDialog by remember { mutableStateOf(false) }
    var showRepetitionSheet by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var notificationOffset by remember { mutableLongStateOf(event.notificationOffset) }
    var selectedDateTime by remember { mutableLongStateOf(event.startDateTime) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentRecurrenceRule by remember { mutableStateOf(event.recurrence) }

    if (showCustomNotificationDialog) {
        CustomNotificationDialog({
            showCustomNotificationDialog = false
        }) { offset -> notificationOffset = offset }
    }

    if (showNotificationDialog) {
        EventNotificationDialog(
            currentOffset = notificationOffset,
            onChange = { offset -> notificationOffset = offset },
            onCustomClick = { showCustomNotificationDialog = true }) {
            showNotificationDialog = false
        }
    }

    if (showTimePicker) {
        TimePicker(
            initialTime = selectedDateTime,
            is24Hour = settings.data.is24HourFormat,
            onConfirm = { selectedDateTime = it },
            onDismiss = { showTimePicker = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.surfaceContainerLow),
                title = { Text(stringResource(R.string.Edit_Event)) },
                navigationIcon = {
                    IconButton({ navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            null
                        )
                    }
                },
                actions = {
                    IconButton(
                        enabled = title.isNotBlank(),
                        onClick = {
                            val updatedEvent = event.copy(title = title, description = description, startDateTime = selectedDateTime, notificationOffset = notificationOffset, recurrence = currentRecurrenceRule)
                            onTaskEvents(TaskEvents.ToggleStatus(!pendingStatus, event.id, workspaceId = workspaceId))
                            onTaskEvents(TaskEvents.UpsertTask(context, updatedEvent))
                            navController.popBackStack()
                        }
                    )
                    { Icon(Icons.Default.Check, null) }
                    IconButton({
                        navController.popBackStack()
                        onTaskEvents(TaskEvents.DeleteTask(event, context))
                    }) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Column(Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    placeholder = { Text(stringResource(R.string.Title)) },
                    textStyle = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    colors = getTextFieldColors()
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text(stringResource(R.string.Description)) },
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraLight),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 4.dp),
                    colors = getTextFieldColors()
                )
            }
            HorizontalDivider(Modifier.fillMaxWidth())
            Column(Modifier.padding(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { showRepetitionSheet = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Repeat, null)
                        Text(currentRecurrenceRule.label())
                    }

                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { showTimePicker = true }
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp))
                    {
                        Icon(Icons.Default.AccessTime, null)
                        Text(
                            convertMillisToTime(selectedDateTime, settings.data.is24HourFormat),
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
                when (val rule = currentRecurrenceRule) {
                    is RecurrenceRule.Once -> {
                        Text(
                            formatOnce(selectedDateTime),
                            modifier = Modifier.padding(top = 12.dp, start = 6.dp)
                        )
                    }

                    is RecurrenceRule.Custom -> {
                        Text(
                            formatCustom(rule),
                            modifier = Modifier.padding(top = 12.dp, start = 6.dp)
                        )
                    }

                    is RecurrenceRule.Monthly -> {
                        Text(
                            formatMonthly(selectedDateTime),
                            modifier = Modifier.padding(top = 12.dp, start = 6.dp)
                        )
                    }

                    is RecurrenceRule.Yearly -> {
                        Text(
                            formatYearly(selectedDateTime),
                            modifier = Modifier.padding(top = 12.dp, start = 6.dp)
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

                        Row(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                            weekdays.forEachIndexed { index, day ->
                                val isSelected = index in rule.daysOfWeek
                                Card(
                                    onClick = {},
                                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceColorAtElevation(
                                            8.dp
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
                }
            }
            HorizontalDivider(Modifier.fillMaxWidth())
            Row(
                Modifier.fillMaxWidth().clickable { showNotificationDialog = true }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.NotificationsActive, null)
                Text(getNotificationText(notificationOffset))
            }
            HorizontalDivider(Modifier.fillMaxWidth())
            Row(
                Modifier.fillMaxWidth().clickable{ pendingStatus = !pendingStatus }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (pendingStatus) Icons.Outlined.Pending else Icons.Filled.CheckCircle,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(if (pendingStatus) stringResource(R.string.Status_Pending) else stringResource(R.string.Status_Completed))
            }
        }
    }

    // Edit Workspace Sheet
    RecurrenceBottomSheet(
        isVisible = showRepetitionSheet,
        sheetState = sheetState,
        startDateTime = selectedDateTime,
        onDismiss = { showRepetitionSheet = false },
        currentRule = currentRecurrenceRule
    ) { newRule, newStart ->
        currentRecurrenceRule = newRule
        selectedDateTime = newStart
    }
}

@Composable
fun formatOnce(selectedDateTime: Long): String {
    val context = LocalContext.current
    val fullDate = Instant.ofEpochMilli(selectedDateTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    return context.getString(R.string.recurrence_once, fullDate)
}

@Composable
fun formatCustom(rule: RecurrenceRule.Custom): String {
    val context = LocalContext.current
    return context.getString(R.string.recurrence_every_x_days, rule.everyXDays)
}

@Composable
fun formatMonthly(selectedDateTime: Long): String {
    val context = LocalContext.current
    val dayOfMonth = Instant.ofEpochMilli(selectedDateTime)
        .atZone(ZoneId.systemDefault())
        .dayOfMonth
    return context.getString(R.string.recurrence_monthly_on, dayOfMonth)
}

@Composable
fun formatYearly(selectedDateTime: Long): String {
    val context = LocalContext.current
    val date = Instant.ofEpochMilli(selectedDateTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("dd MMM"))
    return context.getString(R.string.recurrence_yearly_on, date)
}

@Composable
fun getNotificationText(offsetMillis: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val isEnglish = locale.language == "en"

    return when (offsetMillis) {
        0L -> stringResource(R.string.On_Time)
        5 * 60 * 1000L -> stringResource(R.string.five_minutes_before)
        30 * 60 * 1000L -> stringResource(R.string.thirty_minutes_before)
        else -> {
            val totalMinutes = offsetMillis / (60 * 1000)
            val days = totalMinutes / (24 * 60)
            val hours = (totalMinutes % (24 * 60)) / 60
            val minutes = totalMinutes % 60

            buildString {
                if (days > 0) append("$days ${stringResource(R.string.day)}${if (isEnglish && days > 1) "s" else ""} ")
                if (hours > 0) append("$hours ${stringResource(R.string.hour)}${if (isEnglish && hours > 1) "s" else ""} ")
                if (minutes > 0) append("$minutes ${stringResource(R.string.minute)}${if (isEnglish && minutes > 1) "s" else ""} ")
                append(stringResource(R.string.before))
            }.trim()
        }
    }
}

@Composable
fun getTextFieldColors(): TextFieldColors {
    return TextFieldDefaults.colors(
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
        focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
        focusedTextColor = MaterialTheme.colorScheme.primary,
        focusedPlaceholderColor = MaterialTheme.colorScheme.primary
    )
}