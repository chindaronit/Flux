package com.flux.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


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