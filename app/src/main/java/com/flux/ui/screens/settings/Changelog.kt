package com.flux.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.ui.common.BasicScaffold

@Composable
fun Changelog(
    navController: NavController,
){
    BasicScaffold(
        title = "Changelog",
        onBackClicked = { navController.popBackStack() }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp, 8.dp, 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(CHANGELOG_DATA.sortedByDescending { it.versionCode }){ item->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)){
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tag, null)
                            Text(item.version, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(item.date, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Text(item.changes, modifier = Modifier.alpha(0.8f).padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.ExtraLight, style = MaterialTheme.typography.labelLarge)
                    HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp, top = 12.dp))
                }
            }
        }
    }
}

data class ChangelogEntry(
    val version: String,
    val versionCode: Int,
    val date: String,
    val changes: String
)

val CHANGELOG_DATA = listOf(
    ChangelogEntry(
        version = "v1.0",
        versionCode = 1,
        date = "Aug 10, 2025",
        changes = "Initial Release of the application."
    ),
    ChangelogEntry(
        version = "v2.0",
        versionCode = 2,
        date = "Oct 4, 2025",
        changes = """
            - Database breaking changes has been done (that can't be migrated), please copy your data to a file uninstall old version and reinstall the new one then paste copied data.
            option to change icons of work-spaces
            - import/export data
            -  more languages support
            - Custom repetition to habits and events
            - UI is more intuitive now.
            - More Customizability: multiple theme palettes
        """.trimIndent()
    ),
    ChangelogEntry(
        version = "v2.1",
        versionCode = 3,
        date = "Oct 23, 2025",
        changes = """
            - More Customizability: custom font options.
            - Different Notification Icon for event, habit.
            - import/export and share notes in markdown/txt file.
        """.trimIndent()
    ),
    ChangelogEntry(
        version = "v2.2",
        versionCode = 4,
        date = "Nov 22, 2025",
        changes = """
            - Image Support in Notes.
            - Mark status of Event/Habits through Notification.
            - Added end date for events/habits.
            - Some UI changes in Edit Event.
            - Added more analytics components for habits.
            - Added support to many other languages (German, Russian, Portuguese (Brazil), Spanish).
            - Bug fixes of staggered List in notes, state disappearing in rotation.
        """.trimIndent()
    ),
    ChangelogEntry(
        version = "v3.0",
        versionCode = 5,
        date = "Feb 16, 2026",
        changes = """
            - Markdown Support in Notes & Journal.
            - Share Notes and Journal as markdown, html, image, pdf.
            - Improved To-do UI
            - Merged Calendar with Events and Journal.
        """.trimIndent()
    ),
    ChangelogEntry(
        version = "v3.1.1",
        versionCode = 6,
        date = "Feb 24, 2026",
        changes = """
            - Audio recorder added to notes and journal.
            - Privacy Policy and User guide added in About.
            - Removal of redundant editor options in Customize Screen.
            - Automatic Backup manager added in Flux.
        """.trimIndent()
    ),
    ChangelogEntry(
        version = "v3.1.2",
        versionCode = 7,
        date = "Mar 17, 2026",
        changes = """
            - Fixed Automatic Backup Manager
            - Indication in Monthly Calendar view for both events and journals
            - New Themes page with preview in Customize settings
            - Storage Selection page is added to ensure storage root selection for data, backup
            - Fixed Crash in Save Notes, Journals.
        """.trimIndent()
    ),
    ChangelogEntry(
        version = "v3.1.3",
        versionCode = 8,
        date = "Mar 22, 2026",
        changes = """
            - New Space Addition: Progress Tracker
            - Database Migration query to be crash-free
            - Auto-Capitalization on all the text fields
        """.trimIndent()
    ),
    ChangelogEntry(
        version = "v3.1.4",
        versionCode = 9,
        date = "Mar 28, 2026",
        changes = """
            - Fixed Progress Tracker Delete bug
            - New UI view for Extreme Compact Mode
            - Fixed Habit Streak calculation bug
            - Fixed Compact Mode workspace spacing issue
            - Fixed Default Editor Visibility Bug
            - Fixed Backup import Failure Bug
            - Added System Font in settings
            - Sticky Notification in Habits
        """.trimIndent()
    ),
    ChangelogEntry(
        version = "v3.1.5",
        versionCode = 10,
        date = "May 24, 2026",
        changes = """
            feat!:
               - Counter Habit.
               - Scrollable workspace cover.
               - UI Improvement.
               - Global searching.
               - Filters in notes, journal, global search.
               - Timeline and labels in journal.
               - Journal Heat map in analytics.
               - Additional analytics item in habits.
            
            fix!:
            
               - Habit description visibility.
               - Journal data deletion on habit space removal.
               - Correction in Streak calculation.

        """.trimIndent()
    ),
    ChangelogEntry(
        version = "v3.1.6",
        versionCode = 11,
        date = "Jun 7, 2026",
        changes = """
            feat:
               - Added Undo in Todo Item removal
               - Draggable Items to reorder in todo.
               - Reminder in Todo Items to remind and analyse the list.
            fix:
               - Dated Journal entry bug
               - Create Button Text overflow in note/journal.
        """.trimIndent()
    ),
    ChangelogEntry(
        version = "v3.1.7",
        versionCode = 12,
        date = "Jun 7, 2026",
        changes = """
            feat:
                1. Notes Preview Mode to adjust notes height
            src:
                1. Improved Markdown Render in preview mode for media, links and code-blocks
            fix:
                1. Progress Tracker date bug.
                2. Automatically detect line break in editor
        """.trimIndent()
    ),
    ChangelogEntry(
        version = "v3.1.8",
        versionCode = 13,
        date = "Jun 25, 2026",
        changes = """
            feat:
                - Content copy/move to another workspaces
                - Various export options in todo
                - Clone a data point
                - Responsive UI for various display size.
                - Day addition in journal timeline with 24-hour format support
           
            fix:
                - text overflow at various places
        """.trimIndent()
    ),
    ChangelogEntry(
        version = "v3.1.9",
        versionCode = 14,
        date = "July 12, 2026",
        changes = """
            feat:
                - Share achievement in habits
                - Widgets in habits and todo list.
            
            fix:
                - weekly option selection bug.

        """.trimIndent()
    ),
    ChangelogEntry(
        version = "v3.1.10",
        versionCode = 15,
        date = "July 19, 2026",
        changes = """
            feat:
                - Share achievement in habits
                - Widgets in habits and todo list.
            
            fix:
                - weekly option selection bug.

        """.trimIndent()
    ),
)
