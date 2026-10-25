package com.flux.widgets.habits

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.LazyListScope
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.flux.MainActivity
import com.flux.R
import com.flux.data.model.HabitConfig
import com.flux.data.model.HabitDataItem
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import com.flux.data.model.isActiveOn
import com.flux.data.model.isCompleted
import com.flux.other.ReceiverEntryPoint
import com.flux.widgets.WidgetSize
import com.flux.widgets.todo.ListDeletedState
import com.flux.widgets.todo.TodoWidgetConfigActivity
import com.flux.widgets.todo.WidgetSurface
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val WIDGET_CORNER_RADIUS_DP = 24
private const val MIN_CELL = 8f
private const val SHORT_LABEL_MIN_COL_DP = 28f
private const val CONTENT_PADDING = 18f // 16 dp outer padding + 2 dp inner padding
private const val GAP = 3f
private const val STREAK_ITEM_GAP = 3f
private const val MAX_CELL = 18f
private const val SECTION_GAP = 12f
private const val GRID_COLS = 6
private enum class DayState { Completed, Missed, NotScheduled, Empty }
private const val HEATMAP_DAYS = 30

/** Stats derived from the last 30 days only. "best" is best within this window. */
private data class StreakStats(
    val current: Int,
    val best: Int,
    val completed: Int,
)

// Hardcoded English text (no strings.xml dependency). Not localized.
private fun daysText(n: Int): String = if (n == 1) "1 day" else "$n days"
private fun completedTimesText(n: Int): String =
    if (n == 1) "Completed 1 time" else "Completed $n times"

class SingleHabitAnalyticsReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleHabitAnalytics()
}

private sealed interface WidgetState {
    data object Loading : WidgetState
    data object Missing : WidgetState
    data class Ready(val item: HabitDataItem) : WidgetState
}

