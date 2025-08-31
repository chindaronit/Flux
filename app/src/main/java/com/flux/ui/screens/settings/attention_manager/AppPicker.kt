package com.flux.ui.screens.settings.attention_manager

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.flux.ui.components.ActionType
import com.flux.ui.components.BasicScaffold
import com.flux.ui.components.RenderCustomIcon
import com.flux.ui.components.shapeManager
import com.flux.ui.state.Settings
import com.flux.ui.viewModel.AppPickerViewModel

data class App(val label: String, val packageName: String, val icon: Bitmap)

@Composable
fun AppPicker(
    navController: NavController,
    settings: Settings,
    viewModel: AppPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val data = settings.data

    BasicScaffold(
        title = "Application Picker",
        onBackClicked = { navController.popBackStack() }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(uiState.apps) { app ->
                        Application(
                            label = app.label,
                            packageName = app.packageName,
                            icon = app.icon,
                            radius = shapeManager(
                                radius = data.cornerRadius,
                                isLast = true
                            ),
                            actionType = ActionType.None
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Application(
    radius: RoundedCornerShape? = null,
    label: String,
    packageName: String? = null,
    icon: Bitmap,
    size: Dp = 12.dp,
    actionType: ActionType,
    variable: Boolean? = null,
    isEnabled: Boolean = true,
    switchEnabled: (Boolean) -> Unit = {},
    linkClicked: () -> Unit = {},
    customButton: @Composable (() -> Unit) = { RenderCustomIcon() },
    customAction: @Composable ((() -> Unit) -> Unit) = {},
    clipboardText: String = "",
) {
    var showCustomAction by remember { mutableStateOf(false) }
    if (showCustomAction) customAction { showCustomAction = !showCustomAction }

    AnimatedVisibility(visible = isEnabled) {
        Box(
            modifier = Modifier
                .padding(bottom = 3.dp)
                .clip(radius ?: RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(
                        horizontal = 12.dp,
                        vertical = size
                    )
                    .fillMaxWidth()
            ) {
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Image(
                        bitmap = icon.asImageBitmap(),
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(4.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (!packageName.isNullOrBlank()) {
                            Text(
                                text = packageName.ifBlank { clipboardText },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}