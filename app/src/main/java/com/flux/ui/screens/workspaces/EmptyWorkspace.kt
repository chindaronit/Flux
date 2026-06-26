package com.flux.ui.screens.workspaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.WorkspaceModel
import com.flux.navigation.NavRoutes
import com.flux.data.model.WorkspaceModel
import com.flux.ui.common.SpaceTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyWorkspace(
    navController: NavController,
    workspace: WorkspaceModel,
    allWorkspaces: List<WorkspaceModel> = emptyList(),
    onShowSpaceBottomSheet: () -> Unit,
    onAddCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onDeleteWorkspace: () -> Unit,
    onToggleLock: () -> Unit,
    onWorkspaceSelected: (WorkspaceModel) -> Unit = {},
    onNewWorkspace: () -> Unit = {},
){
    val workspaceId = workspace.workspaceId
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            SpaceTopBar(
                scrollBehavior = scrollBehavior,
                workspace = workspace,
                allWorkspaces = allWorkspaces,
                onAddCover = onAddCover,
                onRemoveCover = onRemoveCover,
                onEditWorkspace = { navController.navigate(NavRoutes.NewWorkspace.withArgs(workspaceId)) },
                onDeleteWorkspace = onDeleteWorkspace,
                onToggleLock = onToggleLock,
                onWorkspaceSelected = onWorkspaceSelected,
                onNewWorkspace = onNewWorkspace
            )
        }
    ) { innerPadding ->
        LazyColumn(
            Modifier.fillMaxSize()
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
                    Button(onShowSpaceBottomSheet){
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.Add, null)
                            Text(stringResource(R.string.Add_Space))
                        }
                    }
                }
            }
        }
    }
}