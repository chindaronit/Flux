package com.flux.ui.screens.workspaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.LaunchedEffect
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewWorkspaceScreen(
    navController: NavController,
    workspace: WorkspaceModel = WorkspaceModel(),
    onEvent: (WorkspaceEvents) -> Unit,
){
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
        mutableStateListOf<Int>().apply{ addAll(workspace.selectedSpaces) }
    }
    var selectedSpaces = allSpaces.filter { selectedSpacesId.contains(it.id) }
    var otherSpaces = allSpaces.filter { !selectedSpacesId.contains(it.id) }

    LaunchedEffect(selectedSpacesId) {
        selectedSpaces = allSpaces.filter { selectedSpacesId.contains(it.id) }
        otherSpaces = allSpaces.filter { !selectedSpacesId.contains(it.id) }
    }

    EditorScaffold(
        title = if(isNew) stringResource(R.string.Add_Workspace) else stringResource(R.string.Edit_Workspace),
        canSave = title.isNotBlank(),
        onBackPressed = { navController.popBackStack() },
        onDone = {
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
            navController.popBackStack()
        }
    ) { innerPadding->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(12.dp)) {
            TextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp),
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
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequesterDesc),
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

            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween){
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
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween){
                Text(stringResource(R.string.Lock_Workspace), style = MaterialTheme.typography.bodyLarge)
                Switch(passkey!=null, onCheckedChange = { passkey = if(it) "" else null })
            }

            passkey?.let {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween){
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

            if(isNew){
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                if(selectedSpaces.isNotEmpty()) Text(stringResource(R.string.selected_spaces))
                FlowRow(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedSpaces.forEach { space ->
                        CompactCard(space.icon, space.title) { selectedSpacesId.remove(space.id) }
                    }
                }
                if(otherSpaces.isNotEmpty()) Text(if(selectedSpaces.isEmpty()) stringResource(R.string.available_spaces) else stringResource(R.string.other_available))
                FlowRow(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    otherSpaces.forEach { space ->
                        CompactCard(space.icon, space.title) { selectedSpacesId.add(space.id) }
                    }
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

