package com.flux.ui.screens.workspaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.WorkspaceModel
import com.flux.data.model.getSpacesList
import com.flux.other.icons
import com.flux.ui.common.ChangeIconSheet
import com.flux.ui.common.CompactCard
import com.flux.ui.common.EditorScaffold
import com.flux.ui.events.WorkspaceEvents
import com.flux.ui.screens.events.getTextFieldColors
import kotlinx.coroutines.launch
import kotlin.collections.forEach
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import com.flux.data.model.Space
import com.flux.ui.common.DeleteAlert
import com.flux.ui.screens.todo.move
import com.flux.ui.viewModel.ViewModels
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewWorkspaceScreen(
    navController: NavController,
    workspace: WorkspaceModel = WorkspaceModel(),
    viewModels: ViewModels,
    onEvent: (WorkspaceEvents) -> Unit,
){
    val context = LocalContext.current
    val allSpaces = getSpacesList()
    val isNew = workspace.title.isBlank()
    var title by remember { mutableStateOf(workspace.title) }
    var description by remember { mutableStateOf(workspace.description) }
    var icon by remember { mutableIntStateOf(workspace.icon) }
    var passkey by remember { mutableStateOf(workspace.passKey) }
    val focusRequesterDesc = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var isSheetVisible by remember { mutableStateOf(false) }
    var isDialogVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedSpacesId = remember {
        mutableStateListOf<Int>().apply { addAll(workspace.selectedSpaces.distinct()) }
    }
    var canDrag by remember { mutableStateOf(false) }
    var showSpaceDeleteWarningDialog by remember { mutableStateOf(false) }
    val originalSpacesId = remember { workspace.selectedSpaces.distinct() }

    val removedSpaces by remember {
        derivedStateOf {
            originalSpacesId
                .filter { it !in selectedSpacesId }
                .mapNotNull { id -> allSpaces.firstOrNull { it.id == id } }
        }
    }

    fun saveWorkspace() {
        onEvent(
            WorkspaceEvents.UpsertSpace(
                workspace.copy(
                    title = title,
                    description = description,
                    icon = icon,
                    passKey = passkey,
                    selectedSpaces = selectedSpacesId.toList()
                )
            )
        )
    }

    if (showSpaceDeleteWarningDialog) {
        DeleteAlert(
            onConfirmation = {
                removedSpaces.forEach { removeSpaceData(workspace.workspaceId, it.id, context, viewModels) }
                showSpaceDeleteWarningDialog = false
                saveWorkspace()

                navController.popBackStack()
            },
            onDismissRequest = { showSpaceDeleteWarningDialog = false },
            dialogTitle = stringResource(R.string.delete_alert),
            dialogText = stringResource(R.string.delete_spaces_alert, removedSpaces.joinToString { it.title })
        )
    }

    EditorScaffold(
        title = if(isNew) stringResource(R.string.Add_Workspace) else stringResource(R.string.Edit_Workspace),
        canSave = title.isNotBlank(),
        onBackPressed = { navController.popBackStack() },
        onDone = {
            if(removedSpaces.isNotEmpty() && !isNew){ showSpaceDeleteWarningDialog=true }
            else{
                saveWorkspace()
                navController.popBackStack()
            }
        }
    ) { innerPadding->
        Column(Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(12.dp)) {
            TextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 3.dp),
                placeholder = { Text(stringResource(R.string.Title)) },
                singleLine = true,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = getTextFieldColors(),
                keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusRequesterDesc.requestFocus() })
            )

            TextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequesterDesc),
                placeholder = { Text(stringResource(R.string.Description)) },
                singleLine = true,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                colors = getTextFieldColors(),
                keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                    }
                )
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Row(Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween){
                Text(stringResource(R.string.icon), style = MaterialTheme.typography.bodyLarge)
                IconButton(
                    { isSheetVisible = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) { Icon(icons[icon], null) }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Row(Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween){
                Text(stringResource(R.string.Lock_Workspace), style = MaterialTheme.typography.bodyLarge)
                Switch(passkey!=null, onCheckedChange = { passkey = if(it) "" else null })
            }

            passkey?.let {
                Row(Modifier
                    .fillMaxWidth()
                    .padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween){
                    Text(stringResource(R.string.passkey), style = MaterialTheme.typography.bodyLarge)

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if(it.isNotBlank()) Text("****")
                        IconButton(
                            { isDialogVisible = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) { Icon(Icons.Default.Edit, null) }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            if (selectedSpacesId.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.selected_spaces))

                    if (selectedSpacesId.size > 1) {
                        IconButton(
                            modifier = Modifier.size(36.dp),
                            onClick = { canDrag = !canDrag }) {
                            Icon(
                                if (canDrag) Icons.Default.LockOpen else Icons.Default.Lock,
                                null
                            )
                        }
                    }
                }

                if (canDrag) {
                    Text(
                        stringResource(R.string.drag_horizontally_to_reorder),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (canDrag) {
                SelectedSpacesOrderEditor(
                    selectedSpacesId = selectedSpacesId,
                    allSpaces = allSpaces
                )
            } else {
                FlowRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedSpacesId.forEach { spaceId ->
                        val space = allSpaces.firstOrNull { it.id == spaceId } ?: return@forEach
                        CompactCard(space.icon, space.title) { selectedSpacesId.remove(space.id) }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            if (selectedSpacesId.size!=allSpaces.size) Text(
                if (selectedSpacesId.isEmpty()) stringResource(R.string.available_spaces) else stringResource(
                    R.string.other_available
                )
            )
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allSpaces.forEach { space ->
                    if (selectedSpacesId.contains(space.id)) return@forEach
                    CompactCard(space.icon, space.title) { selectedSpacesId.add(space.id) }
                }
            }
        }
    }

    ChangeIconSheet (
        isVisible = isSheetVisible,
        sheetState = sheetState,
        onDismiss = { scope.launch { sheetState.hide() }.invokeOnCompletion { isSheetVisible = false } },
        onConfirm = { idx-> scope.launch { sheetState.hide() }.invokeOnCompletion { icon = idx } }
    )

    if (isDialogVisible) { SetPasskeyDialog(passkey,{ passkey = it }) { isDialogVisible = false } }
}

@Composable
fun SelectedSpacesOrderEditor(
    selectedSpacesId: SnapshotStateList<Int>,
    allSpaces: List<Space>
) {
    val canReorder = selectedSpacesId.size > 1
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState
    ) { from, to ->
        if (canReorder) selectedSpacesId.move(from.index, to.index)
    }

    LazyRow(
        state = lazyListState,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(
            items = selectedSpacesId,
            key = { _, spaceId -> spaceId }
        ) { _, spaceId ->
            val space = allSpaces.firstOrNull { it.id == spaceId } ?: return@itemsIndexed

            ReorderableItem(
                state = reorderableState,
                key = spaceId
            ) { _ ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .let { if (canReorder) it.longPressDraggableHandle() else it },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactCard(space.icon, space.title) {  }
                }
            }
        }
    }
}
