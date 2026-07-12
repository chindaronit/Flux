package com.flux.widgets.habits

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.flux.MainActivity
import com.flux.R
import com.flux.data.model.HabitWithStatus
import com.flux.other.ReceiverEntryPoint
import com.flux.widgets.WidgetSize
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

class HabitWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, ReceiverEntryPoint::class.java)
        val habitRepo = entryPoint.habitRepository()

        provideContent {
            val size = LocalSize.current
            val scope = rememberCoroutineScope()

            val habitsWithStatus by habitRepo.observeTodayHabitStatuses()
                .collectAsState(initial = emptyList())

            key(size) {
                GlanceTheme {
                    HabitWidgetContent(
                        habitsWithStatus = habitsWithStatus,
                        widgetSize = size,
                        onToggleHabit = { item -> scope.launch { habitRepo.toggleHabit(item.habit, item.isCompleted) } },
                        onUpdateWidget = {
                            scope.launch { this@HabitWidget.update(context, id) }
                        },
                    )
                }
            }
        }
    }
}

@GlanceComposable
@Composable
private fun HabitWidgetContent(
    habitsWithStatus: List<HabitWithStatus>,
    widgetSize: DpSize,
    onToggleHabit: (HabitWithStatus) -> Unit,
    onUpdateWidget: () -> Unit,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .roundedWidgetBackground(GlanceTheme.colors.widgetBackground)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        HabitWidgetHeader(
            completedCount = habitsWithStatus.count { it.isCompleted },
            totalCount = habitsWithStatus.size,
            onUpdateWidget = onUpdateWidget
        )
        HabitList(
            habitsWithStatus = habitsWithStatus,
            showIcon = widgetSize.width >= WidgetSize.Width4,
            onToggleHabit = onToggleHabit,
        )
    }
}

@GlanceComposable
@Composable
private fun HabitWidgetHeader(
    completedCount: Int,
    totalCount: Int,
    onUpdateWidget: () -> Unit
) {
    val size = LocalSize.current
    TitleBar(
        startIcon = ImageProvider(R.drawable.alarm),
        title = "Habits",
        actions = {
            if (size.width >= WidgetSize.Width3) {
                Row(GlanceModifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$completedCount/$totalCount",
                        style = TextStyle(color = GlanceTheme.colors.onSurface),
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Image(
                        provider = ImageProvider(R.drawable.refresh),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
                        modifier = GlanceModifier.clickable { onUpdateWidget() },
                    )
                }
            }
        },
    )
}

@GlanceComposable
@Composable
private fun HabitList(
    habitsWithStatus: List<HabitWithStatus>,
    showIcon: Boolean,
    onToggleHabit: (HabitWithStatus) -> Unit,
) {
    LazyColumn(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        items(habitsWithStatus, itemId = { it.habit.id.hashCode().toLong() }) { item ->
            Column {
                HabitListItem(
                    item = item,
                    showIcon = showIcon,
                    onToggle = { onToggleHabit(item) },
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
            }
        }

        item { Spacer(modifier = GlanceModifier.height(4.dp)) }
    }
}

@GlanceComposable
@Composable
private fun HabitListItem(
    item: HabitWithStatus,
    showIcon: Boolean,
    onToggle: () -> Unit,
) {
    val roundedSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val containerColor = if (item.isCompleted) {
        GlanceTheme.colors.tertiaryContainer
    } else {
        GlanceTheme.colors.secondaryContainer
    }
    val onContainerColor = if (item.isCompleted) {
        GlanceTheme.colors.onTertiaryContainer
    } else {
        GlanceTheme.colors.onSecondaryContainer
    }

    Row(
        modifier = GlanceModifier.fillMaxWidth()
            .habitItemBackground(roundedSupported, containerColor)
            .padding(vertical = 8.dp)
            .clickable { onToggle() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showIcon) {
            Image(
                provider = ImageProvider(
                    if (item.isCompleted) R.drawable.ic_check_circle else R.drawable.ic_circle_outlined
                ),
                contentDescription = null,
                modifier = GlanceModifier.padding(start = 12.dp),
                colorFilter = ColorFilter.tint(onContainerColor),
            )
        }

        Column(modifier = GlanceModifier.defaultWeight().padding(8.dp)) {
            Text(
                text = item.habit.title,
                style = TextStyle(
                    color = onContainerColor,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )

            if (item.habit.description.isNotEmpty()) {
                Text(
                    text = item.habit.description,
                    style = TextStyle(
                        color = onContainerColor,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

private fun GlanceModifier.roundedWidgetBackground(color: ColorProvider): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        background(color).cornerRadius(24.dp)
    } else {
        background(
            imageProvider = ImageProvider(R.drawable.rounded_4dp),
            colorFilter = ColorFilter.tint(color),
        )
    }

private fun GlanceModifier.habitItemBackground(
    roundedSupported: Boolean,
    color: ColorProvider,
): GlanceModifier =
    if (roundedSupported) {
        cornerRadius(16.dp).background(color)
    } else {
        background(
            imageProvider = ImageProvider(R.drawable.rounded_list_top),
            colorFilter = ColorFilter.tint(color),
        )
    }