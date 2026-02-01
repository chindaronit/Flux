package com.flux.ui.screens.journal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.JournalModel
import com.flux.navigation.Loader
import com.flux.navigation.NavRoutes
import com.flux.ui.components.DailyViewCalendar
import com.flux.ui.components.MonthlyViewCalendar
import com.flux.ui.state.Settings
import java.time.YearMonth
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.text.style.TextOverflow
import com.flux.other.parseMarkdownContent
import com.flux.ui.components.TimelineBody
import com.flux.ui.components.convertMillisToTime
import com.flux.ui.components.shapeManager
import com.flux.ui.events.JournalEvents

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.journalHomeItems(
    navController: NavController,
    settings: Settings,
    selectedMonth: YearMonth,
    selectedDate: Long,
    isLoading: Boolean,
    workspaceId: String,
    allEntries: List<JournalModel>,
    onJournalEvents: (JournalEvents) -> Unit
) {
    val isMonthlyView = settings.data.isCalendarMonthlyView
    val radius  = settings.data.cornerRadius

    if (isMonthlyView) {
        item {
            MonthlyViewCalendar(
                selectedMonth, selectedDate,
                onMonthChange = {
                    onJournalEvents(JournalEvents.ChangeMonth(it))
                },
                onDateChange = {
                    onJournalEvents(JournalEvents.ChangeDate(it))
                })
        }
    } else {
        item {
            DailyViewCalendar(
                selectedMonth,
                selectedDate,
                onDateChange = {
                    onJournalEvents(JournalEvents.ChangeDate(it))
                })
        }
    }

    when {
        isLoading -> item { Loader() }
        allEntries.isEmpty() -> item { EmptyJournal() }
        else -> {
            itemsIndexed(allEntries) { index, entry ->
                Column(Modifier.padding(top = 16.dp)) {
                    JournalCardHeader(convertMillisToTime(entry.dateTime))
                    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                        TimelineBody(isLast = false)
                        JournalPreview(radius, entry.text) {
                            navController.navigate(
                                NavRoutes.EditJournal.withArgs(
                                    workspaceId,
                                    entry.journalId,
                                    0L
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalCardHeader(
    formattedTimestamp: String,
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    typography: Typography = MaterialTheme.typography
) {
    Box(modifier = Modifier
        .wrapContentWidth()
        .padding(bottom = 8.dp, top = 4.dp, start = 4.dp)) {
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(8.dp)
        ) {
            drawCircle(
                color = colorScheme.onSurface,
                radius = size.minDimension / 2
            )
        }

        BasicText(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            text = formattedTimestamp,
            style = typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = ColorProducer { colorScheme.onSurfaceVariant }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalPreview(
    radius: Int,
    content: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)),
        modifier = Modifier.clip(shapeManager(isBoth = true, radius = radius / 2)).fillMaxWidth(),
        shape = shapeManager(isBoth = true, radius = radius / 2),
        onClick = onClick
    ) {
        Text(
            text = parseMarkdownContent(content),
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .alpha(0.9f)
                .padding(12.dp)
                .heightIn(max = 300.dp, min = 50.dp)
        )
    }
}

@Composable
fun EmptyJournal() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AutoStories,
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        Text(stringResource(R.string.Empty_Journal))
    }
}