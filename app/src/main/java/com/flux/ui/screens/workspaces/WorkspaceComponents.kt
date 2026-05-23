package com.flux.ui.screens.workspaces

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.flux.R
import com.flux.data.model.Space
import com.flux.data.model.getSpacesList
import com.flux.other.icons
import com.flux.ui.common.DeleteAlert
import com.flux.ui.screens.settings.shapeManager

// ------------- Dialog -------------
@Composable
fun SetPasskeyDialog(key: String?=null, onConfirmRequest: (String) -> Unit, onDismissRequest: () -> Unit) {
    var passKey by remember { mutableStateOf(key?: "") }

    Dialog(onDismissRequest) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.enterPasskey),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = passKey,
                    singleLine = true,
                    onValueChange = { passKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onConfirmRequest(passKey)
                            onDismissRequest()
                        }
                    )
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onDismissRequest) { Text(stringResource(R.string.Dismiss)) }
                    TextButton(
                        onClick = {
                            onConfirmRequest(passKey)
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors()
                    ) { Text(stringResource(R.string.Confirm)) }
                }
            }
        }
    }
}

// ------------- Bottom Sheet -------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewSpacesBottomSheet(
    isVisible: Boolean,
    sheetState: SheetState,
    selectedSpaces: List<Space>,
    onDismiss: () -> Unit,
    onRemove: (Int) -> Unit,
    onSelect: (Int) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var removeSpace by remember { mutableIntStateOf(-1) }
    val spacesList = getSpacesList()
    if (showDeleteDialog) {
        DeleteAlert(onConfirmation = {
            onRemove(removeSpace)
            removeSpace = -1
            showDeleteDialog = false
        }, onDismissRequest = {
            showDeleteDialog = false
        })
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            LazyColumn(Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)) {
                if (selectedSpaces.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.Current),
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                item {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        selectedSpaces.forEach { space ->
                            SpaceCard(space, true) {
                                removeSpace = space.id
                                showDeleteDialog = true
                            }
                        }
                    }
                }
                if (selectedSpaces.size != spacesList.size) {
                    item {
                        Text(
                            stringResource(R.string.Available_Spaces),
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                item {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        spacesList.filterNot { id -> selectedSpaces.contains(id) }
                            .forEach { space ->
                                SpaceCard(space, false) { onSelect(space.id) }
                            }
                    }
                }
            }
        }
    }
}

// ------------- Card -------------
@Composable
fun SpaceCard(space: Space, isSelected: Boolean, onClick: () -> Unit) {
    val cardContainerColor =
        if (isSelected) MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp) else MaterialTheme.colorScheme.surfaceContainerHigh
    val cardContentColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val iconContainerColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val iconContentColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.clip(RoundedCornerShape(50)),
        shape = RoundedCornerShape(50),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor,
            contentColor = cardContentColor
        )
    ) {
        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = onClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = iconContainerColor,
                    contentColor = iconContentColor
                )
            ) { Icon(space.icon, null, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(2.dp))
            Text(space.title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun WorkspaceCard(
    gridColumns: Int,
    radius: Int,
    isLocked: Boolean = false,
    cover: String,
    title: String,
    description: String,
    iconIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPressed: ()->Unit
){
    val coverHeight = when (gridColumns) {
        1 -> 120.dp
        2 -> 100.dp
        else -> 80.dp
    }

    val maxTitleLines = when (gridColumns) {
        1 -> 2
        else -> 1
    }

    val maxDescriptionLines = when (gridColumns) {
        1 -> 3
        else -> 2
    }

    val paddingValues = when (gridColumns) {
        1 -> 8.dp
        2 -> 6.dp
        else -> 4.dp
    }

    val iconSize = when (gridColumns) {
        1 -> 28.dp
        2 -> 24.dp
        else -> 18.dp
    }

    val titleStyle = when (gridColumns) {
        1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
        2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
    }

    val descriptionStyle = when (gridColumns) {
        1 -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal)
        2 -> MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal)
        else -> MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraLight)
    }

    Card(
        shape = shapeManager(radius = radius*2),
        modifier = Modifier.fillMaxWidth().padding(horizontal = paddingValues)
            .clip(shapeManager(radius = radius*2))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPressed
            ),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = 4.dp)
        ) {
            if (cover.isBlank()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(coverHeight)
                        .alpha(0.125f)
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            } else {
                AsyncImage(
                    model = cover.toUri(),
                    modifier = Modifier
                        .height(coverHeight)
                        .alpha(0.8f),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
            Row(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = paddingValues),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icons[iconIndex],
                    null,
                    Modifier.size(iconSize),
                    MaterialTheme.colorScheme.primary
                )
                if (isLocked) Icon(
                    Icons.Default.Lock,
                    null,
                    Modifier.size(iconSize),
                    MaterialTheme.colorScheme.primary
                )
                Text(
                    title,
                    modifier = Modifier.padding(start = 4.dp),
                    maxLines = maxTitleLines,
                    style = titleStyle,
                    overflow = TextOverflow.Clip,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                description,
                style = descriptionStyle,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .padding(horizontal = paddingValues),
                maxLines = maxDescriptionLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SpacesToolBar(
    title: String,
    icon: ImageVector,
    isEmptyWorkspace: Boolean,
    onMainClick: () -> Unit = {},
    onEditClick: () -> Unit = {}
) {
    Box(Modifier.clip(RoundedCornerShape(50))
        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
    ) {
        if (isEmptyWorkspace) {
            Row(
                modifier = Modifier
                    .clickable { onEditClick() }
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.Add_Spaces_Content_Desc),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.Add_Space),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onMainClick() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = stringResource(R.string.Space_Content_Desc),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.Space_Content_Desc),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                VerticalDivider(Modifier.fillMaxHeight())

                // Right section (Edit icon)
                Row(Modifier.clickable { onEditClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Edit,
                        modifier = Modifier.padding(6.dp),
                        contentDescription = stringResource(R.string.Edit_Space_Content_Desc),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}