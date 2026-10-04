package com.flux.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.WorkspaceModel
import com.flux.other.icons
import com.flux.ui.common.BasicScaffold
import com.flux.ui.events.SettingEvents
import com.flux.ui.state.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultWorkspace(
    navController: NavController,
    settings: Settings,
    workspaces: List<WorkspaceModel>,
    onSettingsEvents: (SettingEvents) -> Unit
) {
    val icons = icons
    val filteredWorkspaces = workspaces.filter { !it.isLocked }
    BasicScaffold(
        title = stringResource(R.string.default_screen),
        onBackClicked = { navController.popBackStack() }
    ){ innerPadding ->
        LazyColumn(Modifier.padding(innerPadding).padding(16.dp)) {
            item {
                WorkspaceItem(
                    title = stringResource(R.string.default_screen),
                    isSelected = settings.data.defaultWorkspace==null,
                    shape = shapeManager(radius = settings.data.cornerRadius, isFirst = true),
                    icon = Icons.Default.Settings,
                    description = stringResource(R.string.default_screen_description),
                    onRadioClicked = { onSettingsEvents(SettingEvents.UpdateSettings(settings.data.copy(defaultWorkspace = null))) }
                )
            }

            itemsIndexed(filteredWorkspaces, key = { _, workspace -> workspace.workspaceId }) { index, workspace->
                WorkspaceItem(
                    title = workspace.title,
                    isSelected = settings.data.defaultWorkspace==workspace.workspaceId,
                    shape = shapeManager(
                        radius = settings.data.cornerRadius,
                        isLast = index == filteredWorkspaces.lastIndex
                    ),
                    icon = icons[workspace.icon],
                    description = workspace.description,
                    onRadioClicked = {
                        onSettingsEvents(SettingEvents.UpdateSettings(settings.data.copy(defaultWorkspace = workspace.workspaceId)))
                    }
                )
            }
        }
    }
}

@Composable
private fun WorkspaceItem(
    shape: RoundedCornerShape,
    title: String,
    description: String? = null,
    icon: ImageVector,
    size: Dp = 12.dp,
    isSelected: Boolean,
    onRadioClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(bottom = 3.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
            .clickable { onRadioClicked() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = size)
                .fillMaxWidth()
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                CircleWrapper(
                    size = 12.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) { Icon(imageVector = icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary) }
                Spacer(modifier = Modifier.width(8.dp))
                MaterialText(title = title, description = description)
            }
            RenderRadio(enabled = isSelected, onRadioEnabled = onRadioClicked)
        }
    }
}