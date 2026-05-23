package com.flux.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun SelectableColorPlatte(
    modifier: Modifier = Modifier,
    selected: Boolean,
    colorScheme: ColorScheme,
    onClick: () -> Unit
) = Box(modifier = modifier.clip(MaterialTheme.shapes.large)) {
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .padding(6.dp)
            .size(48.dp),
        shape = CircleShape,
        color = colorScheme.primary,
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .offset((-24).dp, 24.dp),
                color = colorScheme.tertiary,
            ) {}
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .offset(24.dp, 24.dp),
                color = colorScheme.secondaryContainer,
            ) {}
            AnimatedVisibility(
                visible = selected,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(colorScheme.tertiaryContainer),
                enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Checked",
                    modifier = Modifier
                        .padding(8.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.onTertiary
                )
            }
        }
    }
}

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

                        is SettingIcon.Resource -> AsyncImage(
                            model = it.resId,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
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

@Composable
fun SettingCategory(
    title: String,
    subTitle: String = "",
    icon: ImageVector,
    shape: RoundedCornerShape,
    isLast: Boolean = false,
    action: () -> Unit = {},
    composableAction: @Composable (() -> Unit) -> Unit = {},
) {
    var showCustomAction by remember { mutableStateOf(false) }
    if (showCustomAction) composableAction { showCustomAction = !showCustomAction }

    ElevatedCard(
        shape = shape,
        modifier = Modifier
            .clip(shape)
            .clickable {
                showCustomAction = showCustomAction.not()
                action()
            },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        )
    ) {
        Row(
            modifier = Modifier
                .clip(shape)
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RenderCategoryIcon(icon = icon)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                RenderCategoryTitle(title = title)
                RenderCategoryDescription(subTitle = subTitle)
            }
        }
    }
    Spacer(modifier = Modifier.height(if (isLast) 12.dp else 2.dp))
}

fun shapeManager(
    isBoth: Boolean = false,
    isLast: Boolean = false,
    isFirst: Boolean = false,
    radius: Int
): RoundedCornerShape {
    val smallerRadius: Dp = (radius / 5).dp
    val defaultRadius: Dp = radius.dp

    return when {
        isBoth -> RoundedCornerShape(defaultRadius)
        isLast -> RoundedCornerShape(smallerRadius, smallerRadius, defaultRadius, defaultRadius)
        isFirst -> RoundedCornerShape(defaultRadius, defaultRadius, smallerRadius, smallerRadius)
        else -> RoundedCornerShape(smallerRadius)
    }
}

@Composable
fun CircleWrapper(
    color: Color = MaterialTheme.colorScheme.background,
    size: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = color,
                shape = RoundedCornerShape(50)
            )
            .padding(size),
    ) {
        content()
    }
}

@Composable
fun MaterialText(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    titleSize: TextUnit = 14.sp,
    descriptionSize: TextUnit = 11.sp,
    center: Boolean = false,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    descriptionColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = titleSize),
            color = titleColor,
            textAlign = if (center) TextAlign.Center else TextAlign.Start
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = descriptionSize),
                color = descriptionColor,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RenderRadio(
    enabled: Boolean,
    onRadioEnabled: () -> Unit
) {
    RadioButton(
        selected = enabled,
        onClick = {
            onRadioEnabled()
        },
        modifier = Modifier
            .scale(0.9f)
            .padding(0.dp)
    )
}

@Composable
fun RenderCategoryTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun RenderCategoryDescription(subTitle: String) {
    if (subTitle.isNotBlank()) {
        Text(
            color = MaterialTheme.colorScheme.primary,
            text = subTitle,
            fontSize = 10.sp
        )
    }
}

@Composable
fun RenderCategoryIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(50)
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .scale(1f)
                .padding(9.dp)
        )
    }
}
