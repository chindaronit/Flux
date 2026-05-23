package com.flux.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flux.data.model.RecurrenceRule

enum class SelectionType {
    SINGLE,
    MULTIPLE,
    DATE,
    RECURRENCE
}

data class FilterOption(
    val id: String,
    val label: String,
    val date: Long?= null,
    val recurrenceRule: RecurrenceRule? = null
)

data class FilterCategory(
    val name: String,
    val options: List<FilterOption>,
    val type: SelectionType
)

data class SearchFilterOption(
    val id: String,
    val label: String
)

data class SearchFilterCategory(
    val name: String,
    val options: List<SearchFilterOption>,
    val type: SelectionType
)

@Composable
fun CategoryRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isSelected)
                    Modifier.drawLeftBorder(
                        MaterialTheme.colorScheme.primary,
                        4.dp
                    )
                else Modifier
            )
            .padding(12.dp)
    ) {
        Text(
            text = label,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun OptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f))
        RadioButton(selected = isSelected, onClick = onClick)
    }
}

@Composable
fun DateOptionRow(
    date: Long?,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label)
            if(date!=null) Text(convertMillisToDate(date), style = MaterialTheme.typography.labelSmall)
        }

        IconButton(onClick) {
            Icon(
                Icons.Default.DateRange,
                null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MultiOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f))
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onClick() }
        )
    }
}

fun Modifier.drawLeftBorder(color: Color, width: Dp): Modifier = this.drawWithContent {
    drawContent()
    drawLine(
        color = color,
        start = Offset(0f, 0f),
        end = Offset(0f, size.height),
        strokeWidth = width.toPx()
    )
}
