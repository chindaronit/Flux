package com.flux.ui.screens.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.EventModel
import com.flux.data.model.RecurrenceRule
import com.flux.navigation.NavRoutes
import com.flux.ui.components.DeleteAlert
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
    instanceDate: Long,
    settings: Settings,
    onTaskEvents: (TaskEvents) -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description) }
    var pendingStatus by remember { mutableStateOf(isPending) }
    var notificationOffset by remember { mutableLongStateOf(event.notificationOffset) }
    var currentRecurrenceRule by remember { mutableStateOf(event.recurrence) }
    val context = LocalContext.current
    val time = event.startDateTime.toFormattedTime(settings.data.is24HourFormat)
    var showDeleteDialog by remember { mutableStateOf(false) }

    if(showDeleteDialog){
        DeleteAlert({
            showDeleteDialog=false
        }, {
            onTaskEvents(TaskEvents.DeleteTask(event, context))
            navController.popBackStack()
            showDeleteDialog=false
        })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton({ navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack,"Back")
                    }
                },
                actions = {
                    IconButton({ navController.navigate(NavRoutes.NewEvent.withArgs(workspaceId, event.id, event.startDateTime)) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit"
                        )
                    }
                    IconButton({ showDeleteDialog=true }) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    pendingStatus=!pendingStatus
                    onTaskEvents(TaskEvents.ToggleStatus(!pendingStatus, event.id, workspaceId, instanceDate)) },
                icon = { Icon( if(pendingStatus) Icons.Filled.Verified else Icons.Default.Pending, null) },
                text = { Text(text = stringResource(
                        if (pendingStatus) R.string.mark_completed else R.string.mark_pending
                    ),
                    fontWeight = FontWeight.SemiBold
                ) },
            )
        }
    ) { innerPadding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if(description.isNotBlank()) item { Text(description, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal)) }
            item{
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(6.dp))
                    Text("${getRecurrenceText(context, currentRecurrenceRule, event.startDateTime)} at $time")
                }
            }
            if(event.endDateTime!=-1L){
                item{
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SportsScore, null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.ends_on_date, event.endDateTime.toFormattedDate()))
                    }
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.NotificationsActive, null)
                    Text(getNotificationText(notificationOffset))
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (pendingStatus) Icons.Outlined.Pending else Icons.Filled.Verified,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(if (pendingStatus) stringResource(R.string.Status_Pending) else stringResource(R.string.Status_Completed))
                }
            }
        }
    }
}

@Composable
fun formatOnce(selectedDateTime: Long): String {
    val fullDate = Instant.ofEpochMilli(selectedDateTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    return stringResource(R.string.recurrence_once, fullDate)
}

@Composable
fun formatCustom(rule: RecurrenceRule.Custom): String {
    return stringResource(R.string.recurrence_every_x_days, rule.everyXDays)
}

@Composable
fun formatMonthly(selectedDateTime: Long): String {
    val dayOfMonth = Instant.ofEpochMilli(selectedDateTime)
        .atZone(ZoneId.systemDefault())
        .dayOfMonth
    return stringResource(R.string.recurrence_monthly_on, dayOfMonth)
}

@Composable
fun formatYearly(selectedDateTime: Long): String {
    val date = Instant.ofEpochMilli(selectedDateTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("dd MMM"))
    return stringResource(R.string.recurrence_yearly_on, date)
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