class SingleHabitAnalytics : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, ReceiverEntryPoint::class.java)
        val habitRepo = entryPoint.habitRepository()
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        provideContent {
            val prefs = currentState<Preferences>()
            val habitId = prefs[HabitIdKey]

            val stateFlow: Flow<WidgetState> = remember(habitId) {
                if (habitId == null) emptyFlow()
                else habitRepo.observeHabitDataItem(habitId).map { t ->
                    if (t == null) WidgetState.Missing else WidgetState.Ready(t)
                }
            }
            val state by stateFlow.collectAsState(initial = WidgetState.Loading)

            GlanceTheme {
                if (habitId == null) {
                    WidgetSurface { NoSelectionState() }
                } else when (val s = state) {
                    WidgetState.Loading -> WidgetSurface {}
                    WidgetState.Missing -> WidgetSurface {
                        ListDeletedState(
                            onPickAnother = androidx.glance.appwidget.action.actionStartActivity(
                                Intent(context, TodoWidgetConfigActivity::class.java)
                                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            )
                        )
                    }

                    is WidgetState.Ready -> {
                        val habit = s.item.habit
                        val instances = s.item.instances
                        WidgetSurface {
                            WidgetContent(
                                habit = habit,
                                weekStatus = computeWeekStatus(habit, instances),
                                heatMap = computeHeatMap(habit, instances),
                                modifier = GlanceModifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        val HabitIdKey: Preferences.Key<String> = stringPreferencesKey("habit_id")
    }
}

@GlanceComposable
@Composable
private fun NoSelectionState() {
    Text(text = "No habit selected", style = TextStyle(color = GlanceTheme.colors.onSurface))
}

/** Refresh button target. Re-runs provideGlance for this widget instance. */
class RefreshHabitWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        SingleHabitAnalytics().update(context, glanceId)
    }
}

@GlanceComposable
@Composable
private fun WidgetContent(
    habit: HabitModel,
    weekStatus: List<Boolean>,
    heatMap: List<DayState>,
    modifier: GlanceModifier = GlanceModifier,
) {
    val size = LocalSize.current
    val contentW = (size.width.value - 2 * CONTENT_PADDING).coerceAtLeast(0f)
    val streakIconSize = ((contentW - 6 * STREAK_ITEM_GAP) / 7f).coerceIn(10f, 26f)

    // Square-ish cells from width only. Each column gets contentW / 6, minus the gap.
    val cellHeight = (contentW / GRID_COLS - GAP).coerceIn(MIN_CELL, MAX_CELL)

    val stats = computeStreakStats(heatMap)

    // LazyColumn items swallow taps, so every item root needs its own click action.
    val openApp = GlanceModifier.clickable(actionStartActivity<MainActivity>())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(WIDGET_CORNER_RADIUS_DP.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        LazyColumn(modifier = GlanceModifier.fillMaxSize().padding(2.dp)) {
            item {
                Row(
                    modifier = openApp.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.task),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
                        modifier = GlanceModifier.size(20.dp)
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        text = habit.title,
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    if (size.width >= WidgetSize.Width3) {
                        Image(
                            provider = ImageProvider(R.drawable.refresh),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
                            modifier = GlanceModifier
                                .size(20.dp)
                                .clickable(actionRunCallback<RefreshHabitWidgetAction>())
                        )
                    }
                }
            }

            item { Spacer(openApp.fillMaxWidth().height(SECTION_GAP.dp)) }

            item {
                WeeklyStreakGraphics(
                    dayStatus = weekStatus,
                    iconSize = streakIconSize.dp,
                    columnWidthDp = contentW / 7f,
                    modifier = openApp
                )
            }

            item { Spacer(openApp.fillMaxWidth().height(SECTION_GAP.dp)) }

            heatMapItems(
                days = heatMap,
                cellHeightDp = cellHeight,
                openApp = openApp,
                stats = stats,
            )

            // Room at the bottom, and a natural place to add the next section.
            item { Spacer(openApp.fillMaxWidth().height(SECTION_GAP.dp)) }
        }
    }
}

/**
 * Order: "Last 30 days" label -> streak cards -> "Completed X times" -> heatmap rows.
 * Each heatmap row is its own lazy item, so the list scrolls row by row.
 */
private fun LazyListScope.heatMapItems(
    days: List<DayState>,
    cellHeightDp: Float,
    openApp: GlanceModifier,
    stats: StreakStats,
) {
    val rows = days.chunked(GRID_COLS).map { row ->
        row + List((GRID_COLS - row.size).coerceAtLeast(0)) { DayState.Empty }
    }

    item {
        Text(
            text = LocalContext.current.getString(R.string.last_30_days),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = openApp.fillMaxWidth().padding(bottom = 6.dp)
        )
    }

    // Current streak + best streak
    item {
        StreakStatsRow(
            current = stats.current,
            best = stats.best,
            modifier = openApp.fillMaxWidth()
        )
    }

    // Completed count, directly above the heatmap
    item {
        Text(
            text = completedTimesText(stats.completed),
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 11.sp
            ),
            modifier = openApp.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
        )
    }

    rows.forEach { row ->
        item {
            val completed = GlanceTheme.colors.primary
            val missed = GlanceTheme.colors.secondaryContainer
            val idle = GlanceTheme.colors.surfaceVariant

            Row(modifier = openApp.fillMaxWidth()) {
                row.forEach { state ->
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .padding(horizontal = (GAP / 2f).dp, vertical = (GAP / 2f).dp)
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(cellHeightDp.dp)
                                .background(
                                    when (state) {
                                        DayState.Completed -> completed
                                        DayState.Missed -> missed
                                        DayState.NotScheduled, DayState.Empty -> idle
                                    }
                                )
                                .cornerRadius(2.dp)
                        ) {}
                    }
                }
            }
        }
    }
}

@GlanceComposable
@Composable
private fun StreakStatsRow(
    current: Int,
    best: Int,
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(modifier = modifier) {
        StreakStatCard(
            label = "Current streak",
            value = daysText(current),
            modifier = GlanceModifier.defaultWeight()
        )
        Spacer(GlanceModifier.width(8.dp))
        StreakStatCard(
            label = "Best streak",
            value = daysText(best),
            modifier = GlanceModifier.defaultWeight()
        )
    }
}

@GlanceComposable
@Composable
private fun StreakStatCard(
    label: String,
    value: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier = modifier
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(12.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(R.drawable.fire),
                contentDescription = null,
                modifier = GlanceModifier.size(16.dp)
            )
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = value,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        Text(
            text = label,
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp
            )
        )
    }
}

