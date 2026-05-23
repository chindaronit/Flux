package com.flux.ui.screens.journal

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.JournalModel
import com.flux.data.model.LabelModel
import com.flux.data.model.WorkspaceModel
import com.flux.navigation.Loader
import com.flux.navigation.NavRoutes
import com.flux.ui.common.EmptyJournal
import com.flux.ui.common.SpaceSearchBar
import com.flux.ui.common.SpaceTopBar
import com.flux.ui.common.SpacesMenu
import com.flux.ui.common.convertMillisToDate
import com.flux.ui.common.convertMillisToTime
import com.flux.ui.events.JournalEvents
import com.flux.ui.screens.workspaces.SpacesToolBar
import com.flux.ui.state.JournalState
import com.flux.ui.state.Settings
import java.time.Instant
import java.time.ZoneId

data class FilterState(
    val sort: String? = null,
    val selectedDate: Long? = null,
    val selectedLabelIds: Set<String> = emptySet()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    navController: NavController,
    state: JournalState,
    settings: Settings,
    workspace: WorkspaceModel,
    allLabels: List<LabelModel>,
    onShowSpaceBottomSheet: () -> Unit,
    onSpaceChange: (Int) -> Unit,
    onAddCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onDeleteWorkspace: () -> Unit,
    onToggleLock: () -> Unit,
    onEvent: (JournalEvents) -> Unit
){
    val workspaceId = workspace.workspaceId
    val isLoading = state.isLoading
    val radius = settings.data.cornerRadius
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var showSpacesMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var isToolsSheetVisible by remember { mutableStateOf(false) }
    var isActionButtonExpanded by remember { mutableStateOf(false) }
    val importSuccess = stringResource(R.string.import_success)
    val importFailed = stringResource(R.string.import_failed)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var filterState by remember { mutableStateOf(FilterState()) }
    val labelMap = remember(allLabels) { allLabels.associateBy { it.labelId } }

    val allEntries = state.data
        .filter { it.workspaceId==workspaceId }
        // 🔍 Search
        .filter { entry ->
            val matchesText = entry.text.contains(query, ignoreCase = true)
            val matchesTime = convertMillisToTime(entry.dateTime).contains(query, ignoreCase = true) ||
                    convertMillisToDate(entry.dateTime).contains(query, ignoreCase = true)

            val matchesLabel =
                entry.labels.any { labelId ->
                    labelMap[labelId]?.value
                        ?.contains(query, true) == true
                }

            matchesText || matchesLabel || matchesTime
        }

        // 🏷 Label filter
        .let { entry ->
            if (filterState.selectedLabelIds.isEmpty()) entry
            else entry.filter { e ->
                e.labels.any { it in filterState.selectedLabelIds }
            }
        }

        // 🔄 Sorting
        .let { entries ->
            when (filterState.sort) {
                "latest" -> entries.sortedByDescending { it.dateTime }
                "oldest" -> entries.sortedBy { it.dateTime }
                else -> entries
            }
        }

        // 🔄 Dated
        .let { list ->
            filterState.selectedDate?.let { selected ->
                val zone = ZoneId.systemDefault()

                val selectedDay = Instant.ofEpochMilli(selected)
                    .atZone(zone)
                    .toLocalDate()
                    .toEpochDay()

                list.filter { item ->
                    val itemDay = Instant.ofEpochMilli(item.dateTime)
                        .atZone(zone)
                        .toLocalDate()
                        .toEpochDay()

                    itemDay == selectedDay
                }
            } ?: list
        }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""

                // Create a new note
                val newEntry = JournalModel(
                    text = content,
                    workspaceId = workspaceId,
                    dateTime = System.currentTimeMillis()
                )

                onEvent(JournalEvents.UpsertEntry(newEntry))
                Toast.makeText(context, importSuccess, Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, importFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            SpaceTopBar(
                scrollBehavior = scrollBehavior,
                title            = workspace.title,
                description      = workspace.description,
                cover            = workspace.cover,
                icon             = workspace.icon,
                isLocked         = workspace.passKey!=null,
                onBackPressed    = { navController.popBackStack() },
                onAddCover       = onAddCover,
                onRemoveCover    = onRemoveCover,
                onToggleLock     = onToggleLock,
                onDeleteWorkspace = onDeleteWorkspace,
                onEditWorkspace   = { navController.navigate(NavRoutes.NewWorkspace.withArgs(workspaceId)) }
            )
        },
        floatingActionButton = {
            Box(contentAlignment = Alignment.BottomEnd) {
                // Expanded actions
                AnimatedVisibility(
                    visible = isActionButtonExpanded,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        val buttonModifier = Modifier.width(120.dp)

                        ExtendedFloatingActionButton(
                            modifier = buttonModifier,
                            onClick = { importLauncher.launch(arrayOf("text/markdown", "text/plain")) },
                            icon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                            text = { Text("Import") }
                        )

                        ExtendedFloatingActionButton(
                            modifier = buttonModifier,
                            onClick = { navController.navigate(NavRoutes.EditJournal.withArgs(workspaceId, "", System.currentTimeMillis())) },
                            icon = { Icon(Icons.Outlined.Create, contentDescription = null) },
                            text = { Text("Create") }
                        )

                        ExtendedFloatingActionButton(
                            modifier = buttonModifier,
                            onClick = { isActionButtonExpanded = false },
                            icon = { Icon(Icons.Outlined.Clear, contentDescription = null) },
                            text = { Text("Clear") }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !isActionButtonExpanded,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    FloatingActionButton( { isActionButtonExpanded = true }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            isLoading -> Loader()
            else -> {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(12.dp)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                ) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom=8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if(showSearchBar){ SpaceSearchBar(query, { query=it }, { showSearchBar=false }) }
                            else {
                                SpacesToolBar(
                                    stringResource(R.string.Journal),
                                    Icons.Default.AutoStories,
                                    false,
                                    onMainClick = { showSpacesMenu = true },
                                    onEditClick = onShowSpaceBottomSheet
                                )

                                SpacesMenu(
                                    showSpacesMenu,
                                    workspace,
                                    onSpaceChange
                                ) { showSpacesMenu = false }

                                Row {
                                    IconButton({ showSearchBar = true }) {
                                        Icon(
                                            Icons.Default.Search,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton({ isToolsSheetVisible = true }) {
                                        Icon(
                                            Icons.Default.FilterList,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if(allEntries.isEmpty()) item { EmptyJournal() }
                    items(allEntries) { entry->
                        JournalCardHeader(convertMillisToDate(entry.dateTime) + ", " + convertMillisToTime(entry.dateTime))
                        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                            TimelineBody(isLast = false)
                            JournalPreview(radius, entry.text, allLabels.filter { entry.labels.contains(it.labelId) }) {
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

    if(isToolsSheetVisible) {
        JournalFilterSheet(
            filterState,
            allLabels,
            rememberModalBottomSheetState(),
            onDismiss = { isToolsSheetVisible = false },
        ) { single, multi, date ->
            filterState = FilterState(
                sort = single["Sort By"],
                selectedLabelIds = multi["Labels"] ?: emptySet(),
                selectedDate = date
            )
        }
    }
}

