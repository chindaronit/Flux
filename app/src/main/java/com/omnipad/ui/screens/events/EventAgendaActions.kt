package com.omnipad.ui.screens.events

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.omnipad.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class RevealState { Calendar, Closed, Open }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AgendaEventActions(
    isPending: Boolean,
    onCalendar: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable (trailingContent: @Composable () -> Unit) -> Unit
) {
    val density = LocalDensity.current
    val revealWidth = with(density) { 96.dp.toPx() }
    val state = remember {
        AnchoredDraggableState(initialValue = RevealState.Closed)
    }
    state.updateAnchors(DraggableAnchors {
        RevealState.Calendar at -revealWidth
        RevealState.Closed at 0f
        RevealState.Open at revealWidth
    })
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    val calendarLabel = stringResource(R.string.event_show_in_calendar)
    val completeLabel = stringResource(if (isPending) R.string.event_mark_complete else R.string.event_mark_incomplete)
    val deleteLabel = stringResource(R.string.delete)
    fun perform(action: () -> Unit) {
        menuExpanded = false
        scope.launch { state.animateTo(RevealState.Closed) }
        action()
    }

    Box(Modifier.clipToBounds()) {
        Row(
            modifier = Modifier.align(AbsoluteAlignment.CenterLeft)
                .then(if (state.settledValue != RevealState.Open) Modifier.clearAndSetSemantics {} else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { perform(onComplete) }, modifier = Modifier.width(48.dp)) {
                Icon(Icons.Default.Check, completeLabel)
            }
            IconButton(onClick = { perform(onDelete) }, modifier = Modifier.width(48.dp)) {
                Icon(Icons.Default.Delete, deleteLabel, tint = MaterialTheme.colorScheme.error)
            }
        }
        Box(
            modifier = Modifier
                .align(AbsoluteAlignment.CenterRight)
                .width(96.dp)
                .then(if (state.settledValue != RevealState.Calendar) Modifier.clearAndSetSemantics {} else Modifier),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = { perform(onCalendar) }) {
                Icon(Icons.Default.Event, contentDescription = calendarLabel)
            }
        }
        Box(
            Modifier
                .absoluteOffset { IntOffset(state.requireOffset().roundToInt(), 0) }
                .anchoredDraggable(state, reverseDirection = false, orientation = Orientation.Horizontal)
        ) {
            content {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, stringResource(R.string.event_more_actions))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(calendarLabel) },
                            leadingIcon = { Icon(Icons.Default.Event, null) },
                            onClick = { perform(onCalendar) }
                        )
                        DropdownMenuItem(
                            text = { Text(completeLabel) },
                            leadingIcon = { Icon(Icons.Default.Check, null) },
                            onClick = { perform(onComplete) }
                        )
                        DropdownMenuItem(
                            text = { Text(deleteLabel, color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { perform(onDelete) }
                        )
                    }
                }
            }
        }
    }
}
