package com.flux.ui.screens.events

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import com.flux.other.cancelReminder
import com.flux.ui.components.CustomNotificationDialog
import com.flux.ui.components.DatePickerModal
import com.flux.ui.components.EventNotificationDialog
import com.flux.ui.components.TaskRepetitionDialog
import com.flux.ui.components.TimePicker
import com.flux.ui.components.convertMillisToDate
import com.flux.ui.components.convertMillisToTime
import com.flux.ui.events.TaskEvents
import com.flux.ui.state.Settings
import com.flux.ui.theme.completed
import com.flux.ui.theme.pending
import java.time.LocalDate
import java.util.Calendar

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
    var status by remember { mutableStateOf(isPending) }
    var eventRepetition by remember { mutableStateOf(event.repetition) }
    var checked by remember { mutableStateOf(event.isAllDay) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCustomNotificationDialog by remember { mutableStateOf(false) }
    var showRepetitionDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var notificationOffset by remember { mutableLongStateOf(event.notificationOffset) }
    var selectedDateTime by remember { mutableLongStateOf(event.startDateTime) }

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
    if (showRepetitionDialog) {
        TaskRepetitionDialog(eventRepetition, { eventRepetition = it }) {
            showRepetitionDialog = false
        }
    }
    if (showDatePicker) {
        DatePickerModal(onDateSelected = { newDateMillis ->
            if (newDateMillis != null) {
                val timeOfDay = selectedDateTime % DateUtils.DAY_IN_MILLIS
                selectedDateTime = newDateMillis + timeOfDay
            }
        }, onDismiss = { showDatePicker = false })
    }

    if (showTimePicker) {
        TimePicker(
            initialTime = selectedDateTime,
            is24Hour = settings.data.is24HourFormat,
            onConfirm = {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = selectedDateTime
                    set(Calendar.HOUR_OF_DAY, it.hour)
                    set(Calendar.MINUTE, it.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedDateTime = calendar.timeInMillis
            }) { showTimePicker = false }
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
                            val adjustedTime = getNextValidReminderTime(
                                initialTimeMillis = selectedDateTime,
                                offset = notificationOffset,
                                repetition = eventRepetition
                            )
                            cancelReminder(
                                context,
                                event.eventId,
                                "EVENT",
                                title,
                                description,
                                event.repetition
                            )

                            val updatedEvent = event.copy(
                                title = title,
                                description = description,
                                isAllDay = checked,
                                startDateTime = selectedDateTime,
                                repetition = eventRepetition,
                                notificationOffset = notificationOffset,
                            )

                            if(isPending) {
                                onTaskEvents(
                                    TaskEvents.UpsertInstance(
                                        EventInstanceModel(
                                            eventId = event.eventId,
                                            workspaceId = workspaceId,
                                            instanceDate = LocalDate.now().toEpochDay()
                                        )
                                    )
                                )
                            }
                            else {
                                onTaskEvents(
                                    TaskEvents.DeleteInstance(
                                        EventInstanceModel(
                                            eventId = event.eventId,
                                            workspaceId = workspaceId,
                                            instanceDate = LocalDate.now().toEpochDay()
                                        )
                                    )
                                )
                            }
                            onTaskEvents(TaskEvents.UpsertTask(context, updatedEvent, adjustedTime))
                            navController.popBackStack()
                        }
                    )
                    { Icon(Icons.Default.Check, null) }
                    IconButton({
                        cancelReminder(
                            context,
                            event.eventId,
                            "EVENT",
                            event.title,
                            event.description,
                            event.repetition
                        )
                        navController.popBackStack()
                        onTaskEvents(TaskEvents.DeleteTask(event))
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
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedTextColor = MaterialTheme.colorScheme.primary,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(2.dp))
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text(stringResource(R.string.Description)) },
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraLight),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedTextColor = MaterialTheme.colorScheme.primary,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            HorizontalDivider(Modifier.fillMaxWidth())
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, null)
                        Text(stringResource(R.string.All_Day))
                    }
                    Switch(
                        checked = checked,
                        onCheckedChange = { newChecked ->
                            checked = newChecked
                            if (newChecked) {
                                val calendar = Calendar.getInstance().apply {
                                    timeInMillis = selectedDateTime
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                selectedDateTime = calendar.timeInMillis
                            }
                        },
                        thumbContent = { Icon(Icons.Default.Check, null) }
                    )
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        convertMillisToDate(selectedDateTime),
                        modifier = Modifier
                            .clickable { showDatePicker = true }
                            .padding(4.dp)
                    )
                    if (!checked) {
                        Text(
                            convertMillisToTime(selectedDateTime, settings.data.is24HourFormat),
                            modifier = Modifier
                                .clickable { showTimePicker = true }
                                .padding(4.dp)
                        )
                    }
                }
            }
            HorizontalDivider(Modifier.fillMaxWidth())
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { showRepetitionDialog = true }
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Repeat, null)
                Text(eventRepetition)
            }
            HorizontalDivider(Modifier.fillMaxWidth())
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { showNotificationDialog = true }
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.NotificationsActive, null)
                Text(getNotificationText(notificationOffset))
            }
            HorizontalDivider(Modifier.fillMaxWidth())
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (status) Icons.Filled.Pending else Icons.Filled.CheckCircle,
                    null,
                    tint = if (status) pending else completed
                )
                Text(if (status) stringResource(R.string.Status_Pending) else stringResource(R.string.Status_Completed))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                ElevatedButton(
                    onClick = { status = !status },
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(
                        if (status) stringResource(R.string.Mark_As_Completed) else stringResource(
                            R.string.Mark_As_Uncompleted
                        ), style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}


fun getNextValidReminderTime(
    initialTimeMillis: Long,
    offset: Long,
    repetition: String
): Long {
    var reminderTime = initialTimeMillis - offset
    val now = System.currentTimeMillis()

    if (reminderTime > now) return reminderTime

    // Adjust based on repetition
    return when (repetition) {
        "DAILY" -> {
            while (reminderTime <= now) {
                reminderTime += DateUtils.DAY_IN_MILLIS
            }
            reminderTime
        }

        "WEEKLY" -> {
            while (reminderTime <= now) {
                reminderTime += DateUtils.WEEK_IN_MILLIS
            }
            reminderTime
        }

        "MONTHLY" -> {
            val calendar = Calendar.getInstance().apply { timeInMillis = reminderTime }
            while (calendar.timeInMillis <= now) {
                calendar.add(Calendar.MONTH, 1)
            }
            calendar.timeInMillis
        }

        else -> {
            val calendar = Calendar.getInstance().apply { timeInMillis = reminderTime }
            while (calendar.timeInMillis <= now) {
                calendar.add(Calendar.YEAR, 1)
            }
            calendar.timeInMillis
        }
    }
}

@Composable
fun getNotificationText(offsetMillis: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val isEnglish = locale.language == "en"

    return when (offsetMillis) {
        0L -> stringResource(R.string.On_Time)
        5 * 60 * 1000L -> stringResource(R.string.five_minutes_before)
        10 * 60 * 1000L -> stringResource(R.string.ten_minutes_before)
        15 * 60 * 1000L -> stringResource(R.string.fifteen_minutes_before)
        30 * 60 * 1000L -> stringResource(R.string.thirty_minutes_before)
        60 * 60 * 1000L -> stringResource(R.string.one_hour_before)
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
