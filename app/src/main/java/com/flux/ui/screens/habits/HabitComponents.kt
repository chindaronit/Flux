package com.flux.ui.screens.habits

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.IncompleteCircle
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.flux.R
import com.flux.data.model.HabitConfig
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import com.flux.data.model.isCompleted
import com.flux.ui.common.convertMillisToDate
import com.flux.ui.common.convertMillisToTime
import com.flux.ui.events.HabitEvents
import com.flux.ui.screens.analytics.HeatMapCard
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import com.flux.data.model.RecurrenceRule
import com.flux.ui.screens.events.IconRadioButton
import java.time.DayOfWeek
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import kotlin.collections.filter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import com.flux.data.model.isCounted
import com.flux.data.model.isLive
import com.flux.data.model.isTimed
import com.flux.ui.common.FilterCategory
import com.flux.ui.common.FilterOption
import com.flux.ui.common.SelectionType
import com.flux.ui.screens.settings.CircleWrapper
import com.flux.ui.screens.settings.shapeManager
import androidx.compose.ui.platform.LocalLocale

fun toMillis(hours: Int, minutes: Int): Long =
    (hours * 60L * 60_000L) + (minutes * 60_000L)

// ------------- Dialogs -------------
@Composable
fun TimerDialog(
    durationMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val totalMinutes = durationMillis / 60_000L
    var hours by remember { mutableIntStateOf((totalMinutes / 60).toInt()) }
    var minutes by remember { mutableIntStateOf((totalMinutes % 60).toInt()) }

    // Prevent invalid state
    LaunchedEffect(hours, minutes) {
        if (hours == 0 && minutes == 0) {
            minutes = 1
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(16.dp).fillMaxWidth()) {
                NumberPickerRow(Modifier.fillMaxWidth(), hours, minutes, { hours = it }) {
                    minutes = it
                }
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onConfirm(toMillis(hours, minutes))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.Confirm))
                }
            }
        }
    }
}

@Composable
fun CountedHabitStatus(
    radius: Int,
    habit: HabitModel,
    instance: HabitInstanceModel?,
    onHabitEvents: (HabitEvents) -> Unit
) {

    val goal = (habit.habitConfig as HabitConfig.Counted).goal

    // Single source of truth
    val currentCount = instance?.count ?: 0

    val completion = (currentCount.toFloat() / goal.toFloat())
        .coerceIn(0f, 1f)

    val completionPercentage = (completion * 100).toInt()

    val todayEpoch = LocalDate.now().toEpochDay()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(
            MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        ),
        onClick = {}
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                "Current Habit Status",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                CircularProgressIndicator(
                    progress = { completion },
                    modifier = Modifier.size(120.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.primary.copy(0.35f),
                    strokeCap = StrokeCap.Round,
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    HabitInfoComponent(
                        Icons.Outlined.IncompleteCircle,
                        "Current",
                        currentCount.toString()
                    )

                    HabitInfoComponent(
                        Icons.Default.TrackChanges,
                        "Goal",
                        goal.toString()
                    )

                    HabitInfoComponent(
                        Icons.Default.Percent,
                        "Completion",
                        completionPercentage.toString()
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                FilledTonalButton(
                    onClick = {

                        val count = maxOf(currentCount - 1, 0)

                        val newInstance = HabitInstanceModel(
                            instanceDate = todayEpoch,
                            habitId = habit.id,
                            workspaceId = habit.workspaceId,
                            count = count
                        )

                        onHabitEvents(
                            HabitEvents.UpdateInstance(
                                newInstance,
                                habit.habitConfig
                            )
                        )
                    },
                    modifier = Modifier.weight(0.5f)
                ) {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            Icons.Default.Remove,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text("Decrement")
                    }
                }

                FilledTonalButton(
                    onClick = {

                        val count = currentCount + 1

                        val newInstance = HabitInstanceModel(
                            instanceDate = todayEpoch,
                            habitId = habit.id,
                            workspaceId = habit.workspaceId,
                            count = count
                        )

                        onHabitEvents(
                            HabitEvents.UpdateInstance(
                                newInstance,
                                habit.habitConfig
                            )
                        )
                    },
                    modifier = Modifier.weight(0.5f)
                ) {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            Icons.Default.Add,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text("Increment")
                    }
                }
            }
        }
    }
}

@Composable
fun TimedHabitStatus (
    radius: Int,
    habit: HabitModel,
    instance: HabitInstanceModel?,
    onHabitEvents: (HabitEvents) -> Unit
) {
    val goal = (habit.habitConfig as HabitConfig.Timed).durationMillis
    var timeSpent by remember { mutableLongStateOf(instance?.timeSpent?: 0L) }
    val completion = (timeSpent.toDouble()/goal.toDouble()).toFloat()
    val completionPercentage = (completion*100).toInt()
    val todayEpoch = LocalDate.now().toEpochDay()
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(0) }

    // Prevent invalid state
    LaunchedEffect(hours, minutes) {
        if (hours == 0 && minutes == 0) {
            minutes = 1
        }
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    val options = listOf("Countdown", "Manual")
    val icons = listOf(Icons.Default.HourglassTop, Icons.Default.Edit)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)),
        onClick = {}
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Current Habit Status",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    progress = { completion },
                    modifier = Modifier.size(120.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.primary.copy(0.35f),
                    strokeCap = StrokeCap.Round,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HabitInfoComponent(
                        Icons.Outlined.IncompleteCircle,
                        "Current",
                        formatDuration(timeSpent)
                    )
                    HabitInfoComponent(
                        Icons.Default.TrackChanges,
                        "Goal",
                        formatDuration(goal)
                    )
                    HabitInfoComponent(
                        Icons.Default.Percent,
                        "Completion",
                        completionPercentage.toString()
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(vertical = 6.dp)) {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        ),
                        onClick = { selectedIndex = index },
                        selected = index == selectedIndex,
                        label = { Text(label) },
                        icon = { Icon(icons[index], null) }
                    )
                }
            }

            NumberPickerRow(Modifier.fillMaxWidth().padding(horizontal = 32.dp), hours, minutes, {hours=it}) { minutes=it }

            FilledTonalButton(
                {
                    timeSpent += toMillis(hours, minutes)

                    val newInstance = HabitInstanceModel(
                        instanceDate = todayEpoch,
                        habitId = habit.id,
                        workspaceId = habit.workspaceId,
                        timeSpent = timeSpent
                    )
                    onHabitEvents(HabitEvents.UpdateInstance(newInstance, habit.habitConfig))
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Icon(Icons.Default.Add, null)
                    Text("Log time")
                }
            }
        }
    }
}

