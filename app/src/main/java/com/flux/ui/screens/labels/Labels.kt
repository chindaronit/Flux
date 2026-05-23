package com.flux.ui.screens.labels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LabelImportant
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.LabelModel
import com.flux.navigation.Loader
import com.flux.ui.common.EmptyLabels
import com.flux.ui.events.LabelEvents
import com.flux.ui.screens.notes.AddLabelDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLabels(
    navController: NavController,
    isLoading: Boolean,
    workspaceId: String,
    allLabels: List<LabelModel>,
    onEvent: (LabelEvents) -> Unit,
) {
    var showAddLabel by remember { mutableStateOf(false) }
    var selectedLabel by remember { mutableStateOf(LabelModel()) }

    if (showAddLabel) {
        AddLabelDialog(
            initialValue = selectedLabel.value,
            onConfirmation = {
                onEvent(
                    LabelEvents.UpsertLabel(
                        selectedLabel.copy(
                            value = it,
                            workspaceId = workspaceId
                        )
                    )
                )
                selectedLabel = LabelModel()
            }
        ) {
            showAddLabel = false
            selectedLabel = LabelModel()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                title = { Text(stringResource(R.string.Edit_Labels)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddLabel = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> Loader()
            allLabels.isEmpty() -> EmptyLabels()
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allLabels) { label ->
                        EditLabelBox(label.value, onDelete = {
                            onEvent(LabelEvents.DeleteLabel(label))
                        }) {
                            showAddLabel = true
                            selectedLabel = label
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditLabelBox(label: String, onDelete: () -> Unit, onEdit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.LabelImportant, null)
            Text(label)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onEdit) { Icon(Icons.Outlined.Edit, null) }
            IconButton(onDelete) { Icon(Icons.Outlined.DeleteOutline, null) }
        }
    }
}