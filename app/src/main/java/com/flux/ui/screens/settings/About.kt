package com.flux.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.flux.BuildConfig
import com.flux.R
import com.flux.ui.components.ActionType
import com.flux.ui.components.BasicScaffold
import com.flux.ui.components.SettingIcon
import com.flux.ui.components.SettingOption
import com.flux.ui.components.SingleSettingOption
import com.flux.ui.components.shapeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun About(navController: NavController, radius: Int) {
    val context = LocalContext.current

    BasicScaffold(
        title = stringResource(R.string.About),
        onBackClicked = { navController.popBackStack() }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp, 8.dp, 16.dp)
        ) {
            item {
                SingleSettingOption(
                    radius,
                    stringResource(R.string.app_name),
                    leadingIcon = SettingIcon.Resource(R.mipmap.ic_launcher_foreground),
                    textStyle = MaterialTheme.typography.titleLarge,
                    last = true
                ) { }
            }

            item {
                SettingOption(
                    title = stringResource(R.string.Build_type),
                    description = BuildConfig.BUILD_TYPE.uppercase(),
                    icon = Icons.Filled.Settings,
                    radius = shapeManager(radius = radius, isFirst = true),
                    actionType = ActionType.None
                )
            }

            item {
                SettingOption(
                    title = stringResource(R.string.Build_version),
                    description = BuildConfig.VERSION_NAME,
                    icon = Icons.Rounded.Info,
                    radius = shapeManager(radius = radius, isLast = true),
                    actionType = ActionType.None
                )
            }

            item {
                Spacer(Modifier.height(24.dp))
                SettingOption(
                    title = stringResource(R.string.Developer),
                    description = stringResource(R.string.Developer_Name),
                    icon = Icons.Rounded.DeveloperMode,
                    radius = shapeManager(radius = radius, isFirst = true),
                    actionType = ActionType.None
                )
            }

            item {
                SettingOption(
                    title = stringResource(R.string.license),
                    description = stringResource(R.string.license_gpl3_only),
                    icon = Icons.AutoMirrored.Rounded.Article,
                    radius = shapeManager(radius = radius),
                    actionType = ActionType.None
                )
            }

            item {
                SettingOption(
                    title = stringResource(R.string.Source_Code),
                    description = stringResource(R.string.Github_Repository),
                    icon = Icons.Rounded.Code,
                    radius = shapeManager(radius = radius, isLast = true),
                    actionType = ActionType.LINK,
                    linkClicked = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/chindaronit/Flux".toUri()
                        )
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}