@Composable
fun NumberPickerRow(
    modifier: Modifier = Modifier,
    hours: Int,
    minutes: Int,
    onHourChange: (Int)-> Unit,
    onMinutesChange: (Int)->Unit
) {
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Hours", color = MaterialTheme.colorScheme.primary)
            NumberPicker(
                range = 0..23,
                selected = hours,
                onValueChange = onHourChange
            )
        }

        VerticalDivider(
            modifier = Modifier
                .height(150.dp)
                .align(Alignment.CenterVertically),
            color = Color.Gray
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Minutes", color = MaterialTheme.colorScheme.primary)
            NumberPicker(
                range = 0..59,
                selected = minutes,
                onValueChange = onMinutesChange,
                invalidValue = if (hours == 0) 0 else null
            )
        }
    }
}

@Composable
fun NumberPicker(
    range: IntRange,
    selected: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    invalidValue: Int? = null
) {
    val count = range.count()
    val startOffset = (Int.MAX_VALUE / 2) - (Int.MAX_VALUE / 2) % count

    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val itemHeightDp = 50.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeightDp.toPx() }

    val initialIndex = startOffset + (selected - range.first)

    val haptic = LocalHapticFeedback.current

    // Sync scroll with selected value
    LaunchedEffect(selected, range) {
        listState.scrollToItem(initialIndex)
    }

    val centeredIndex by remember(listState, itemHeightPx) {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset
            if (offset > itemHeightPx / 2)
                listState.firstVisibleItemIndex + 1
            else
                listState.firstVisibleItemIndex
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect {
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
    }

    var lastCenter by remember { mutableIntStateOf(centeredIndex) }

    LaunchedEffect(listState, range, invalidValue) {
        snapshotFlow { centeredIndex }
            .distinctUntilChanged()
            .collect { index ->

                if (index != lastCenter) {
                    // stronger feedback on snap
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    lastCenter = index
                }

                val normalized =
                    ((index - startOffset) % count + count) % count

                var value = range.first + normalized

                // prevent invalid selection BEFORE applying
                if (invalidValue != null && value == invalidValue) {
                    value = range.first + ((normalized + 1) % count)
                }

                onValueChange(value)
            }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = itemHeightDp),
        modifier = modifier.height(itemHeightDp * 3)
    ) {
        items(Int.MAX_VALUE) { index ->
            val normalized =
                ((index - startOffset) % count + count) % count
            val value = range.first + normalized
            val isSelected = index == centeredIndex

            val isInvalid = invalidValue != null && value == invalidValue

            Text(
                text = value.toString().padStart(2, '0'),
                fontSize = if (isSelected) 28.sp else 18.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isInvalid -> Color.LightGray
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> Color.Gray
                },
                modifier = Modifier
                    .height(itemHeightDp)
                    .wrapContentHeight(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun TimedHabitDialog(
    durationMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
){
    val totalMinutes = durationMillis / 60_000L
    var hours by remember { mutableIntStateOf((totalMinutes / 60).toInt()) }
    var minutes by remember { mutableIntStateOf((totalMinutes % 60).toInt()) }

    // Prevent invalid state
    LaunchedEffect(hours, minutes) {
        if (hours == 0 && minutes == 0) {
            minutes = 1
        }
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    val options = listOf("Countdown", "Manual")
    val icons = listOf(Icons.Default.HourglassTop, Icons.Default.Edit)

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(16.dp).fillMaxWidth()) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(vertical = 6.dp)) {
                    options.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size
                            ),
                            onClick = { selectedIndex = index },
                            selected = index == selectedIndex,
                            label = { Text(label) },
                            icon = { Icon(icons[index], null) }
                        )
                    }
                }

                NumberPickerRow(
                    Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    hours,
                    minutes,
                    { hours=it }
                ) { minutes=it }
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onConfirm(toMillis(hours, minutes))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.Confirm))
                }
            }
        }
    }
}

