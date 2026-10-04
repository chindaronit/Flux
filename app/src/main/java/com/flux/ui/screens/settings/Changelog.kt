package com.flux.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.ui.common.BasicScaffold

// ─────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────

@Composable
fun Changelog(
    navController: NavController,
) {
    // Sort once instead of on every recomposition
    val entries = remember { CHANGELOG_DATA.sortedByDescending { it.versionCode } }

    BasicScaffold(
        title = stringResource(R.string.changelog),
        onBackClicked = { navController.popBackStack() }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 8.dp,
                bottom = 24.dp
            )
        ) {
            itemsIndexed(
                items = entries,
                key = { _, item -> item.versionCode }
            ) { index, item ->
                TimelineVersion(
                    entry = item,
                    isFirst = index == 0,
                    isLast = index == entries.lastIndex
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────
// Timeline row: [rail] [card]
// ─────────────────────────────────────────────────────

@Composable
private fun TimelineVersion(
    entry: ChangelogEntry,
    isFirst: Boolean,
    isLast: Boolean
) {
    val primary = MaterialTheme.colorScheme.primary
    val lineColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {

        // ── Rail ──────────────────────────────────────
        Column(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(22.dp))

            if (isFirst) {
                // Highlighted node for the latest release
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .border(2.dp, primary, CircleShape)
                        .padding(4.dp)
                        .background(primary, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(primary.copy(alpha = 0.55f), CircleShape)
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(2.dp)
                        .weight(1f)
                        .background(lineColor)
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // ── Card ──────────────────────────────────────
        VersionCard(
            entry = entry,
            isLatest = isFirst,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun VersionCard(
    entry: ChangelogEntry,
    isLatest: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (isLatest)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Header: version + latest badge, date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = entry.version,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (isLatest) {
                        Row(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "Latest",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = entry.date,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sections
            entry.changes.forEach { section ->
                ChangelogSectionBlock(section)
            }
        }
    }
}

// ─────────────────────────────────────────────────────
// Section (chip + items)
// ─────────────────────────────────────────────────────

@Composable
private fun sectionColor(type: ChangelogType): Color = when (type) {
    ChangelogType.FEAT -> MaterialTheme.colorScheme.primary
    ChangelogType.FIX -> MaterialTheme.colorScheme.error
    ChangelogType.SRC -> MaterialTheme.colorScheme.tertiary
    ChangelogType.CHORE -> MaterialTheme.colorScheme.secondary
}

private fun sectionIcon(type: ChangelogType): ImageVector = when (type) {
    ChangelogType.FEAT -> Icons.Default.CheckCircle
    ChangelogType.FIX -> Icons.Default.Build
    ChangelogType.SRC -> Icons.Default.Code
    ChangelogType.CHORE -> Icons.Default.Tag
}

@Composable
private fun ChangelogSectionBlock(section: ChangelogSection) {
    val color = sectionColor(section.type)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Type chip
        Row(
            modifier = Modifier
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = sectionIcon(section.type),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Text(
                text = section.type.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        // Items
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            section.items.forEach { change ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp, start = 4.dp)
                            .size(5.dp)
                            .background(color.copy(alpha = 0.7f), CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = change,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────
// Models
// ─────────────────────────────────────────────────────
data class ChangelogEntry(
    val version: String,
    val versionCode: Int,
    val date: String,
    val changes: List<ChangelogSection>
)

data class ChangelogSection(
    val type: ChangelogType,
    val items: List<String>
)

enum class ChangelogType(
    val label: String
) {
    FEAT("Features"),
    FIX("Fixes"),
    SRC("Source"),
    CHORE("Chore")
}

// ─────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────

private fun feat(vararg items: String) =
    ChangelogSection(ChangelogType.FEAT, items.toList())

private fun fix(vararg items: String) =
    ChangelogSection(ChangelogType.FIX, items.toList())

private fun src(vararg items: String) =
    ChangelogSection(ChangelogType.SRC, items.toList())

private fun chore(vararg items: String) =
    ChangelogSection(ChangelogType.CHORE, items.toList())

// ─────────────────────────────────────────────────────
// Changelog data (unchanged)
// ─────────────────────────────────────────────────────

val CHANGELOG_DATA = listOf(

    ChangelogEntry(
        version = "v1.0",
        versionCode = 1,
        date = "Aug 10, 2025",
        changes = listOf(
            feat("Initial release of the application.")
        )
    ),

    ChangelogEntry(
        version = "v2.0",
        versionCode = 2,
        date = "Oct 4, 2025",
        changes = listOf(
            feat(
                "Database breaking changes. Copy your data to a file, uninstall the old version, reinstall the new version, then restore the data.",
                "Option to change workspace icons.",
                "Import and export data.",
                "More language support.",
                "Custom repetition for habits and events.",
                "Improved UI.",
                "Multiple theme palettes."
            )
        )
    ),

    ChangelogEntry(
        version = "v2.1",
        versionCode = 3,
        date = "Oct 23, 2025",
        changes = listOf(
            feat(
                "Custom font options.",
                "Different notification icons for events and habits.",
                "Import, export and share notes as Markdown/TXT."
            )
        )
    ),

    ChangelogEntry(
        version = "v2.2",
        versionCode = 4,
        date = "Nov 22, 2025",
        changes = listOf(
            feat(
                "Image support in Notes.",
                "Mark event and habit status through notifications.",
                "End dates for events and habits.",
                "UI improvements in Edit Event.",
                "Additional analytics components for habits.",
                "Additional language support: German, Russian, Portuguese (Brazil), Spanish."
            ),
            fix(
                "Fixed staggered list issues in Notes.",
                "Fixed state disappearing during rotation."
            )
        )
    ),

    ChangelogEntry(
        version = "v3.0",
        versionCode = 5,
        date = "Feb 16, 2026",
        changes = listOf(
            feat(
                "Markdown support in Notes and Journal.",
                "Share Notes and Journal as Markdown, HTML, image and PDF.",
                "Improved To-do UI.",
                "Merged Calendar with Events and Journal."
            )
        )
    ),

    ChangelogEntry(
        version = "v3.1.1",
        versionCode = 7,
        date = "Feb 24, 2026",
        changes = listOf(
            feat(
                "Audio recorder in Notes and Journal.",
                "Privacy Policy and User Guide in About.",
                "Automatic Backup Manager."
            ),
            src("Removed redundant editor options from Customize Screen.")
        )
    ),

    ChangelogEntry(
        version = "v3.1.2",
        versionCode = 8,
        date = "Mar 17, 2026",
        changes = listOf(
            feat(
                "Monthly Calendar indicators for events and journals.",
                "New Themes page with previews.",
                "Storage Selection page for data and backup storage roots."
            ),
            fix(
                "Fixed Automatic Backup Manager.",
                "Fixed crash while saving Notes and Journals."
            )
        )
    ),

    ChangelogEntry(
        version = "v3.1.3",
        versionCode = 9,
        date = "Mar 22, 2026",
        changes = listOf(
            feat(
                "New Progress Tracker space.",
                "Auto-capitalization for text fields."
            ),
            src("Improved database migration queries for crash-free migration.")
        )
    ),

    ChangelogEntry(
        version = "v3.1.4",
        versionCode = 10,
        date = "Mar 28, 2026",
        changes = listOf(
            feat(
                "Extreme Compact Mode UI.",
                "System Font setting.",
                "Sticky notifications for Habits."
            ),
            fix(
                "Fixed Progress Tracker delete bug.",
                "Fixed Habit Streak calculation bug.",
                "Fixed Compact Mode workspace spacing.",
                "Fixed Default Editor visibility bug.",
                "Fixed Backup import failure."
            )
        )
    ),

    ChangelogEntry(
        version = "v3.1.5",
        versionCode = 11,
        date = "May 24, 2026",
        changes = listOf(
            feat(
                "Counter Habit.",
                "Scrollable workspace cover.",
                "UI improvements.",
                "Global search.",
                "Filters in Notes, Journal and Global Search.",
                "Timeline and labels in Journal.",
                "Journal heat map in Analytics.",
                "Additional analytics items in Habits."
            ),
            fix(
                "Fixed Habit description visibility.",
                "Fixed Journal data deletion when removing a Habit space.",
                "Corrected Streak calculation."
            )
        )
    ),

    ChangelogEntry(
        version = "v3.1.6",
        versionCode = 12,
        date = "Jun 7, 2026",
        changes = listOf(
            feat(
                "Undo when removing Todo items.",
                "Drag-and-drop reordering in Todo.",
                "Todo item reminders for analysis and notifications."
            ),
            fix(
                "Fixed dated Journal entry bug.",
                "Fixed Create Button text overflow in Notes and Journal."
            )
        )
    ),

    ChangelogEntry(
        version = "v3.1.7",
        versionCode = 13,
        date = "Jun 7, 2026",
        changes = listOf(
            feat("Notes Preview Mode to adjust note height."),
            src("Improved Markdown rendering in Preview Mode for media, links and code blocks."),
            fix(
                "Fixed Progress Tracker date bug.",
                "Automatically detect line breaks in the editor."
            )
        )
    ),

    ChangelogEntry(
        version = "v3.1.8",
        versionCode = 14,
        date = "Jun 25, 2026",
        changes = listOf(
            feat(
                "Copy and move content between workspaces.",
                "Various Todo export options.",
                "Clone a data point.",
                "Responsive UI for different display sizes.",
                "Journal timeline day addition with 24-hour format support."
            ),
            fix("Fixed text overflow in various places.")
        )
    ),

    ChangelogEntry(
        version = "v3.1.9",
        versionCode = 15,
        date = "July 12, 2026",
        changes = listOf(
            feat(
                "Share achievements in Habits.",
                "Widgets for Habits and Todo List."
            ),
            fix("Fixed weekly option selection bug.")
        )
    ),

    ChangelogEntry(
        version = "v3.1.10",
        versionCode = 16,
        date = "July 19, 2026",
        changes = listOf(
            feat(
                "Share achievements in Habits.",
                "Widgets for Habits and Todo List."
            ),
            fix("Fixed weekly option selection bug.")
        )
    ),

    ChangelogEntry(
        version = "v3.2.0",
        versionCode = 17,
        date = "Aug 1, 2026",
        changes = listOf(
            feat(
                "Backup encryption for all backup exports.",
                "Social links in Notes and Journal.",
                "Media detection support in Journals.",
                "Consistent saving of Habits and Events.",
                "Changelog in About."
            ),
            fix(
                "Fixed local date bug in Events.",
                "Fixed stale EventDetails data after editing an event."
            )
        )
    ),

    ChangelogEntry(
        version = "v3.2.1",
        versionCode = 18,
        date = "Aug 9, 2026",
        changes = listOf(
            feat(
                "Option to change storage root.",
                "Manual drag-and-drop reordering for Notes, Todo and Habits."
            ),
            src("Updated Markdown editor row options with sections."),
            fix("Fixed bugs in the Social dialog in Markdown Editor."),
            chore(
                "Updated changelog.",
                "Updated Gradle library versions in metadata.",
                "Prepared release 3.2.1."
            )
        )
    ),

    ChangelogEntry(
        version = "v3.2.2",
        versionCode = 19,
        date = "Aug 12, 2026",
        changes = listOf(
            feat("Haptic feedback in Notes selection and sliders."),
            fix(
                "Fixed clone label in Notes.",
                "Fixed radio slider bug.",
                "Fixed duplicate content creation when navigating back from Notes, Todo and Journal.",
                "Fixed Journal date bug for new Journals."
            ),
            src("Added more categories to the icon sheet."),
            chore(
                "Updated changelog.",
                "Prepared release 3.2.2."
            )
        )
    ),

    ChangelogEntry(
        version = "v3.2.3",
        versionCode = 20,
        date = "Aug 22, 2026",
        changes = listOf(
            fix(
                "Fixed encrypted backup in Backup Manager.",
                "Fixed duplicate data-point entries.",
                "Fixed app crash when selecting storage root during backup.",
                "Fixed Habit Streak calculation."
            ),
            src(
                "UI improvements in Authentication Screen.",
                "UI improvements in Storage Root Selection."
            ),
            chore("Updated dependencies to latest versions.")
        )
    ),
    ChangelogEntry(
        version = "v3.2.4",
        versionCode = 21,
        date = "Oct 4, 2026",
        changes = listOf(
            feat(
                "Default Workspace screen option in customization",
                "Navigation of workspace from search screen",
                "Add data from search screen",
                "ics import/export option of events"
            ),
            fix(
                "Auto Backup manager bug",
            ),
            src(
                "Added month change option in daily calendar view.",
                "UI improvements in changelog screen",
                "Keep once option as default for event creation"
            ),
            chore(
                "Updated dependencies to latest versions.",
                "Updated version to 3.2.4 (21) for release."
            )
        )
    )
)