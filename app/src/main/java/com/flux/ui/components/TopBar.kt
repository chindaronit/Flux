package com.flux.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.flux.data.model.WorkspaceModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailsTopBar(
    isPinned: Boolean,
    isReadView: Boolean,
    onBackPressed: () -> Unit,
    onReadClick: () -> Unit,
    onEditClick: ()->Unit,
    onDelete: () -> Unit,
    onAddLabel: () -> Unit,
    onTogglePinned: () -> Unit,
    onAboutClicked: () -> Unit,
    onShareNote: () -> Unit,
    onSaveNote: () -> Unit,
    onPrintNote: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Row {
                Box(
                    modifier = Modifier
                        .background(
                            if(!isReadView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                            RoundedCornerShape(bottomStart = 32.dp, topStart = 32.dp)
                        )
                        .clip(RoundedCornerShape(bottomStart = 32.dp, topStart = 32.dp))
                        .clickable { onEditClick() }
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Edit, null, tint= if(!isReadView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(1.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if(isReadView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                            RoundedCornerShape(bottomEnd = 32.dp, topEnd = 32.dp)
                        )
                        .clip(RoundedCornerShape(bottomEnd = 32.dp, topEnd = 32.dp))
                        .clickable { onReadClick() }
                        .padding(8.dp)
                ) { Icon(Icons.Default.RemoveRedEye, null, tint=if(isReadView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary) }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        navigationIcon = { IconButton(onClick = onBackPressed) { Icon(Icons.AutoMirrored.Default.ArrowBack, null) } },
        actions = { DropdownMenuWithDetails(isPinned, onTogglePinned, onAddLabel, onAboutClicked, onShareNote, onSaveNote, onPrintNote, onDelete) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceTopBar(
    workspace: WorkspaceModel,
    onBackPressed: () -> Unit,
    onDelete: () -> Unit,
    onTogglePinned: () -> Unit,
    onToggleLock: () -> Unit,
    onAddCover: () -> Unit,
    onEditDetails: () -> Unit,
    onEditLabel: () -> Unit,
    onRemoveCover: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (workspace.cover.isNotBlank()) 160.dp else 80.dp)
    ) {
        // Background image
        AsyncImage(
            model = workspace.cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // Overlay TopAppBar
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            title = {},
            navigationIcon = {
                IconButton(
                    onClick = onBackPressed,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) { Icon(Icons.AutoMirrored.Default.ArrowBack, null) }
            },
            actions = {
                WorkspaceMore(
                    isCoverAdded = workspace.cover.isNotBlank(),
                    isLocked = workspace.passKey.isNotBlank(),
                    isPinned = workspace.isPinned,
                    showEditLabel = workspace.selectedSpaces.contains(1),
                    onDelete = onDelete,
                    onEditDetails = onEditDetails,
                    onEditLabel = onEditLabel,
                    onRemoveCover = onRemoveCover,
                    onAddCover = onAddCover,
                    onTogglePinned = onTogglePinned,
                    onToggleLock = onToggleLock
                )
            },
            modifier = Modifier.matchParentSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectedBar(
    showDeleteOption: Boolean=true,
    isAllSelected: Boolean,
    isAllSelectedPinned: Boolean,
    selectedItemsSize: Int,
    onPinClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onCloseClick) {
                Icon(
                    Icons.Default.Close,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text("$selectedItemsSize", color = MaterialTheme.colorScheme.primary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = {
                onPinClick()
                onCloseClick()
            }) { Icon(if(isAllSelectedPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null, tint = MaterialTheme.colorScheme.primary)  }
            IconButton(onSelectAllClick) { Icon(if(isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll, null, tint = MaterialTheme.colorScheme.primary) }
            if(showDeleteOption) IconButton({
                onDeleteClick()
            } ) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.primary) }
        }
    }
}