@Composable
fun HabitDetailedInfo(
    radius: Int,
    habit: HabitModel,
    instances: List<HabitInstanceModel>
){
    val streakData = remember(habit, instances) {
        calculateStreaks(habit.recurrence, habit.startDateTime, habit.habitConfig, instances)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)),
        onClick = {}
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center)
            {
                Icon(Icons.Default.EventAvailable, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if(habit.description.isNotBlank()){
                Text(
                    text = habit.description,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Light),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 1000.dp)
                    .padding(vertical = 12.dp)
            ) {
                item {
                    HabitInfoComponent(
                        Icons.Default.OutlinedFlag,
                        stringResource(R.string.Started),
                        convertMillisToDate(habit.startDateTime))
                }
                if(habit.endDateTime != -1L){
                    item {
                        HabitInfoComponent(
                            Icons.Default.SportsScore,
                            stringResource(R.string.ends_on),
                            convertMillisToDate(habit.endDateTime)
                        )
                    }
                }
                item {
                    HabitInfoComponent(
                        Icons.Default.LocalFireDepartment,
                        "Current ${streakData.currentStreak}",
                        "${stringResource(R.string.Best_Streak)} ${streakData.bestStreak}"
                    )
                }

                item {
                    val habitTypeIcon = when (habit.habitConfig) {
                        is HabitConfig.Simple -> Icons.Default.CheckCircleOutline
                        is HabitConfig.Counted -> Icons.Default.Numbers
                        else ->  Icons.Default.Timer
                    }
                    val habitTypeText = when (habit.habitConfig) {
                        is HabitConfig.Timed -> "Timed"
                        is HabitConfig.Counted -> "Counter"
                        else -> "Simple"
                    }
                    HabitInfoComponent(
                        habitTypeIcon,
                        "Type",
                        habitTypeText
                    )
                }

                if(habit.habitConfig is HabitConfig.Simple || habit.habitConfig is HabitConfig.Timed) {
                    item {
                        HabitInfoComponent(
                            Icons.Outlined.NotificationsActive,
                            "Remind at",
                            convertMillisToTime(habit.startDateTime)
                        )
                    }
                }

                if(habit.habitConfig is HabitConfig.Timed) {
                    item {
                        HabitInfoComponent(
                            Icons.Outlined.Timer,
                            "Timer",
                            formatDuration(habit.habitConfig.durationMillis)
                        )
                    }
                }

                if(habit.habitConfig is HabitConfig.Counted) {
                    item {
                        HabitInfoComponent(
                            Icons.Outlined.Circle,
                            "Active from",
                            convertMillisToTime(habit.habitConfig.activeStartTime)
                        )
                    }

                    item {
                        HabitInfoComponent(
                            Icons.Outlined.StopCircle,
                            "Active uptil",
                            convertMillisToTime(habit.habitConfig.activeEndTime)
                        )
                    }

                    item {
                        HabitInfoComponent(
                            Icons.Outlined.NotificationsActive,
                            "Notify Every",
                            formatDuration(habit.habitConfig.intervalMillis)
                        )
                    }

                    item {
                        HabitInfoComponent(
                            Icons.Default.TrackChanges,
                            "Goal",
                            habit.habitConfig.goal.toString()
                        )
                    }

                    if(habit.habitConfig.unit.isNotBlank()){
                        item {
                            HabitInfoComponent(
                                Icons.Default.AcUnit,
                                "Unit",
                                habit.habitConfig.unit
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabitInfoComponent(icon: ImageVector, title: String, description: String){
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CircleWrapper(MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Column {
            Text(title, modifier = Modifier.alpha(0.85f), style = MaterialTheme.typography.labelMedium)
            Text(description, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun SingleHabitHeatMap(
    radius: Int,
    habit: HabitModel,
    instances: List<HabitInstanceModel>,
){
    val today = LocalDate.now()
    val yearStart = LocalDate.of(today.year, 1, 1)

    // Calculate the offset from Monday for January 1st
    val jan1DayOfWeek = yearStart.dayOfWeek.value // Monday = 1, Sunday = 7
    val offsetFromMonday = jan1DayOfWeek - 1 // 0 for Monday, 6 for Sunday

    val totalDays = ChronoUnit.DAYS.between(yearStart, today).toInt() + 1
    val allDates = (0 until totalDays).map { yearStart.plusDays(it.toLong()) }

    val heatMap = remember(instances, habit) {
        instances
            .groupBy { LocalDate.ofEpochDay(it.instanceDate) }
            .mapValues { (_, instancesForDay) ->
                instancesForDay.count { instance ->
                    instance.isCompleted(habit)
                }
            }
    }

    // Create week columns with proper day alignment
    val weekColumns = mutableListOf<List<LocalDate?>>()
    var currentWeek = MutableList<LocalDate?>(7) { null }

    // Fill the first week with nulls for days before January 1st
    for (i in 0 until offsetFromMonday) {
        currentWeek[i] = null
    }

    // Add all dates starting from the correct day of week
    allDates.forEachIndexed { index, date ->
        val dayIndex = (offsetFromMonday + index) % 7
        currentWeek[dayIndex] = date

        // When we complete a week (reach Sunday) or it's the last date
        if (dayIndex == 6 || index == allDates.size - 1) {
            weekColumns.add(currentWeek.toList())
            currentWeek = MutableList(7) { null }
        }
    }
    val boxSize = 24.dp
    val lazyListState = rememberLazyListState()

    // Calculate the index of the current month's first week
    val currentMonthStartIndex = remember(weekColumns) {
        val currentMonth = today.month
        weekColumns.indexOfFirst { week ->
            week.any { date -> date?.month == currentMonth }
        }.takeIf { it != -1 } ?: 0
    }

    // Auto-scroll to current month on first composition
    LaunchedEffect(currentMonthStartIndex) {
        if (currentMonthStartIndex > 0) {
            lazyListState.scrollToItem(index = maxOf(0, currentMonthStartIndex - 2))
        }
    }

    HeatMapCard(
        radius,
        "This Year",
        "",
        boxSize,
        2,
        lazyListState,
        weekColumns,
        heatMap.toMap()
    )
}

// ------------- Card -------------
@Composable
fun HabitDateCard(
    radius: Int,
    isTodayDone: Boolean,
    isDone: Boolean,
    day: String,
    date: Int,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isTodayDone && isDone -> MaterialTheme.colorScheme.primary
        isTodayDone && !isDone -> MaterialTheme.colorScheme.primaryContainer
        isDone -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
    }

    val contentColor = when {
        isDone -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier,
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                day.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraLight),
                modifier = Modifier.alpha(0.95f)
            )
            Text(
                date.toString(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraLight),
                modifier = Modifier.alpha(0.95f)
            )
        }
    }
}

@Composable
fun HabitPreviewCard(
    radius: Int,
    is24HourFormat: Boolean,
    habit: HabitModel,
    instances: List<HabitInstanceModel>,
    onClick: (Long) -> Unit,
    onAnalyticsClicked: () -> Unit
) {
    val todayEpoch = LocalDate.now().toEpochDay()
    val instance = instances.firstOrNull { it.instanceDate == todayEpoch }
    val isTodayDone = instance?.isCompleted(habit) ?: false

    // Get Monday of this week
    val mondayEpoch = LocalDate.now().with(DayOfWeek.MONDAY).toEpochDay()
    val weekDates = (0L..6L).map { mondayEpoch + it }
    val (currentStreak, _) = calculateStreaks(habit.recurrence, habit.startDateTime, habit.habitConfig, instances)
    val habitTypeIcon = when (habit.habitConfig) {
        is HabitConfig.Simple -> Icons.Default.CheckCircleOutline
        is HabitConfig.Counted -> Icons.Default.Numbers
        else ->  Icons.Default.Timer
    }

    val habitTypeText = when (habit.habitConfig) {
        is HabitConfig.Timed -> formatDuration(habit.habitConfig.durationMillis)
        is HabitConfig.Counted -> habit.habitConfig.goal.toString()
        else -> ""
    }

    val habitTimeText = when (habit.habitConfig) {
        is HabitConfig.Counted -> "${convertMillisToTime(habit.habitConfig.activeStartTime, is24HourFormat)} - ${convertMillisToTime(habit.habitConfig.activeEndTime, is24HourFormat)}"
        else -> convertMillisToTime(habit.startDateTime, is24HourFormat)
    }

    Card(
        onClick = { onClick(todayEpoch) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTodayDone) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        ),
        shape = shapeManager(radius = radius * 2)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    IconRadioButton(
                        selected = isTodayDone,
                    ) { onClick(todayEpoch) }

                    Column(Modifier.padding(top = 8.dp)) {
                        Text(
                            habit.title,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            habitTimeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraLight),
                            modifier = Modifier.alpha(0.9f)
                        )
                    }
                }

                Row(modifier = Modifier.padding(end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton({}, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.LocalFireDepartment, null) }
                    Text("$currentStreak", modifier = Modifier.padding(end = 4.dp), style = MaterialTheme.typography.labelLarge)
                    IconButton({}, modifier = Modifier.size(36.dp)) { Icon(habitTypeIcon, null) }
                    if(habit.habitConfig is HabitConfig.Counted || habit.habitConfig is HabitConfig.Timed){
                        Text(habitTypeText, modifier = Modifier.padding(end = 4.dp), style = MaterialTheme.typography.labelLarge)
                    }
                    IconButton(onAnalyticsClicked, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Analytics, null) }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekDates.forEach { epochDay ->
                    val localDate = LocalDate.ofEpochDay(epochDay)
                    val instance = instances.firstOrNull { it.instanceDate == epochDay }
                    val isCardDone = instance?.isCompleted(habit) ?: false

                    when(habit.habitConfig) {
                        is HabitConfig.Simple ->
                            HabitDateCard(
                                radius = radius,
                                isTodayDone = isTodayDone,
                                isDone = isCardDone,
                                day = localDate.dayOfWeek.name.take(3),
                                date = localDate.dayOfMonth,
                                modifier = Modifier.weight(1f)
                            )
                        else ->
                            OtherConfigCard(
                                radius = radius,
                                instance = instance,
                                isTimed = habit.isTimed,
                                isTodayDone=isTodayDone,
                                isDone = isCardDone,
                                day = localDate.dayOfWeek.name.take(1),
                                date = localDate.dayOfMonth,
                                modifier = Modifier.weight(1f)
                            )
                    }
                }
            }
        }
    }
}

@Composable
fun OtherConfigCard(
    radius: Int,
    isTimed: Boolean,
    instance: HabitInstanceModel?,
    isTodayDone: Boolean,
    isDone: Boolean,
    day: String,
    date: Int,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isTodayDone && isDone -> MaterialTheme.colorScheme.primary
        isTodayDone && !isDone -> MaterialTheme.colorScheme.primaryContainer
        isDone -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
    }

    val contentColor = when {
        isDone -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val text = if (isTimed) { formatDuration(instance?.timeSpent ?: 0L) } else { (instance?.count ?: 0).toString() }
    Card(
        modifier = modifier,
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row{
                Text(
                    "$date, ",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraLight),
                    modifier = Modifier.alpha(0.95f)
                )

                Text(
                    day.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraLight),
                    modifier = Modifier.alpha(0.95f)
                )
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraLight),
                modifier = Modifier.alpha(0.85f)
            )
        }
    }
}

