package com.flux.ui.screens.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.NotificationsActive
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.flux.ui.components.DatePickerModal
import com.flux.ui.components.EventNotificationDialog
import com.flux.ui.components.RecurrenceBottomSheet
import com.flux.ui.components.TimePicker
import com.flux.ui.components.convertMillisToTime
import com.flux.ui.components.label
import com.flux.ui.events.TaskEvents
import com.flux.ui.state.Settings
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEvent(
    navController: NavController,
    event: EventModel,
    settings: Settings,
    onTaskEvents: (TaskEvents) -> Unit
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf(event.title) }
    var description by rememberSaveable { mutableStateOf(event.description) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCustomNotificationDialog by remember { mutableStateOf(false) }
    var showRepetitionSheet by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var notificationOffset by rememberSaveable { mutableLongStateOf(event.notificationOffset) }
    var selectedDateTime by rememberSaveable { mutableLongStateOf(event.startDateTime) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentRecurrenceRule by remember { mutableStateOf(event.recurrence) }
    var eventEndsOn by rememberSaveable { mutableLongStateOf(event.endDateTime) }
    var neverEnds by rememberSaveable { mutableStateOf(event.endDateTime==-1L) }
    var showDatePicker by remember { mutableStateOf(false) }

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

    if(showDatePicker){
        DatePickerModal(onDateSelected = { if(it!=null) eventEndsOn=it }){
            showDatePicker=false
        }
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
                            val updatedEvent = event.copy(title = title, description = description, startDateTime = selectedDateTime, notificationOffset = notificationOffset, recurrence = currentRecurrenceRule, endDateTime = eventEndsOn)
                            onTaskEvents(TaskEvents.UpsertTask(context, updatedEvent))
                            navController.popBackStack()
                        }
                    )
                    { Icon(Icons.Default.Check, null) }
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
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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

            if(currentRecurrenceRule!= RecurrenceRule.Once){
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically){
                        Icon(Icons.Outlined.Flag, null)
                        Text("Never Ends")
                    }

                    Switch(neverEnds, onCheckedChange = {
                        if(neverEnds){
                            neverEnds=false
                            eventEndsOn=max(selectedDateTime, System.currentTimeMillis())
                        }
                        else{
                            neverEnds=true
                            eventEndsOn=-1L
                        }
                    })
                }

                if(!neverEnds){
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween){
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Today, null, modifier = Modifier.size(24.dp))
                            Text("Ends on")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(eventEndsOn.toFormattedDate())
                            FilledTonalIconButton({ showDatePicker=true }) {
                                Icon(
                                    imageVector = Icons.Default.Create,
                                    contentDescription = "Pick Time"
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()
            }

            Row(
                Modifier.fillMaxWidth().clickable { showNotificationDialog = true }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.NotificationsActive, null)
                Text(getNotificationText(notificationOffset))
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