@GlanceComposable
@Composable
fun WeeklyStreakGraphics(
    dayStatus: List<Boolean>,
    iconSize: androidx.compose.ui.unit.Dp,
    columnWidthDp: Float,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val useInitial = columnWidthDp < SHORT_LABEL_MIN_COL_DP
    val days = listOf(
        R.string.monday_short, R.string.tuesday_short, R.string.wednesday_short,
        R.string.thursday_short, R.string.friday_short, R.string.saturday_short,
        R.string.sunday_short
    ).map { id ->
        val s = context.getString(id)
        if (useInitial) s.take(1) else s
    }

    val completedCount = dayStatus.count { it }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = context.getString(R.string.This_Week),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Text(
            text = context.getString(R.string.completed_habits, completedCount),
            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 11.sp),
        )

        Spacer(GlanceModifier.height(12.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            days.forEachIndexed { index, day ->
                val done = dayStatus.getOrElse(index) { false }
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (done) {
                        Image(
                            provider = ImageProvider(R.drawable.fire),
                            contentDescription = null,
                            modifier = GlanceModifier.size(iconSize)
                        )
                    } else {
                        Image(
                            provider = ImageProvider(R.drawable.outlinedcircle),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                            modifier = GlanceModifier.size(iconSize)
                        )
                    }
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text = day,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun isDone(habit: HabitModel, instance: HabitInstanceModel?): Boolean =
    when (val config = habit.habitConfig) {
        is HabitConfig.Simple -> instance?.isCompleted(habit) == true
        is HabitConfig.Counted -> (instance?.count ?: 0) >= config.goal
        is HabitConfig.Timed -> (instance?.timeSpent ?: 0L) >= config.durationMillis
    }

private fun computeWeekStatus(
    habit: HabitModel,
    instances: List<HabitInstanceModel>
): List<Boolean> {
    val startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY)
    val instanceMap = instances.associateBy { it.instanceDate }
    return List(7) { i -> isDone(habit, instanceMap[startOfWeek.plusDays(i.toLong()).toEpochDay()]) }
}

/** Oldest -> newest, last 30 days including today. */
private fun computeHeatMap(
    habit: HabitModel,
    instances: List<HabitInstanceModel>
): List<DayState> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val start = Instant.ofEpochMilli(habit.startDateTime).atZone(zone).toLocalDate()
    val end = if (habit.endDateTime == -1L) null
    else Instant.ofEpochMilli(habit.endDateTime).atZone(zone).toLocalDate()
    val map = instances.associateBy { it.instanceDate }

    return (HEATMAP_DAYS - 1 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        when {
            isDone(habit, map[date.toEpochDay()]) -> DayState.Completed
            date >= start && (end == null || date <= end) &&
                    habit.recurrence.isActiveOn(date, start) -> DayState.Missed
            else -> DayState.NotScheduled
        }
    }
}

/**
 * Derives stats from the 30-day heatmap (oldest -> newest).
 * - Completed: +1 to the streak.
 * - NotScheduled: skipped, neither extends nor breaks the streak.
 * - Missed: breaks the streak.
 * - Today still pending (Missed as the last entry) doesn't reset the current streak.
 */
private fun computeStreakStats(heatMap: List<DayState>): StreakStats {
    val completed = heatMap.count { it == DayState.Completed }

    // Best streak within the window
    var best = 0
    var run = 0
    heatMap.forEach { state ->
        when (state) {
            DayState.Completed -> {
                run++
                if (run > best) best = run
            }
            DayState.Missed -> run = 0
            DayState.NotScheduled, DayState.Empty -> Unit
        }
    }

    // Current streak: walk backwards from today
    var current = 0
    var i = heatMap.lastIndex
    if (i >= 0 && heatMap[i] == DayState.Missed) i-- // today not done yet, grace
    while (i >= 0) {
        when (heatMap[i]) {
            DayState.Completed -> current++
            DayState.Missed -> break
            DayState.NotScheduled, DayState.Empty -> Unit
        }
        i--
    }

    return StreakStats(current = current, best = best, completed = completed)
}