package com.flux.widgets.todo

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
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
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.flux.MainActivity
import com.flux.R
import com.flux.data.model.TodoDisplayItem
import com.flux.other.ReceiverEntryPoint
import com.flux.widgets.WidgetSize
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()
}

class TodoWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, ReceiverEntryPoint::class.java)
        val todoRepo = entryPoint.todoRepository()

        provideContent {
            val size = LocalSize.current
            val scope = rememberCoroutineScope()
            val prefs = currentState<Preferences>()
            val todoId = prefs[TODO_ID_KEY]

            val item = if (todoId != null) {
                val state by todoRepo.observeTodoList(todoId).collectAsState(initial = null)
                state
            } else {
                null
            }

            GlanceTheme {
                if (todoId == null) {
                    WidgetSurface {
                        NoSelectionState()
                    }
                } else if (item == null) {
                    WidgetSurface {}
                } else {
                    SingleTodoContent(
                        todo = item,
                        widgetSize = size,
                        onToggleItem = { itemId ->
                            scope.launch { todoRepo.toggleTodoItem(item, itemId) }
                        },
                        onUpdateWidget = { scope.launch { this@TodoWidget.update(context, id) } }
                    )
                }
            }
        }
    }

    companion object {
        val TODO_ID_KEY: Preferences.Key<String> = stringPreferencesKey("todo_id")
    }
}

@GlanceComposable
@Composable
private fun WidgetSurface(
    content: @Composable () -> Unit,
) {
    val roundedSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .roundedWidgetBackground(GlanceTheme.colors.widgetBackground, roundedSupported)
            .clickable(actionStartActivity<MainActivity>()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@GlanceComposable
@Composable
private fun NoSelectionState() {
    Text(text = "No todo selected", style = TextStyle(color = GlanceTheme.colors.onSurface))
}

@GlanceComposable
@Composable
private fun SingleTodoContent(
    todo: TodoDisplayItem,
    widgetSize: DpSize,
    onToggleItem: (String) -> Unit,
    modifier: GlanceModifier = GlanceModifier,
    onUpdateWidget: () -> Unit
) {
    val size = LocalSize.current
    val roundedSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val showIcon = widgetSize.width >= WidgetSize.Width4
    val isCompactHeight = widgetSize.height <= WidgetSize.Height1
    val itemSpacing = if (isCompactHeight) 2.dp else 6.dp

    val visibleItems = todo.items
    val checkedCount = todo.items.count { it.isChecked }

    Column(
        modifier = modifier
            .fillMaxSize()
            .roundedWidgetBackground(GlanceTheme.colors.widgetBackground, roundedSupported)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        TitleBar(
            startIcon = ImageProvider(R.drawable.task),
            title = todo.title,
            actions = {
                if (size.width >= WidgetSize.Width3) {
                    Row(GlanceModifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$checkedCount/${todo.items.size}",
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

        LazyColumn(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            items(visibleItems, itemId = { it.id.hashCode().toLong() }) { todoItem ->
                Column {
                    TodoItemRow(
                        text = todoItem.value,
                        isChecked = todoItem.isChecked,
                        showIcon = showIcon,
                        roundedSupported = roundedSupported,
                        onToggle = { onToggleItem(todoItem.id) },
                    )
                    Spacer(modifier = GlanceModifier.height(itemSpacing))
                }
            }
        }
    }
}

@GlanceComposable
@Composable
private fun TodoItemRow(
    text: String,
    isChecked: Boolean,
    showIcon: Boolean,
    roundedSupported: Boolean,
    onToggle: () -> Unit,
) {
    val containerColor = if (isChecked) GlanceTheme.colors.tertiaryContainer else GlanceTheme.colors.secondaryContainer
    val onContainerColor = if (isChecked) GlanceTheme.colors.onTertiaryContainer else GlanceTheme.colors.onSecondaryContainer

    Row(
        modifier = GlanceModifier.fillMaxWidth()
            .todoItemBackground(roundedSupported, containerColor)
            .padding(vertical = 6.dp, horizontal = 8.dp)
            .clickable { onToggle() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showIcon) {
            Image(
                provider = ImageProvider(if (isChecked) R.drawable.ic_check_circle else R.drawable.ic_circle_outlined),
                contentDescription = null,
                colorFilter = ColorFilter.tint(onContainerColor),
            )
        }
        Text(
            text = text,
            style = TextStyle(
                color = onContainerColor,
                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
            ),
            maxLines = 1,
            modifier = GlanceModifier.padding(start = if (showIcon) 8.dp else 0.dp),
        )
    }
}

private fun GlanceModifier.roundedWidgetBackground(color: ColorProvider, roundedSupported: Boolean): GlanceModifier =
    if (roundedSupported) background(color).cornerRadius(24.dp)
    else background(imageProvider = ImageProvider(R.drawable.rounded_4dp), colorFilter = ColorFilter.tint(color))

private fun GlanceModifier.todoItemBackground(roundedSupported: Boolean, color: ColorProvider): GlanceModifier =
    if (roundedSupported) cornerRadius(16.dp).background(color)
    else background(imageProvider = ImageProvider(R.drawable.rounded_list_top), colorFilter = ColorFilter.tint(color))