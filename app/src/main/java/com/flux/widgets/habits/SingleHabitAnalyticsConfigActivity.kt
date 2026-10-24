package com.flux.widgets.habits

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
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.flux.data.model.HabitModel
import com.flux.data.repository.HabitRepository
import com.flux.ui.common.EmptyData
import com.flux.ui.theme.FluxTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SingleHabitAnalyticsConfigActivity : ComponentActivity() {

    @Inject
    lateinit var habitRepo: HabitRepository

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
                HabitPickerScreen(
                    habitRepository = habitRepo,
                    onHabitSelected = ::completeConfiguration
                )
            }
        }
    }

    private fun completeConfiguration(habitId: String) {
        lifecycleScope.launch {

            val glanceId = GlanceAppWidgetManager(this@SingleHabitAnalyticsConfigActivity)
                .getGlanceIdBy(appWidgetId)

            updateAppWidgetState(
                context = this@SingleHabitAnalyticsConfigActivity,
                definition = PreferencesGlanceStateDefinition,
                glanceId = glanceId,
            ) { prefs ->

                prefs.toMutablePreferences().apply {
                    this[SingleHabitAnalytics.HabitIdKey] = habitId
                }
            }

            SingleHabitAnalytics().update(this@SingleHabitAnalyticsConfigActivity, glanceId)

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
private fun HabitPickerScreen(
    habitRepository: HabitRepository,
    onHabitSelected: (String) -> Unit,
) {
    val habits by habitRepository.observePublicHabits().collectAsState(initial = null)

    Scaffold(topBar = { TopAppBar(title = { Text("Pick a Habit") }) }) { innerPadding->
        Surface(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val list = habits) {
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
                            ) { habit ->

                                HabitRow(
                                    habit = habit,
                                    onClick = { onHabitSelected(habit.id) }
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
private fun HabitRow(
    habit: HabitModel,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(habit.title)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}