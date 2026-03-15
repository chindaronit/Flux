package com.flux.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.navigation.NavRoutes
import com.flux.other.ensureStorageRoot
import com.flux.ui.components.CircleWrapper
import com.flux.ui.viewModel.SettingsViewModel

@Composable
fun StorageSelectionScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel,
    isStorageRootSelected: Boolean
) {
    // Navigate immediately if storage root is already selected (inside LaunchedEffect)
    LaunchedEffect(isStorageRootSelected) {
        if (isStorageRootSelected) {
            navController.navigate(NavRoutes.Workspace.route) {
                popUpTo(NavRoutes.StorageSelection.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val rootPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            settingsViewModel.saveRootUri(uri)
        }

    val scope = rememberCoroutineScope()

    if(!isStorageRootSelected){
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { },
                        shape = RoundedCornerShape(50),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                CircleWrapper(
                                    size = 0.dp,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Icon(
                                        painter = painterResource(R.mipmap.ic_launcher_foreground),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Select where to store app data including images, audio, videos, documents, backup",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = {
                        ensureStorageRoot(scope, settingsViewModel, rootPicker){
                            navController.navigate(NavRoutes.Workspace.route) {
                                popUpTo(NavRoutes.StorageSelection.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, null)
                            Text("Select Folder")
                        }
                    }
                }
            }
        }
    }
}