@Composable
fun HabitCalendarCard(
    radius: Int,
    habit: HabitModel,
    habitInstances: List<HabitInstanceModel>,
    onHabitEvents: (HabitEvents) -> Unit
) {
    val context = LocalContext.current
    val startDateTime = habit.startDateTime
    val endDateTime = habit.endDateTime
    val recurrence = habit.recurrence
    val workspaceId = habit.workspaceId

    val habitStartMonth =
        Instant.ofEpochMilli(startDateTime).atZone(ZoneId.systemDefault()).toLocalDate()
            .let { YearMonth.of(it.year, it.month) }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val currentYearMonth = YearMonth.now()
    val endOfYear = YearMonth.of(currentYearMonth.year, 12)

    val canGoBack = currentMonth > habitStartMonth
    val canGoForward = currentMonth < endOfYear

    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7
    val dates = (1..daysInMonth).map { currentMonth.atDay(it) }

    val daysOfWeek = listOf(
        stringResource(R.string.sunday_short),
        stringResource(R.string.monday_short),
        stringResource(R.string.tuesday_short),
        stringResource(R.string.wednesday_short),
        stringResource(R.string.thursday_short),
        stringResource(R.string.friday_short),
        stringResource(R.string.saturday_short)

    )
    val today = LocalDate.now()
    val habitStartDate =
        Instant.ofEpochMilli(startDateTime).atZone(ZoneId.systemDefault()).toLocalDate()
    val habitEndEpochDay = if (endDateTime == -1L) null
    else Instant.ofEpochMilli(endDateTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toEpochDay()
    val locale = LocalLocale.current.platformLocale
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp),
        onClick = {},
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            // Month navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentMonth.month.getDisplayName(TextStyle.FULL, locale) +
                            " ${currentMonth.year}",
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    IconButton(
                        onClick = { if (canGoBack) currentMonth = currentMonth.minusMonths(1) },
                        enabled = canGoBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBackIos,
                            contentDescription = "Previous Month",
                            modifier = Modifier
                                .alpha(if (canGoBack) 0.8f else 0.3f)
                                .size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { if (canGoForward) currentMonth = currentMonth.plusMonths(1) },
                        enabled = canGoForward
                    ) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowForwardIos,
                            contentDescription = "Next Month",
                            modifier = Modifier
                                .alpha(if (canGoForward) 0.8f else 0.3f)
                                .size(16.dp)
                        )
                    }
                }
            }

            // Days of week row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                daysOfWeek.forEach {
                    Text(
                        text = it,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            // Calendar grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Empty cells before 1st day
                items(firstDayOfWeek) { Box(modifier = Modifier.size(32.dp)) }

                items(dates) { date ->
                    val epochDay = date.toEpochDay()
                    val instance = habitInstances.find { it.instanceDate == epochDay }
                    val isMarked = instance?.isCompleted(habit) ?: false
                    val isBeforeStart = epochDay < habitStartDate.toEpochDay()
                    val isAfterToday = epochDay > today.toEpochDay()
                    val isAfterEnd = habitEndEpochDay != null && epochDay > habitEndEpochDay
                    val isAllowedByRecurrence = isDateAllowedForHabit(recurrence, epochDay)

                    val backgroundColor = when {
                        isMarked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        isAllowedByRecurrence && !isBeforeStart && !isAfterEnd -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else -> Color.Transparent
                    }

                    val textColor = when {
                        isMarked -> MaterialTheme.colorScheme.onPrimary
                        isAllowedByRecurrence && !isBeforeStart && !isAfterEnd -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    }

                    val dateAlpha = when {
                        isBeforeStart -> 0.2f
                        isAfterToday || isAfterEnd -> 0.4f
                        !isAllowedByRecurrence -> 0.4f
                        else -> 1f
                    }

                    val habitAlreadyEnded = stringResource(R.string.habit_already_ended)
                    val cantMarkFutureDate = stringResource(R.string.cannot_mark_future_dates)
                    val habitStartsLater = stringResource(R.string.habit_starts_later)
                    val notInSchedule = stringResource(R.string.date_not_in_schedule)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(backgroundColor)
                            .alpha(dateAlpha)
                            .clickable {
                                when {
                                    isAfterEnd -> Toast.makeText(
                                        context,
                                        habitAlreadyEnded,
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    isAfterToday -> Toast.makeText(
                                        context,
                                        cantMarkFutureDate,
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    isBeforeStart -> Toast.makeText(
                                        context,
                                        habitStartsLater,
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    !isAllowedByRecurrence -> Toast.makeText(
                                        context,
                                        notInSchedule,
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    else -> {
                                        val oldInstance =
                                            habitInstances.find { it.instanceDate == epochDay }
                                        val count = if (habit.isCounted) {
                                            if (oldInstance != null) {
                                                val goal =
                                                    (habit.habitConfig as HabitConfig.Counted).goal
                                                if (oldInstance.count == goal) 0
                                                else goal
                                            } else (habit.habitConfig as HabitConfig.Counted).goal
                                        } else 0

                                        val newInstance = HabitInstanceModel(
                                            instanceDate = epochDay,
                                            habitId = habit.id,
                                            workspaceId = workspaceId,
                                            count = count
                                        )
                                        onHabitEvents(
                                            HabitEvents.UpdateInstance(
                                                newInstance,
                                                habit.habitConfig
                                            )
                                        )
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(date.dayOfMonth.toString(), color = textColor)
                    }
                }

            }
        }
    }
}

@Composable
fun WeeklyHabitAnalyticsCard(
    radius: Int,
    habit: HabitModel,
    instances: List<HabitInstanceModel>
) {
    val today = LocalDate.now()
    val startOfWeek = today.with(DayOfWeek.MONDAY)

    val instanceMap = remember(instances) {
        instances.associateBy { it.instanceDate }
    }

    val daysOfWeek = listOf(
        stringResource(R.string.monday_short),
        stringResource(R.string.tuesday_short),
        stringResource(R.string.wednesday_short),
        stringResource(R.string.thursday_short),
        stringResource(R.string.friday_short),
        stringResource(R.string.saturday_short),
        stringResource(R.string.sunday_short)
    )

    val dayStatus = daysOfWeek.mapIndexed { index, _ ->
        val date = startOfWeek.plusDays(index.toLong())
        val epoch = date.toEpochDay()

        val instance = instanceMap[epoch]

        when (val config = habit.habitConfig) {
            is HabitConfig.Simple -> {
                instance?.isCompleted(habit) == true
            }

            is HabitConfig.Counted -> {
                (instance?.count ?: 0) >= config.goal
            }

            is HabitConfig.Timed -> {
                (instance?.timeSpent ?: 0L) >= config.durationMillis
            }
        }
    }

    val completedCount = dayStatus.count { it }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        ),
        onClick = {}
    ) {
        Column(Modifier
            .fillMaxSize()
            .padding(12.dp)) {
            Text(
                text = stringResource(R.string.This_Week),
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = stringResource(R.string.completed_habits, completedCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEachIndexed { index, day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Card(
                            shape = RoundedCornerShape(50),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = if(dayStatus[index]) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        ) {
                            Icon(
                                imageVector = if (dayStatus[index]) Icons.Default.Verified else Icons.Default.Pending,
                                contentDescription = null,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyHabitAnalyticsCard(
    radius: Int,
    habit: HabitModel,
    instances: List<HabitInstanceModel>
) {
    val today = LocalDate.now()
    val currentYearMonth = YearMonth.of(today.year, today.month)
    val daysInMonth = currentYearMonth.lengthOfMonth()

    val instanceMap = remember(instances) {
        instances.associateBy { it.instanceDate }
    }

    val weekRanges = remember(daysInMonth) {
        val ranges = mutableListOf<IntRange>()
        var start = 1
        while (start <= daysInMonth) {
            val end = minOf(start + 6, daysInMonth)
            ranges.add(start..end)
            start = end + 1
        }
        ranges
    }

    val weekCounts = remember(instances, habit) {
        val counts = MutableList(weekRanges.size) { 0 }

        for (day in 1..daysInMonth) {
            val date = currentYearMonth.atDay(day)
            val epoch = date.toEpochDay()

            val instance = instanceMap[epoch]

            val isCompleted = when (val config = habit.habitConfig) {
                is HabitConfig.Simple ->
                    instance?.isCompleted(habit) == true

                is HabitConfig.Counted ->
                    (instance?.count ?: 0) >= config.goal

                is HabitConfig.Timed ->
                    (instance?.timeSpent ?: 0L) >= config.durationMillis
            }

            if (isCompleted) {
                weekRanges.forEachIndexed { index, range ->
                    if (day in range) {
                        counts[index]++
                        return@forEachIndexed
                    }
                }
            }
        }

        counts
    }

    val completedDays = weekCounts.sum()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        ),
        onClick = {}
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = stringResource(R.string.This_Month),
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = stringResource(R.string.completed_habits, completedDays),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            HabitBarChart(
                weekCounts = weekCounts,
                weekLabels = weekRanges.map { "${it.first}-${it.last}" }
            )
        }
    }
}

@Composable
fun SingleHabitWeeklyProgressChart(
    radius: Int,
    habit: HabitModel,
    instances: List<HabitInstanceModel>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val startOfWeek = today.with(DayOfWeek.MONDAY)
    val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }
    val instanceMap = remember(instances) { instances.associateBy { it.instanceDate } }

    val percentages = weekDays.map { day ->
        val dayEpoch = day.toEpochDay()
        val dayOfWeekIndex = (day.dayOfWeek.value + 6) % 7

        val weekly = habit.recurrence as? RecurrenceRule.Weekly

        val isScheduled =
            weekly?.daysOfWeek?.contains(dayOfWeekIndex) == true &&
                    habit.isLive()

        if (!isScheduled) return@map 0f

        val instance = instanceMap[dayEpoch]

        when (val config = habit.habitConfig) {
            is HabitConfig.Simple -> {
                if (instance?.isCompleted(habit) == true) 100f else 0f
            }

            is HabitConfig.Counted -> {
                val progress = (instance?.count ?: 0).toFloat() / config.goal
                (progress.coerceIn(0f, 1f)) * 100f
            }

            is HabitConfig.Timed -> {
                val progress = (instance?.timeSpent ?: 0L).toFloat() / config.durationMillis
                (progress.coerceIn(0f, 1f)) * 100f
            }
        }
    }

    WeeklyProgressChart(radius, modifier, percentages)
}

@Composable
fun HabitsWeeklyProgressAnalysis(
    radius: Int,
    habits: List<HabitModel>,
    habitInstances: List<HabitInstanceModel>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val startOfWeek = today.with(DayOfWeek.MONDAY)
    val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }
    val instanceMap = habitInstances.associateBy { it.habitId to it.instanceDate }

    val percentages = weekDays.map { day ->
        val dayEpoch = day.toEpochDay()
        val dayOfWeekIndex = (day.dayOfWeek.value + 6) % 7

        val scheduledHabits = habits.filter { habit ->
            val weekly = habit.recurrence as? RecurrenceRule.Weekly

            weekly?.daysOfWeek?.contains(dayOfWeekIndex) == true && habit.isLive()
        }

        val completed = scheduledHabits.count { habit ->
            val instance = instanceMap[habit.id to dayEpoch]
            instance?.isCompleted(habit) == true
        }

        if (scheduledHabits.isNotEmpty()) {
            (completed.toFloat() / scheduledHabits.size) * 100f
        } else 0f
    }

    WeeklyProgressChart(radius, modifier, percentages)
}

@Composable
fun WeeklyProgressChart(
    radius: Int,
    modifier: Modifier = Modifier,
    percentages: List<Float>
){
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryAlpha = primaryColor.copy(alpha = 0.4f)

    val today = LocalDate.now()
    val startOfWeek = today.with(DayOfWeek.MONDAY)
    val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }

    val dayLabels = listOf(
        stringResource(R.string.monday_short),
        stringResource(R.string.tuesday_short),
        stringResource(R.string.wednesday_short),
        stringResource(R.string.thursday_short),
        stringResource(R.string.friday_short),
        stringResource(R.string.saturday_short),
        stringResource(R.string.sunday_short)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        ),
        onClick = {}
    ) {
        Column(modifier = modifier.padding(12.dp)) {
            Text(
                stringResource(R.string.weekly_habit_completion),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 8.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
                    .height(200.dp)
            ) {
                val leftPadding = 32.dp.toPx()
                val bottomPadding = 32.dp.toPx()
                val widthPerPoint = (size.width - leftPadding) / (weekDays.size - 1)
                val height = size.height - bottomPadding

                // Vertical percentage labels
                for (i in 0..4) {
                    val y = height - i * height / 4
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            "${i * 25}%",
                            leftPadding - 14.dp.toPx(),
                            y,
                            android.graphics.Paint().apply {
                                color = primaryColor.toArgb()
                                textSize = 12.sp.toPx()
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }
                        )
                    }
                }

                // Line path
                val linePath = Path().apply {
                    percentages.forEachIndexed { index, percentage ->
                        val x = leftPadding + index * widthPerPoint
                        val y = height - (percentage / 100f * height)
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }

                // Shadow under line
                val shadowPath = Path().apply {
                    addPath(linePath)
                    lineTo(leftPadding + (weekDays.size - 1) * widthPerPoint, height)
                    lineTo(leftPadding, height)
                    close()
                }

                drawPath(
                    path = shadowPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryAlpha, primaryColor.copy(alpha = 0f)),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw line
                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw dots
                percentages.forEachIndexed { index, percentage ->
                    val x = leftPadding + index * widthPerPoint
                    val y = height - (percentage / 100f * height)
                    drawCircle(
                        color = primaryColor,
                        radius = 6.dp.toPx(),
                        center = Offset(x, y)
                    )
                }

                // Draw day labels
                dayLabels.forEachIndexed { index, day ->
                    val x = leftPadding + index * widthPerPoint
                    val y = height + 24.dp.toPx()
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            day,
                            x,
                            y,
                            android.graphics.Paint().apply {
                                color = primaryColor.toArgb()
                                textSize = 12.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HabitBarChart(
    weekCounts: List<Int>,
    weekLabels: List<String>,
    modifier: Modifier = Modifier
) {
    val maxDaysPerWeek = 7
    val yLabels = (maxDaysPerWeek downTo 1).toList()
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(bottom = 16.dp)
    ) {
        // Y-axis labels
        Box(
            modifier = Modifier
                .width(20.dp)
                .padding(end = 8.dp)
                .fillMaxHeight()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepHeight = size.height / maxDaysPerWeek
                val textPaint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    textSize = 32f
                    color = primaryColor.toArgb()
                    textAlign = android.graphics.Paint.Align.RIGHT
                }

                yLabels.forEach { label ->
                    val y = size.height - (stepHeight * label)
                    drawContext.canvas.nativeCanvas.drawText(
                        label.toString(),
                        size.width - 10f,
                        y + 5f,
                        textPaint
                    )
                }
            }
        }

        // Bar chart canvas
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            val barWidth = size.width / (weekCounts.size * 2 + 1)
            val stepHeight = size.height / maxDaysPerWeek

            weekCounts.forEachIndexed { index, count ->
                val barHeight = stepHeight * count
                val x = barWidth + index * (barWidth + barWidth)
                val y = size.height - barHeight

                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(x = 16.dp.toPx(), y = 16.dp.toPx())
                )

                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = primaryColor.toArgb()
                        textSize = 30f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    canvas.nativeCanvas.drawText(
                        weekLabels[index],
                        x + barWidth / 2,
                        size.height + 40f,
                        paint
                    )
                }
            }

            for (i in 1..maxDaysPerWeek) {
                val y = size.height - (stepHeight * i)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 0.25.dp.toPx()
                )
            }
        }
    }
}

