package com.flux.ui.events

import android.content.Context
import android.net.Uri
import android.webkit.WebView
import androidx.compose.foundation.text.input.TextFieldState
import com.flux.data.model.LabelModel
import com.flux.data.model.NotesModel
import com.flux.other.ExportType

sealed class NotesEvents {
    data class DeleteAllWorkspaceNotes(val workspaceId: String) : NotesEvents()
    data class EnterWorkspace(val workspaceId: String) : NotesEvents()
    data class DeleteNote(val data: NotesModel) : NotesEvents()
    data class DeleteNotes(val data: List<NotesModel>) : NotesEvents()
    data class TogglePinMultiple(val data: List<NotesModel>) : NotesEvents()
    data class UpsertNote(val data: NotesModel) : NotesEvents()
    data class DeleteLabel(val data: LabelModel) : NotesEvents()
    data class UpsertLabel(val data: LabelModel) : NotesEvents()
    data class SelectNotes(val noteId: String) : NotesEvents()
    data class UnSelectNotes(val noteId: String) : NotesEvents()
    data class ImportAudio(val context: Context, val sourceUri: Uri, val contentState: TextFieldState): NotesEvents()
    data class ImportImages(val context: Context, val uriList: List<Uri>, val contentState: TextFieldState): NotesEvents()
    data class ImportVideo(val context: Context, val uri: Uri, val contentState: TextFieldState): NotesEvents()
    data class ExportNote(val context: Context, val type: ExportType, val noteTitle: String, val noteDescription: String, val webView: WebView?): NotesEvents()
    data class CalculateOutline(val content: CharSequence): NotesEvents()
    data class CalculateTextState(val content: CharSequence): NotesEvents()
    data object ClearSelection : NotesEvents()
    data object SelectAllNotes : NotesEvents()
}