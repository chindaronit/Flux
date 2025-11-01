package com.flux.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ActionType {
    RADIOBUTTON,
    SWITCH,
    LINK,
    CUSTOM,
    CLIPBOARD,
    None
}

sealed class SettingIcon {
    data class Vector(val icon: ImageVector) : SettingIcon()
    data class Resource(val resId: Int) : SettingIcon()
}

@Composable
fun SingleSettingOption(
    radius: Int,
    text: String,
    description: String? = null,
    trailingIcon: SettingIcon? = null,
    leadingIcon: SettingIcon? = null,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    first: Boolean = false,
    last: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(top = if (first) 16.dp else 0.dp, bottom = if (last) 16.dp else 0.dp)
            .clip(shapeManager(isBoth = true, radius = radius))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
            .clickable { onClick() },
        shape = shapeManager(isBoth = true, radius = radius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                leadingIcon?.let {
                    when (it) {
                        is SettingIcon.Vector -> CircleWrapper(
                            size = 12.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Icon(
                                imageVector = it.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        is SettingIcon.Resource -> CircleWrapper(
                            size = 0.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Icon(
                                painter = painterResource(it.resId),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text, style = textStyle, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))

                if (description != null) {
                    Text(
                        description,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraLight
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                trailingIcon?.let {
                    CircleWrapper(
                        size = 12.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        when (it) {
                            is SettingIcon.Vector -> Icon(
                                imageVector = it.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            is SettingIcon.Resource -> Icon(
                                painter = painterResource(it.resId),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingOption(
    radius: RoundedCornerShape? = null,
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    size: Dp = 12.dp,
    actionType: ActionType,
    variable: Boolean? = null,
    isEnabled: Boolean = true,
    switchEnabled: (Boolean) -> Unit = {},
    linkClicked: () -> Unit = {},
    customButton: @Composable () -> Unit = { RenderCustomIcon() },
    onCustomClick: () -> Unit = {},
    clipboardText: String = "",
) {
    val context = LocalContext.current

    AnimatedVisibility(visible = isEnabled) {
        Box(
            modifier = Modifier
                .padding(bottom = 3.dp)
                .clip(radius ?: RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                .clickable {
                    handleAction(
                        context,
                        actionType,
                        variable,
                        switchEnabled,
                        onCustomClick,
                        linkClicked,
                        clipboardText
                    )
                }
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
                    icon?.let {
                        CircleWrapper(
                            size = 12.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
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
                RenderActionComponent(
                    actionType,
                    variable,
                    switchEnabled,
                    linkClicked,
                    customButton
                )
            }
        }
    }
}

private fun handleAction(
    context: Context,
    actionType: ActionType,
    variable: Boolean?,
    onSwitchEnabled: (Boolean) -> Unit,
    customAction: () -> Unit,
    onLinkClicked: () -> Unit,
    clipboardText: String
) {
    when (actionType) {
        ActionType.RADIOBUTTON -> onSwitchEnabled(variable == false)
        ActionType.SWITCH -> onSwitchEnabled(variable == false)
        ActionType.LINK -> onLinkClicked()
        ActionType.CUSTOM -> customAction()
        ActionType.CLIPBOARD -> copyToClipboard(context, clipboardText)
        ActionType.None -> {}
    }
}

@Composable
private fun RenderClipboardIcon() {
    Icon(
        imageVector = Icons.Default.ContentCopy,
        contentDescription = null,
        modifier = Modifier.padding(12.dp),
        tint = MaterialTheme.colorScheme.primary
    )
}

fun copyToClipboard(context: Context, clipboardText: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = android.content.ClipData.newPlainText("Copied Text", clipboardText)
    clipboard.setPrimaryClip(clip)
}

@Composable
private fun RenderActionComponent(
    actionType: ActionType,
    variable: Boolean?,
    onSwitchEnabled: (Boolean) -> Unit,
    onLinkClicked: () -> Unit,
    customButton: @Composable () -> Unit
) {
    when (actionType) {
        ActionType.RADIOBUTTON -> RenderRadioButton(variable, onSwitchEnabled)
        ActionType.SWITCH -> RenderSwitch(variable, onSwitchEnabled)
        ActionType.LINK -> RenderLinkIcon(onLinkClicked)
        ActionType.CLIPBOARD -> RenderClipboardIcon()
        ActionType.CUSTOM -> customButton()
        ActionType.None -> {}
    }
}

@Composable
private fun RenderRadioButton(variable: Boolean?, onSwitchEnabled: (Boolean) -> Unit) {
    RadioButton(
        selected = variable == true,
        onClick = { onSwitchEnabled(true) }
    )
}

@Composable
private fun RenderSwitch(variable: Boolean?, onSwitchEnabled: (Boolean) -> Unit) {
    Switch(
        checked = variable == true,
        onCheckedChange = { onSwitchEnabled(it) },
        thumbContent = if (variable == true) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        } else {
            null
        }
    )
}

@Composable
private fun RenderLinkIcon(onLinkClicked: () -> Unit) {
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
        contentDescription = null,
        modifier = Modifier
            .padding(16.dp)
            .clickable { onLinkClicked() },
        tint = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun RenderCustomIcon() {
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
        contentDescription = null,
        modifier = Modifier
            .scale(0.6f)
            .padding(12.dp)
    )
}

