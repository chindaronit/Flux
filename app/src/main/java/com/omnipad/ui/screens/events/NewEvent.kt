package com.omnipad.ui.screens.events

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavController
import com.omnipad.R
import com.omnipad.data.model.EventExceptionModel
import com.omnipad.data.model.EventInstanceModel
import com.omnipad.data.model.EventModel
import com.omnipad.data.model.WorkspaceModel
import com.omnipad.data.model.effectiveReminderOffsets
import com.omnipad.data.model.configuredDurationMillis
import com.omnipad.data.model.durationUntilEndTime
import com.omnipad.data.model.eventEditorPreviewColor
import com.omnipad.data.model.IntervalUnit
import com.omnipad.data.model.RecurrenceRule
import com.omnipad.ui.common.DatePickerModal
import com.omnipad.ui.common.DiscardChangesDialog
import com.omnipad.ui.common.RecurrenceBottomSheet
import com.omnipad.ui.common.TimePicker
import com.omnipad.ui.common.convertMillisToTime
import com.omnipad.ui.common.label
import com.omnipad.ui.common.DataCopyDialog
import com.omnipad.ui.common.DeleteAlert
import com.omnipad.ui.common.EventDropdownMenu
import com.omnipad.ui.events.TaskEvents
import com.omnipad.ui.events.WorkspaceEvents
import com.omnipad.navigation.NavRoutes
import com.omnipad.other.Constants
import com.omnipad.other.DataCopyType
import com.omnipad.other.ensureStorageRoot
import com.omnipad.ui.state.Settings
import com.omnipad.ui.viewModel.SettingsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEvent(
    navController: NavController,
    event: EventModel,
    settings: Settings,
    onTaskEvents: (TaskEvents) -> Unit,
    occurrenceDate: Long? = null,
    isNewEvent: Boolean,
    workspaces: List<WorkspaceModel>,
    allEventInstances: List<EventInstanceModel>,
    eventOccurrenceColorArgb: Int?,
    hasOccurrenceColorOverride: Boolean,
    onWorkspaceEvents: (WorkspaceEvents) -> Unit,
    settingsViewModel: SettingsViewModel
) {
    var editingOccurrence by rememberSaveable(event.id, occurrenceDate) {
        mutableStateOf(occurrenceDate != null)
    }
    key(event.id, editingOccurrence) {
        EventEditor(
            navController = navController,
            event = event,
            settings = settings,
            onTaskEvents = onTaskEvents,
            occurrenceDate = occurrenceDate.takeIf { editingOccurrence },
            isNewEvent = isNewEvent,
            workspaces = workspaces,
            allEventInstances = allEventInstances,
            eventOccurrenceColorArgb = if (editingOccurrence || occurrenceDate == null) {
                eventOccurrenceColorArgb
            } else {
                event.colorArgb
            },
            hasOccurrenceColorOverride = hasOccurrenceColorOverride,
            onWorkspaceEvents = onWorkspaceEvents,
            settingsViewModel = settingsViewModel,
            onSwitchScope = if (!isNewEvent && occurrenceDate != null && event.recurrence != RecurrenceRule.Once) {
                { editingOccurrence = !editingOccurrence }
            } else null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventEditor(
    navController: NavController,
    event: EventModel,
    settings: Settings,
    onTaskEvents: (TaskEvents) -> Unit,
    occurrenceDate: Long?,
    isNewEvent: Boolean,
    workspaces: List<WorkspaceModel>,
    allEventInstances: List<EventInstanceModel>,
    eventOccurrenceColorArgb: Int?,
    hasOccurrenceColorOverride: Boolean,
    onWorkspaceEvents: (WorkspaceEvents) -> Unit,
    settingsViewModel: SettingsViewModel,
    onSwitchScope: (() -> Unit)?
) {
    val context = LocalContext.current
    var originalEvent by remember(event.id) { mutableStateOf(event) }
    LaunchedEffect(event) { originalEvent = event }
    val isOccurrenceEdit = occurrenceDate != null
    val instanceDate = occurrenceDate ?: Instant.ofEpochMilli(event.startDateTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toEpochDay()
    var pendingStatus by remember(event.id, instanceDate) {
        mutableStateOf(
            allEventInstances.none {
                it.eventId == event.id && it.instanceDate == instanceDate
            }
        )
    }
    val occurrenceStartDateTime = remember(event.startDateTime, occurrenceDate) {
        occurrenceDate?.let { epochDay ->
            val zone = java.time.ZoneId.systemDefault()
            java.time.LocalDate.ofEpochDay(epochDay)
                .atTime(java.time.Instant.ofEpochMilli(event.startDateTime).atZone(zone).toLocalTime())
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        }
    }

    fun Long.toLocalStartOfDayMillis(): Long {
        val zone = ZoneId.systemDefault()
        return Instant.ofEpochMilli(this)
            .atZone(zone)
            .toLocalDate()
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }
    var isAllDay by rememberSaveable(event.id, occurrenceDate) {
        mutableStateOf(event.isAllDay)
    }
    var selectedTimeOfDayMillis by rememberSaveable(event.id, occurrenceDate) {
        val initialDateTime = occurrenceStartDateTime ?: event.startDateTime
        mutableLongStateOf(
            if (event.isAllDay) 9 * 60 * 60 * 1_000L
            else Instant.ofEpochMilli(initialDateTime)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .toNanoOfDay() / 1_000_000L
        )
    }
    var title by rememberSaveable { mutableStateOf(event.title) }
    var description by rememberSaveable { mutableStateOf(event.description) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showEventEndTimePicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showCustomNotificationDialog by remember { mutableStateOf(false) }
    var showRepetitionSheet by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var notificationOffsets by rememberSaveable(event.id, occurrenceDate) {
        mutableStateOf(event.effectiveReminderOffsets())
    }
    var selectedDateTime by rememberSaveable(event.id, occurrenceDate) {
        val initialDateTime = occurrenceStartDateTime ?: event.startDateTime
        mutableLongStateOf(
            if (event.isAllDay) initialDateTime.toLocalStartOfDayMillis()
            else initialDateTime
        )
    }
    var durationMillis by rememberSaveable(event.id, occurrenceDate) {
        mutableStateOf(event.configuredDurationMillis())
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentRecurrenceRule by remember { mutableStateOf(event.recurrence) }
    var eventEndsOn by rememberSaveable { mutableLongStateOf(event.endDateTime) }
    var neverEnds by rememberSaveable { mutableStateOf(event.endDateTime==-1L) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var discardMessage by remember { mutableStateOf<String?>(null) }
    var switchScopeAfterDiscard by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteChoiceDialog by remember { mutableStateOf(false) }
    var deletingOccurrence by remember { mutableStateOf(false) }
    var showDataCopyDialog by remember { mutableStateOf(false) }
    var selectedColorArgb by remember(event.id, occurrenceDate, eventOccurrenceColorArgb) {
        mutableStateOf(eventOccurrenceColorArgb)
    }
    val scope = rememberCoroutineScope()
    val rootPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.let(settingsViewModel::saveRootUri)
    }
    val attachmentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val attachmentEvent = if (isNewEvent) {
                event.copy(
                    title = title,
                    description = description,
                    startDateTime = selectedDateTime,
                    endDateTime = eventEndsOn,
                    durationMillis = durationMillis,
                    recurrence = currentRecurrenceRule,
                    isAllDay = isAllDay,
                    notificationOffset = notificationOffsets.firstOrNull() ?: 0L,
                    notificationOffsets = notificationOffsets,
                    colorArgb = selectedColorArgb ?: settings.data.defaultNewEventColorArgb
                )
            } else {
                event
            }
            onTaskEvents(TaskEvents.ImportEventAttachments(context, uris, attachmentEvent))
        }
    }
    val unsavedChangesMessage = stringResource(R.string.unsaved_changes_message)
    val switchScopeChangesMessage = stringResource(R.string.event_switch_scope_changes_message)
    val contentCopiedString = stringResource(R.string.content_copied)
    val contentMovedString = stringResource(R.string.content_moved)

    fun setAllDay(enabled: Boolean) {
        if (isAllDay == enabled) return
        val zone = ZoneId.systemDefault()
        if (enabled) {
            selectedTimeOfDayMillis = Instant.ofEpochMilli(selectedDateTime)
                .atZone(zone)
                .toLocalTime()
                .toNanoOfDay() / 1_000_000L
            selectedDateTime = selectedDateTime.toLocalStartOfDayMillis()
        } else {
            val date = Instant.ofEpochMilli(selectedDateTime).atZone(zone).toLocalDate()
            selectedDateTime = date.atTime(
                java.time.LocalTime.ofNanoOfDay(selectedTimeOfDayMillis * 1_000_000L)
            ).atZone(zone).toInstant().toEpochMilli()
        }
        isAllDay = enabled
    }

    fun applyEventColor(colorArgb: Int?) {
        selectedColorArgb = colorArgb
        if (!isNewEvent) {
            onTaskEvents(
                TaskEvents.SetEventColor(
                    event = event,
                    occurrenceDate = instanceDate,
                    colorArgb = colorArgb,
                    applyToSeries = !isOccurrenceEdit && !hasOccurrenceColorOverride
                )
            )
        }
    }

    fun openAttachment(fileName: String) {
        val storageRootUri = settings.data.storageRootUri
        if (storageRootUri == null) {
            android.widget.Toast.makeText(
                context,
                R.string.event_attachment_storage_unavailable,
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    DocumentFile.fromTreeUri(context, storageRootUri.toUri())
                        ?.findFile(Constants.File.OMNIPAD)
                        ?.findFile(Constants.File.OMNIPAD_DOCUMENTS)
                        ?.findFile(fileName)
                        ?.takeIf { it.isFile }
                        ?: throw IllegalStateException(
                            context.getString(R.string.event_attachment_not_found, fileName)
                        )
                }
                context.startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setDataAndType(file.uri, file.type ?: "application/octet-stream")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ActivityNotFoundException) {
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.event_attachment_no_viewer, fileName),
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } catch (exception: Exception) {
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.event_attachment_open_failed, fileName),
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    if (showColorPicker) {
        EventColorSelectionDialog(
            selectedColorArgb = selectedColorArgb,
            onColorSelected = {
                showColorPicker = false
                applyEventColor(it)
            },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showDeleteChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteChoiceDialog = false },
            title = { Text(stringResource(R.string.delete_event_scope_title)) },
            text = { Text(stringResource(R.string.delete_event_scope_message)) },
            dismissButton = null,
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        showDeleteChoiceDialog = false
                        deletingOccurrence = true
                        showDeleteDialog = true
                    }) { Text(stringResource(R.string.event_scope_this)) }
                    TextButton(onClick = {
                        showDeleteChoiceDialog = false
                        deletingOccurrence = false
                        showDeleteDialog = true
                    }) { Text(stringResource(R.string.event_scope_series)) }
                    TextButton(onClick = { showDeleteChoiceDialog = false }) {
                        Text(stringResource(R.string.Cancel))
                    }
                }
            }
        )
    }

    if (showDeleteDialog) {
        DeleteAlert(
            onDismissRequest = { showDeleteDialog = false },
            onConfirmation = {
                if (deletingOccurrence) {
                    onTaskEvents(
                        TaskEvents.DeleteOccurrence(
                            EventExceptionModel(
                                eventId = event.id,
                                occurrenceDate = instanceDate,
                                workspaceId = event.workspaceId
                            )
                        )
                    )
                } else {
                    onTaskEvents(TaskEvents.DeleteTask(event, context))
                }
                showDeleteDialog = false
                navController.popBackStack()
            },
            dialogTitle = stringResource(
                if (deletingOccurrence) R.string.delete_occurrence_title else R.string.deleteDialogTitle
            ),
            dialogText = stringResource(
                if (deletingOccurrence) R.string.delete_occurrence_message else R.string.deleteDialogText
            )
        )
    }

    if (showCustomNotificationDialog) {
        CustomNotificationDialog({
            showCustomNotificationDialog = false
        }) { offset -> notificationOffsets = (notificationOffsets + offset).distinct() }
    }

    if (showNotificationDialog) {
        EventNotificationDialog(
            currentOffset = notificationOffsets.firstOrNull() ?: 0L,
            onChange = { offset -> notificationOffsets = (notificationOffsets + offset).distinct() },
            onCustomClick = { showCustomNotificationDialog = true }) {
            showNotificationDialog = false
        }
    }

    if (showTimePicker) {
        TimePicker(
            initialTime = selectedDateTime,
            is24Hour = settings.data.is24HourFormat,
            onConfirm = {
                selectedDateTime = it
                selectedTimeOfDayMillis = Instant.ofEpochMilli(it)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
                    .toNanoOfDay() / 1_000_000L
            },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showEventEndTimePicker) {
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(selectedDateTime).atZone(zone).toLocalDateTime()
        val end = start.plus(durationMillis ?: 30 * 60_000L, java.time.temporal.ChronoUnit.MILLIS)
        TimePicker(
            initialTime = end.atZone(zone).toInstant().toEpochMilli(),
            is24Hour = settings.data.is24HourFormat,
            onConfirm = {
                val endTime = Instant.ofEpochMilli(it).atZone(zone).toLocalTime()
                durationMillis = durationUntilEndTime(start.toLocalTime(), endTime)
            },
            onDismiss = { showEventEndTimePicker = false }
        )
    }

    if (showStartDatePicker) {
        val zone = java.time.ZoneId.systemDefault()
        DatePickerModal(
            initialSelectedDateMillis = java.time.Instant.ofEpochMilli(selectedDateTime)
                .atZone(zone)
                .toLocalDate()
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
            onDateSelected = { dateMillis ->
                if (dateMillis != null) {
                    val localDate = java.time.Instant.ofEpochMilli(dateMillis)
                        .atZone(java.time.ZoneOffset.UTC)
                        .toLocalDate()
                    val localTime = java.time.Instant.ofEpochMilli(selectedDateTime)
                        .atZone(zone)
                        .toLocalTime()
                    selectedDateTime = localDate.atTime(localTime)
                        .atZone(zone)
                        .toInstant()
                        .toEpochMilli()
                }
            }
        ) {
            showStartDatePicker = false
        }
    }

    if (showEndDatePicker) {
        DatePickerModal(
            initialSelectedDateMillis = eventEndsOn.takeIf { it > 0L } ?: selectedDateTime,
            onDateSelected = { if (it != null) eventEndsOn = it }
        ) {
            showEndDatePicker = false
        }
    }

    fun saveEventIfPossible(): Boolean {
        if (isOccurrenceEdit) {
            val selectedOccurrenceDate = occurrenceDate
            val originalOccurrenceDateTime = occurrenceStartDateTime ?: event.startDateTime
            val hasOccurrenceChanges =
                title != originalEvent.title ||
                    description != originalEvent.description ||
                    selectedDateTime != originalOccurrenceDateTime ||
                    isAllDay != originalEvent.isAllDay ||
                    durationMillis != originalEvent.configuredDurationMillis() ||
                    notificationOffsets != originalEvent.effectiveReminderOffsets()

            if (!hasOccurrenceChanges) return true
            if (title.isBlank()) {
                discardMessage = null
                showDiscardDialog = true
                return false
            }

            val detachedEvent = EventModel(
                title = title,
                description = description,
                recurrence = RecurrenceRule.Once,
                startDateTime = selectedDateTime,
                endDateTime = -1L,
                durationMillis = durationMillis,
                isAllDay = isAllDay,
                notificationOffset = notificationOffsets.firstOrNull() ?: 0L,
                notificationOffsets = notificationOffsets,
                workspaceId = originalEvent.workspaceId,
                attachments = originalEvent.attachments,
                colorArgb = selectedColorArgb ?: originalEvent.colorArgb
            )
            onTaskEvents(
                TaskEvents.DetachOccurrence(
                    context,
                    EventExceptionModel(
                        eventId = originalEvent.id,
                        occurrenceDate = selectedOccurrenceDate,
                        workspaceId = originalEvent.workspaceId
                    ),
                    detachedEvent
                )
            )
            return true
        }

        val candidate = originalEvent.copy(
            title = title,
            startDateTime = selectedDateTime,
            description = description,
            endDateTime = if (currentRecurrenceRule == RecurrenceRule.Once && durationMillis == null) -1L else eventEndsOn,
            durationMillis = durationMillis,
            recurrence = currentRecurrenceRule,
            isAllDay = isAllDay,
            notificationOffset = notificationOffsets.firstOrNull() ?: 0L,
            notificationOffsets = notificationOffsets,
            colorArgb = if (hasOccurrenceColorOverride) originalEvent.colorArgb else selectedColorArgb
        )
        val hasContent = candidate != originalEvent.copy(durationMillis = originalEvent.configuredDurationMillis())

        if (!hasContent) { return true }

        if (title.isBlank()) {
            showDiscardDialog = true
            return false
        }

        onTaskEvents(TaskEvents.UpsertTask(context, candidate))
        return true
    }

    fun requestExit(switchScope: Boolean = false) {
        val hasChanges = if (isOccurrenceEdit) {
            title != originalEvent.title ||
                description != originalEvent.description ||
                selectedDateTime != (occurrenceStartDateTime ?: originalEvent.startDateTime) ||
                isAllDay != originalEvent.isAllDay ||
                durationMillis != originalEvent.configuredDurationMillis() ||
                notificationOffsets != originalEvent.effectiveReminderOffsets()
        } else {
            originalEvent.copy(
                title = title,
                startDateTime = selectedDateTime,
                description = description,
                endDateTime = if (currentRecurrenceRule == RecurrenceRule.Once && durationMillis == null) -1L else eventEndsOn,
                durationMillis = durationMillis,
                recurrence = currentRecurrenceRule,
                isAllDay = isAllDay,
                notificationOffset = notificationOffsets.firstOrNull() ?: 0L,
                notificationOffsets = notificationOffsets,
                colorArgb = if (hasOccurrenceColorOverride) originalEvent.colorArgb else selectedColorArgb
            ) != originalEvent.copy(durationMillis = originalEvent.configuredDurationMillis())
        }
        if (hasChanges) {
            discardMessage = if (switchScope) switchScopeChangesMessage else unsavedChangesMessage
            switchScopeAfterDiscard = switchScope
            showDiscardDialog = true
        } else if (switchScope) {
            onSwitchScope?.invoke()
        } else {
            navController.popBackStack()
        }
    }
    BackHandler(enabled = !showDiscardDialog) { requestExit() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.surfaceContainerLow),
                title = {
                    Text(
                        stringResource(
                            when {
                                isNewEvent -> R.string.create_event
                                occurrenceDate == null && event.recurrence != RecurrenceRule.Once ->
                                    R.string.event_edit_series
                                else -> R.string.Edit_Event
                            }
                        ),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton({ requestExit() }) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showColorPicker = true }) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = stringResource(R.string.event_color_title)
                        )
                    }
                    if (!isNewEvent) {
                        IconButton(
                            onClick = {
                                pendingStatus = !pendingStatus
                                onTaskEvents(
                                    TaskEvents.ToggleStatus(
                                        !pendingStatus,
                                        event.id,
                                        event.workspaceId,
                                        instanceDate
                                    )
                                )
                            }
                        ) {
                            Icon(
                                if (pendingStatus) Icons.Default.Verified else Icons.Default.Pending,
                                contentDescription = stringResource(
                                    if (pendingStatus) R.string.mark_completed else R.string.mark_pending
                                )
                            )
                        }
                        EventDropdownMenu(
                            onExport = {
                                onTaskEvents(TaskEvents.ExportIcsEvents(context, event))
                            },
                            onDelete = {
                                if (isOccurrenceEdit && event.recurrence != RecurrenceRule.Once) {
                                    showDeleteChoiceDialog = true
                                } else {
                                    deletingOccurrence = false
                                    showDeleteDialog = true
                                }
                            },
                            onCopy = { showDataCopyDialog = true },
                            onClone = {
                                onTaskEvents(
                                    TaskEvents.UpsertTask(
                                        context,
                                        event.copy(
                                            id = java.util.UUID.randomUUID().toString(),
                                            title = "Clone ${event.title}",
                                            startDateTime = System.currentTimeMillis(),
                                            endDateTime = -1L
                                        )
                                    )
                                )
                                android.widget.Toast.makeText(
                                    context,
                                    R.string.clone_created_successfully,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        if (saveEventIfPossible()) navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Text(
                        stringResource(if (isNewEvent) R.string.create_event else R.string.save),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    ) { innerPadding ->
        val previewColorArgb = eventEditorPreviewColor(
            selectedColorArgb = selectedColorArgb,
            seriesColorArgb = event.colorArgb,
            useSeriesFallback = isOccurrenceEdit || hasOccurrenceColorOverride,
            isNewEvent = isNewEvent,
            defaultNewEventColorArgb = settings.data.defaultNewEventColorArgb,
            completedColorArgb = settings.data.completedEventColorArgb,
            isPending = pendingStatus
        )
        val previewBackground = previewColorArgb?.let(::Color)
            ?: if (isNewEvent || !pendingStatus) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        val previewContent = previewColorArgb?.let(::eventTextColor)
            ?: if (isNewEvent || !pendingStatus) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.size(width = 1.dp, height = 16.dp))
            if (onSwitchScope != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isOccurrenceEdit,
                        onClick = { if (!isOccurrenceEdit) requestExit(switchScope = true) },
                        label = { Text(stringResource(R.string.event_scope_this)) }
                    )
                    FilterChip(
                        selected = !isOccurrenceEdit,
                        onClick = { if (isOccurrenceEdit) requestExit(switchScope = true) },
                        label = { Text(stringResource(R.string.event_scope_series)) }
                    )
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = previewBackground,
                    contentColor = previewContent
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = previewContent.copy(alpha = 0.12f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            modifier = Modifier.padding(14.dp).size(28.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.event_preview),
                            style = MaterialTheme.typography.labelMedium,
                            color = previewContent.copy(alpha = 0.72f)
                        )
                        Text(
                            title.ifBlank { stringResource(R.string.event_untitled) },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (isAllDay) {
                                "${selectedDateTime.toFormattedDate()} • ${stringResource(R.string.event_all_day)}"
                            } else {
                                "${selectedDateTime.toFormattedDate()} • ${convertMillisToTime(selectedDateTime, settings.data.is24HourFormat)}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = previewContent.copy(alpha = 0.82f)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EventFormSectionTitle(stringResource(R.string.event_details_section))
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = title,
                            onValueChange = { title = it },
                            shape = RoundedCornerShape(18.dp),
                            placeholder = { Text(stringResource(R.string.Title)) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            colors = getTextFieldColors()
                        )
                        TextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text(stringResource(R.string.Description)) },
                            textStyle = MaterialTheme.typography.bodyLarge,
                            shape = RoundedCornerShape(18.dp),
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Default
                            ),
                            colors = getTextFieldColors()
                        )
                    }
                }
            }

            val canAddAttachments = isNewEvent || isOccurrenceEdit || event.recurrence == RecurrenceRule.Once
            if (title.isNotBlank() && (canAddAttachments || event.attachments.isNotEmpty())) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventFormSectionTitle(stringResource(R.string.event_attachments))
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (canAddAttachments) {
                                TextButton(
                                    onClick = {
                                        ensureStorageRoot(scope, settingsViewModel, rootPicker) {
                                            attachmentPicker.launch(arrayOf("*/*"))
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null)
                                    Spacer(Modifier.size(8.dp))
                                    Text(stringResource(R.string.event_add_attachments))
                                }
                            }
                            event.attachments.forEach { fileName ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { openAttachment(fileName) }
                                        .padding(start = 8.dp, end = 0.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null)
                                    Spacer(Modifier.size(12.dp))
                                    Text(
                                        fileName,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    IconButton(
                                        onClick = {
                                            onTaskEvents(
                                                TaskEvents.RemoveEventAttachment(event.id, fileName)
                                            )
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = stringResource(
                                                R.string.event_remove_attachment,
                                                fileName
                                            ),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EventFormSectionTitle(stringResource(R.string.event_schedule_section))
                if (isOccurrenceEdit) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            stringResource(R.string.independent_event_notice),
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    if (event.recurrence != RecurrenceRule.Once) {
                        EventFormSettingCard(
                            icon = { Icon(Icons.Default.Repeat, contentDescription = null) },
                            label = stringResource(R.string.event_series_schedule),
                            value = getRecurrenceText(context, event.recurrence, event.startDateTime),
                            onClick = null
                        )
                        if (event.endDateTime > 0L) {
                            EventFormSettingCard(
                                icon = { Icon(Icons.Default.Today, contentDescription = null) },
                                label = stringResource(R.string.ends_on),
                                value = event.endDateTime.toFormattedDate(),
                                onClick = null
                            )
                        }
                    }
                }
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Today, contentDescription = null)
                            Text(
                                stringResource(R.string.event_all_day),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Switch(
                            checked = isAllDay,
                            onCheckedChange = { setAllDay(it) }
                        )
                    }
                }
                EventFormSettingCard(
                    icon = { Icon(Icons.Default.Today, contentDescription = null) },
                    label = stringResource(R.string.date),
                    value = selectedDateTime.toFormattedDate(),
                    onClick = { showStartDatePicker = true }
                )
                if (!isAllDay) {
                    EventFormSettingCard(
                        icon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                        label = stringResource(R.string.time),
                        value = convertMillisToTime(selectedDateTime, settings.data.is24HourFormat),
                        onClick = { showTimePicker = true }
                    )
                    val start = Instant.ofEpochMilli(selectedDateTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    val end = durationMillis?.let { start.plus(it, java.time.temporal.ChronoUnit.MILLIS) }
                    val endLabel = if (end == null) {
                        stringResource(R.string.event_no_end_time)
                    } else {
                        val time = convertMillisToTime(end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), settings.data.is24HourFormat)
                        when {
                            end.toLocalDate() == start.toLocalDate().plusDays(1) ->
                                stringResource(R.string.event_end_time_next_day, time)
                            end.toLocalDate().isAfter(start.toLocalDate()) ->
                                stringResource(R.string.event_end_time_with_date, time, formatAgendaDate(end.toLocalDate()))
                            else -> time
                        }
                    }
                    EventFormSettingCard(
                        icon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                        label = stringResource(R.string.event_end_time),
                        value = endLabel,
                        onClick = { showEventEndTimePicker = true }
                    )
                    if (durationMillis != null) {
                        TextButton(onClick = { durationMillis = null }, modifier = Modifier.align(Alignment.End)) {
                            Text(stringResource(R.string.event_clear_end_time))
                        }
                    }
                }
                if (!isOccurrenceEdit) {
                    EventFormSettingCard(
                        icon = { Icon(Icons.Default.Repeat, contentDescription = null) },
                        label = stringResource(R.string.event_repeat),
                        value = currentRecurrenceRule.label(),
                        onClick = { showRepetitionSheet = true }
                    )
                }
                if (!isOccurrenceEdit) when (val rule = currentRecurrenceRule) {
                    is RecurrenceRule.Once -> Unit

                    is RecurrenceRule.Custom -> {
                        Text(
                            formatCustom(rule),
                            modifier = Modifier.padding(start = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is RecurrenceRule.Interval -> {
                        Text(
                            intervalRecurrenceText(rule),
                            modifier = Modifier.padding(start = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is RecurrenceRule.Monthly -> {
                        Text(
                            formatMonthly(selectedDateTime),
                            modifier = Modifier.padding(start = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is RecurrenceRule.Yearly -> {
                        Text(
                            formatYearly(selectedDateTime),
                            modifier = Modifier.padding(start = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            maxItemsInEachRow = 7
                        ) {
                            weekdays.forEachIndexed { index, day ->
                                val isSelected = index in rule.daysOfWeek

                                Card(
                                    shape = RoundedCornerShape(50),
                                    colors = CardDefaults.cardColors(
                                        containerColor =
                                            if (isSelected)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceContainerHigh,

                                        contentColor =
                                            if (isSelected)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(
                                        text = day,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodyMedium,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 7.dp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }

            if(!isOccurrenceEdit && currentRecurrenceRule!= RecurrenceRule.Once){
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventFormSectionTitle(stringResource(R.string.event_end_section))
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Flag, null, tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text(stringResource(R.string.never_ends), style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            stringResource(R.string.event_end_hint),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(neverEnds, onCheckedChange = {
                                    if (neverEnds) {
                                        neverEnds = false
                                        eventEndsOn = max(selectedDateTime, System.currentTimeMillis())
                                    } else {
                                        neverEnds = true
                                        eventEndsOn = -1L
                                    }
                                })
                            }
                            if (!neverEnds) {
                                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                                EventFormSettingCard(
                                    icon = { Icon(Icons.Default.Today, contentDescription = null) },
                                    label = stringResource(R.string.ends_on),
                                    value = eventEndsOn.toFormattedDate(),
                                    onClick = { showEndDatePicker = true }
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EventFormSectionTitle(stringResource(R.string.event_reminder_section))
                if (notificationOffsets.isEmpty()) {
                    Text(
                        stringResource(R.string.event_no_reminders),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                notificationOffsets.forEachIndexed { index, offset ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    stringResource(R.string.event_reminder_number, index + 1),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    getNotificationText(offset),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(
                                onClick = {
                                    notificationOffsets = notificationOffsets.toMutableList()
                                        .also { it.removeAt(index) }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.event_remove_reminder)
                                )
                            }
                        }
                    }
                }
                EventFormSettingCard(
                    icon = { Icon(Icons.Outlined.NotificationsActive, contentDescription = null) },
                    label = stringResource(R.string.Add_Notification),
                    value = stringResource(
                        if (notificationOffsets.isEmpty()) {
                            R.string.event_choose_reminder
                        } else {
                            R.string.event_add_another_reminder
                        }
                    ),
                    onClick = { showNotificationDialog = true }
                )
            }
            Spacer(Modifier.size(width = 1.dp, height = 20.dp))
        }
    }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            onDiscard = {
                showDiscardDialog = false
                if (switchScopeAfterDiscard) {
                    onSwitchScope?.invoke()
                } else {
                    navController.popBackStack()
                }
            },
            onDismiss = {
                showDiscardDialog=false
                discardMessage = null
                switchScopeAfterDiscard = false
            },
            message = discardMessage
        )
    }

    RecurrenceBottomSheet(
        isVisible = showRepetitionSheet,
        sheetState = sheetState,
        startDateTime = selectedDateTime,
        onDismiss = { showRepetitionSheet = false },
        currentRule = currentRecurrenceRule
    ) { newRule, newStart ->
        currentRecurrenceRule = newRule
        selectedDateTime = if (isAllDay) newStart.toLocalStartOfDayMillis() else newStart
    }

    if (showDataCopyDialog) {
        DataCopyDialog(
            workspaces.filterNot { it.workspaceId == event.workspaceId },
            { dataCopyType, selectedWorkspaces ->
                if (selectedWorkspaces.isEmpty()) return@DataCopyDialog
                selectedWorkspaces.forEach { workspace ->
                    if (3 !in workspace.selectedSpaces) {
                        onWorkspaceEvents(
                            WorkspaceEvents.UpsertSpace(
                                workspace.copy(selectedSpaces = workspace.selectedSpaces + 3)
                            )
                        )
                    }
                    onTaskEvents(
                        TaskEvents.UpsertTask(
                            context,
                            EventModel(
                                title = title,
                                description = description,
                                recurrence = event.recurrence,
                                endDateTime = event.endDateTime,
                                notificationOffset = event.notificationOffset,
                                notificationOffsets = event.notificationOffsets,
                                workspaceId = workspace.workspaceId,
                                attachments = event.attachments,
                                colorArgb = selectedColorArgb
                            )
                        )
                    )
                }

                if (dataCopyType == DataCopyType.COPY) {
                    android.widget.Toast.makeText(
                        context,
                        contentCopiedString,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    onTaskEvents(TaskEvents.DeleteTask(event, context))
                    navController.popBackStack()
                    android.widget.Toast.makeText(
                        context,
                        contentMovedString,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        ) { showDataCopyDialog = false }
    }
}

@Composable
private fun EventFormSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun EventFormSettingCard(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    onClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}