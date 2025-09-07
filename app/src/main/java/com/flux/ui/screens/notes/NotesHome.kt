package com.flux.ui.screens.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.LabelModel
import com.flux.data.model.NotesModel
import com.flux.navigation.Loader
import com.flux.navigation.NavRoutes
import com.flux.ui.components.EmptyNotes
import com.flux.ui.components.NotesPreviewCard
import com.flux.ui.events.NotesEvents

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.notesHomeItems(
    navController: NavController,
    workspaceId: String,
    selectedNotes: List<String>,
    query: String,
    radius: Int,
    isGridView: Boolean,
    allLabels: List<LabelModel>,
    isLoading: Boolean,
    allNotes: List<NotesModel>,
    onNotesEvents: (NotesEvents) -> Unit
) {
    val pinnedNotes = allNotes.filter { it.isPinned }
    val unPinnedNotes = allNotes.filter { !it.isPinned }

    when {
        isLoading -> item { Loader() }
        else ->
            if (pinnedNotes.isEmpty() && unPinnedNotes.isEmpty()) {
                item { EmptyNotes() }
            } else {
                if (pinnedNotes.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.Pinned),
                            modifier = Modifier.padding(vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                item {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val spacing = 8.dp
                        val cellWidth = if(isGridView) (maxWidth - spacing) / 2 else maxWidth

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            pinnedNotes.filter {
                                it.title.contains(query, ignoreCase = true) ||
                                        it.description.contains(query, ignoreCase = true)
                            }.forEach { note ->
                                NotesPreviewCard(
                                    radius = radius,
                                    isSelected = selectedNotes.contains(note.notesId),
                                    note = note,
                                    labels = allLabels.filter { note.labels.contains(it.labelId) }.map { it.value },
                                    onClick = {
                                        navController.navigate(
                                            NavRoutes.NoteDetails.withArgs(workspaceId, note.notesId)
                                        )
                                    },
                                    onLongPressed = {
                                        if (selectedNotes.contains(note.notesId)) {
                                            onNotesEvents(NotesEvents.UnSelectNotes(note.notesId))
                                        } else {
                                            onNotesEvents(NotesEvents.SelectNotes(note.notesId))
                                        }
                                    },
                                    modifier = Modifier.width(cellWidth)
                                )
                            }
                        }
                    }
                }

                if (pinnedNotes.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.Others),
                            modifier = Modifier.padding(vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                item {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val spacing = 8.dp
                        val cellWidth = if(isGridView) (maxWidth - spacing) / 2 else maxWidth

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            unPinnedNotes.filter {
                                it.title.contains(query, ignoreCase = true) ||
                                        it.description.contains(query, ignoreCase = true)
                            }.forEach { note ->
                                NotesPreviewCard(
                                    radius = radius,
                                    isSelected = selectedNotes.contains(note.notesId),
                                    note = note,
                                    labels = allLabels.filter { note.labels.contains(it.labelId) }.map { it.value },
                                    onClick = {
                                        navController.navigate(
                                            NavRoutes.NoteDetails.withArgs(workspaceId, note.notesId)
                                        )
                                    },
                                    onLongPressed = {
                                        if (selectedNotes.contains(note.notesId)) {
                                            onNotesEvents(NotesEvents.UnSelectNotes(note.notesId))
                                        } else {
                                            onNotesEvents(NotesEvents.SelectNotes(note.notesId))
                                        }
                                    },
                                    modifier = Modifier.width(cellWidth)
                                )
                            }
                        }
                    }
                }
            }
    }
}