data class StreakData(
    val currentStreak: Int,
    val bestStreak: Int
)

fun calculateStreaks(
    recurrence: RecurrenceRule,
    startDateTime: Long,
    habitConfig: HabitConfig,
    instances: List<HabitInstanceModel>
): StreakData {

    if (instances.isEmpty()) {
        return StreakData(
            currentStreak = 0,
            bestStreak = 0
        )
    }

    val expectedDates =
        getExpectedDates(
            recurrence = recurrence,
            startDateTime = startDateTime
        )

    val completedDates =
        getCompletedDates(
            habitConfig = habitConfig,
            instances = instances
        )

    val currentStreak =
        calculateCurrentStreak(
            expectedDates = expectedDates,
            completedDates = completedDates
        )

    val bestStreak =
        calculateBestStreak(
            expectedDates = expectedDates,
            completedDates = completedDates
        )

    return StreakData(
        currentStreak = currentStreak,
        bestStreak = bestStreak
    )
}

private fun getCompletedDates(
    habitConfig: HabitConfig,
    instances: List<HabitInstanceModel>
): Set<Long> {

    return when (habitConfig) {

        is HabitConfig.Simple -> {

            instances
                .map { it.instanceDate }
                .toSet()
        }

        is HabitConfig.Counted -> {

            instances
                .filter { instance ->

                    val count =
                        instance.count ?: 0

                    count >= habitConfig.goal
                }
                .map { it.instanceDate }
                .toSet()
        }

        is HabitConfig.Timed -> {

            instances
                .map { it.instanceDate }
                .toSet()
        }
    }
}

