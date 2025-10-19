package com.flux.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    Spacer(modifier = Modifier.height(if (isLast) 24.dp else 2.dp))
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