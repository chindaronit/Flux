package com.flux.widgets.todo

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.flux.R
import com.flux.data.model.TodoModel
import com.flux.data.repository.TodoRepository
import com.flux.ui.common.EmptyData
import com.flux.ui.theme.FluxTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TodoWidgetConfigActivity : ComponentActivity() {

    @Inject
    lateinit var todoRepo: TodoRepository

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            FluxTheme {
                TodoPickerScreen(
                    todoRepository = todoRepo,
                    onTodoSelected = ::completeConfiguration
                )
            }
        }
    }

    private fun completeConfiguration(todoId: String) {
        lifecycleScope.launch {

            val glanceId = GlanceAppWidgetManager(this@TodoWidgetConfigActivity)
                .getGlanceIdBy(appWidgetId)

            updateAppWidgetState(
                context = this@TodoWidgetConfigActivity,
                definition = PreferencesGlanceStateDefinition,
                glanceId = glanceId,
            ) { prefs ->

                prefs.toMutablePreferences().apply {
                    this[TodoWidget.TODO_ID_KEY] = todoId
                }
            }

            TodoWidget().update(this@TodoWidgetConfigActivity, glanceId)

            setResult(
                RESULT_OK,
                Intent().putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    appWidgetId
                )
            )

            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoPickerScreen(
    todoRepository: TodoRepository,
    onTodoSelected: (String) -> Unit,
) {
    val todos by todoRepository.observePublicTodos().collectAsState(initial = null)

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.pick_a_todo)) }) }) { innerPadding->
        Surface(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val list = todos) {
                null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }

                else -> {
                    if (list.isEmpty()) { EmptyData() } else {
                        LazyColumn {
                            items(
                                items = list,
                                key = { it.id }
                            ) { todo ->

                                TodoRow(
                                    todo = todo,
                                    onClick = { onTodoSelected(todo.id) }
                                )

                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoRow(
    todo: TodoModel,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(todo.title)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}