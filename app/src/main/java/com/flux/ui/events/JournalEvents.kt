package com.flux.ui.events

import android.content.Context
import android.net.Uri
import android.webkit.WebView
import androidx.compose.foundation.text.input.TextFieldState
import com.flux.data.model.JournalModel
import com.flux.other.ExportType
import java.time.YearMonth

sealed class JournalEvents {
    data class UpsertEntry(val entry: JournalModel) : JournalEvents()
    data class DeleteEntry(val entry: JournalModel) : JournalEvents()
    data class DeleteWorkspaceEntries(val workspaceId: String) : JournalEvents()
    data class EnterWorkspace(val workspaceId: String) : JournalEvents()
    data class ImportAudio(val context: Context, val sourceUri: Uri, val contentState: TextFieldState): JournalEvents()
    data class ImportImages(val context: Context, val uriList: List<Uri>, val contentState: TextFieldState): JournalEvents()
    data class ImportVideo(val context: Context, val uri: Uri, val contentState: TextFieldState): JournalEvents()
    data class ExportJournal(val context: Context, val type: ExportType, val title: String, val content: String, val webView: WebView?): JournalEvents()
    data class CalculateOutline(val content: CharSequence): JournalEvents()
    data class CalculateTextState(val content: CharSequence): JournalEvents()
    data class ChangeMonth(val newYearMonth: YearMonth) : JournalEvents()
    data class ChangeDate(val newLocalDate: Long) : JournalEvents()
}