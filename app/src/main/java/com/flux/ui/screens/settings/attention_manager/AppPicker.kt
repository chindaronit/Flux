package com.flux.ui.screens.settings.attention_manager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_META_DATA
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import com.flux.ui.components.ActionType
import com.flux.ui.components.BasicScaffold
import com.flux.ui.components.CircleWrapper
import com.flux.ui.components.MaterialText
import com.flux.ui.components.RenderCustomIcon
import com.flux.ui.components.shapeManager
import com.flux.ui.state.Settings

@Composable
fun AppPicker(navController: NavController, settings: Settings) {
    BasicScaffold(
        title = "hi",
        onBackClicked = { navController.popBackStack() }
    ) { innerPadding ->
        val context = LocalContext.current
        val apps = getInstalledUserApps(context)
        val data = settings.data

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            for (app in apps) {
                item {
                    Application(
                        title = app.loadLabel(context.packageManager).toString(),
                        description = app.packageName,
                        icon = app.loadIcon(context.packageManager).toBitmap(),
                        radius = shapeManager(radius = data.cornerRadius, isLast = true),
                        actionType = ActionType.None
                    )
                }
            }
        }
    }
}

@Composable
fun Application(
    radius: RoundedCornerShape? = null,
    title: String,
    description: String? = null,
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
    val context = LocalContext.current
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
                    icon.let {
                        CircleWrapper(
                            size = 12.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Image(
                                bitmap = icon.asImageBitmap(),
                                contentDescription = "App Icon"
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (!description.isNullOrBlank()) {
                        MaterialText(
                            title = title,
                            description = description.ifBlank { clipboardText }
                        )
                    }
                }
            }
        }
    }
}


fun getInstalledUserApps(context: Context): List<ApplicationInfo> {
    val packageManager: PackageManager = context.packageManager

    val packages: List<ApplicationInfo> = packageManager.getInstalledApplications(GET_META_DATA)

    return packages
}