private fun getExpectedDates(
    recurrence: RecurrenceRule,
    startDateTime: Long
): List<Long> {

    val today = LocalDate.now()

    val startDate =
        Instant.ofEpochMilli(startDateTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

    val weekly =
        recurrence as? RecurrenceRule.Weekly
            ?: return emptyList()

    val expectedDates = mutableListOf<Long>()

    var current = startDate

    while (!current.isAfter(today)) {

        // Monday=0 ... Sunday=6
        val dayOfWeek =
            current.dayOfWeek.value - 1

        if (dayOfWeek in weekly.daysOfWeek) {
            expectedDates.add(current.toEpochDay())
        }

        current = current.plusDays(1)
    }

    return expectedDates
}

private fun calculateCurrentStreak(
    expectedDates: List<Long>,
    completedDates: Set<Long>
): Int {

    if (expectedDates.isEmpty()) return 0

    var streak = 0

    for (expectedDate in expectedDates.asReversed()) {

        if (expectedDate in completedDates) {
            streak++
        } else {
            break
        }
    }

    // If latest expected date is incomplete,
    // streak is dead.
    val latestExpected = expectedDates.last()

    if (latestExpected !in completedDates) {
        return 0
    }

    return streak
}

private fun calculateBestStreak(expectedDates: List<Long>, completedDates: Set<Long>): Int {

    if (expectedDates.isEmpty()) return 0

    var bestStreak = 0
    var currentStreak = 0

    for (expectedDate in expectedDates) {

        if (expectedDate in completedDates) {

            currentStreak++

            bestStreak =
                maxOf(bestStreak, currentStreak)

        } else {

            currentStreak = 0
        }
    }

    return bestStreak
}

@Composable
fun HabitConfigRow(
    modifier: Modifier,
    config: HabitConfig,
    onChangeConfig: (HabitConfig)->Unit
) {
    Row(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        HabitConfigCard(isExpanded = config is HabitConfig.Simple, config = HabitConfig.Simple) { onChangeConfig(HabitConfig.Simple) }
        HabitConfigCard(isExpanded = config is HabitConfig.Counted, config = HabitConfig.Counted()) { onChangeConfig(HabitConfig.Counted()) }
        // HabitConfigCard(isExpanded = config is HabitConfig.Timed, config = HabitConfig.Timed()) { onChangeConfig(HabitConfig.Timed()) }
    }
}

@Composable
fun HabitConfigCard(
    isExpanded: Boolean,
    config: HabitConfig,
    onClick: () -> Unit
){
    val title = when(config) {
        is HabitConfig.Simple -> "Simple"
        is HabitConfig.Counted -> "Counter"
        is HabitConfig.Timed -> "Timed"
    }

    val icon = when(config) {
        is HabitConfig.Simple -> Icons.Default.Check
        is HabitConfig.Counted -> Icons.Default.Numbers
        is HabitConfig.Timed -> Icons.Default.Timer
    }

    Card(
        modifier = Modifier.clip(RoundedCornerShape(50)).padding(horizontal = 2.dp),
        shape = RoundedCornerShape(50),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if(isExpanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if(isExpanded){
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) { Icon(icon, null, Modifier.size(18.dp)) }

                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier
                    .padding(end = 8.dp)
                    .widthIn(max = 150.dp), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            }
            else{
                IconButton(onClick = onClick, modifier = Modifier.size(32.dp),
                ) { Icon(icon, null, Modifier.size(18.dp)) }
            }
        }
    }
}

fun buildFilterCategories(recurrence: RecurrenceRule): List<FilterCategory> {
    return listOf(
        FilterCategory(
            name = "Sort By",
            options = listOf(
                FilterOption("latest", "Latest"),
                FilterOption("oldest", "Oldest")
            ),
            type = SelectionType.SINGLE
        ),
        FilterCategory(
            name = "Recurrence",
            options = listOf(
                FilterOption("recurrence", "Occurs On", null, recurrence)
            ),
            type = SelectionType.RECURRENCE
        ),
        FilterCategory(
            name = "Type",
            options = listOf(
                FilterOption("simple", "Simple"),
                FilterOption("timed", "Timed")
            ),
            type = SelectionType.SINGLE
        ),
        FilterCategory(
            name = "Active",
            options = listOf(
                FilterOption("live", "Live"),
                FilterOption("ended", "Ended")
            ),
            type = SelectionType.SINGLE
        )
    )
}