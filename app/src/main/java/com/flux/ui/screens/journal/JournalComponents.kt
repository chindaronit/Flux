package com.flux.ui.screens.journal

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LabelImportant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flux.R
import com.flux.data.model.LabelModel
import com.flux.other.parseMarkdownContent
import com.flux.ui.common.CategoryRow
import com.flux.ui.common.DateOptionRow
import com.flux.ui.common.DatePickerModal
import com.flux.ui.common.FilterCategory
import com.flux.ui.common.FilterOption
import com.flux.ui.common.MultiOptionRow
import com.flux.ui.common.OptionRow
import com.flux.ui.common.SelectionType
import com.flux.ui.screens.settings.shapeManager
import kotlin.collections.set

@Composable
fun JournalCardHeader(
    formattedTimestamp: String,
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    typography: Typography = MaterialTheme.typography
) {
    Box(modifier = Modifier
        .wrapContentWidth()
        .padding(bottom = 8.dp, top = 4.dp, start = 4.dp)) {
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(8.dp)
        ) {
            drawCircle(
                color = colorScheme.onSurface,
                radius = size.minDimension / 2
            )
        }

        BasicText(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            text = formattedTimestamp,
            style = typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = ColorProducer { colorScheme.onSurfaceVariant }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalPreview(
    radius: Int,
    content: String,
    labels: List<LabelModel>,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)),
        modifier = Modifier.clip(shapeManager(isBoth = true, radius = radius / 2)).fillMaxWidth(),
        shape = shapeManager(isBoth = true, radius = radius / 2),
        onClick = onClick
    ) {
        Text(
            text = parseMarkdownContent(content),
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .alpha(0.9f)
                .padding(12.dp)
                .heightIn(min = 50.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            labels.forEach { label ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Default.LabelImportant,
                            null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = label.value,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineBody(
    isLast: Boolean,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(0.3f),
    thickness: Dp = 2.dp
) {
    Canvas(
        modifier = Modifier
            .width(16.dp)
            .fillMaxHeight()
    ) {
        if (!isLast) {
            val x = size.width / 2f
            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = thickness.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

fun buildFilterCategories(context: Context,labels: List<LabelModel>, date: Long?): List<FilterCategory> {
    return listOf(
        FilterCategory(
            name = context.getString(R.string.sort_by),
            options = listOf(
                FilterOption("latest", context.getString(R.string.latest)),
                FilterOption("oldest", context.getString(R.string.oldest))
            ),
            type = SelectionType.SINGLE
        ),
        FilterCategory(
            name = context.getString(R.string.date),
            options = listOf(
                FilterOption("date", context.getString(R.string.filter_by_date), date)
            ),
            type = SelectionType.DATE
        ),
        FilterCategory(
            name = context.getString(R.string.labels),
            options = labels.map {
                FilterOption(it.labelId, it.value)
            },
            type = SelectionType.MULTIPLE
        ),
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalFilterSheet(
    filterState: FilterState,
    labels: List<LabelModel>,
    sheetState: SheetState,
    onDismiss: () -> Unit = {},
    onApply: (
        Map<String, String>,
        Map<String, Set<String>>,
        Long?
    ) -> Unit
) {

    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.4f
    val categories = remember(labels) {
        buildFilterCategories(context, labels, filterState.selectedDate)
    }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    val sortByLabel = stringResource(R.string.sort_by)
    val labelName = stringResource(R.string.labels)
    val viewLabel = stringResource(R.string.view)

    val singleSelections = remember(filterState) {
        mutableStateMapOf<String, String>().apply {
            filterState.sort?.let { put(sortByLabel, it) }
        }
    }

    val selectedDate = remember(filterState) {
        mutableStateOf(filterState.selectedDate)
    }

    val multiSelections = remember(filterState, categories) {
        mutableStateMapOf<String, SnapshotStateList<String>>().apply {
            categories
                .filter { it.type == SelectionType.MULTIPLE }
                .forEach { category ->
                    val list = mutableStateListOf<String>()

                    if (category.name == labelName) {
                        list.addAll(filterState.selectedLabelIds)
                    }

                    put(category.name, list)
                }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(top = 8.dp)
        ) {

            Row(modifier = Modifier.weight(1f)) {

                // LEFT PANEL
                LazyColumn(
                    modifier = Modifier
                        .width(150.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    items(categories) { category ->
                        CategoryRow(
                            label = category.name,
                            isSelected = category.name == selectedCategory.name,
                            onClick = { selectedCategory = category }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface)
                )

                // RIGHT PANEL
                LazyColumn(modifier = Modifier.weight(1f)) {

                    if (selectedCategory.options.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(stringResource(R.string.no_options_available))
                            }
                        }
                    }

                    items(selectedCategory.options) { option ->

                        when (selectedCategory.type) {

                            SelectionType.SINGLE -> {
                                val selected = singleSelections[selectedCategory.name]

                                OptionRow(
                                    label = option.label,
                                    isSelected = selected == option.id,
                                    onClick = {
                                        singleSelections[selectedCategory.name] = option.id
                                    }
                                )
                            }

                            SelectionType.MULTIPLE -> {
                                val list =
                                    multiSelections[selectedCategory.name]
                                        ?: return@items

                                val isSelected = option.id in list

                                MultiOptionRow(
                                    label = option.label,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (isSelected) list.remove(option.id)
                                        else list.add(option.id)
                                    }
                                )
                            }

                            SelectionType.DATE -> {
                                DateOptionRow(
                                    date = selectedDate.value,
                                    label = option.label,
                                    onClick = {
                                        showDatePicker = true
                                    }
                                )
                            }

                            else -> {}
                        }
                    }
                }
            }

            HorizontalDivider()

            // FOOTER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        singleSelections.clear()
                        singleSelections[viewLabel] = "grid"
                        multiSelections.values.forEach { it.clear() }
                        selectedDate.value = null
                    }
                ) {
                    Text(stringResource(R.string.reset))
                }

                Button(
                    modifier = Modifier.weight(2f),
                    onClick = {
                        onApply(
                            singleSelections.toMap(),
                            multiSelections.mapValues { it.value.toSet() },
                            selectedDate.value
                        )
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.Confirm))
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (showDatePicker) {
        DatePickerModal(
            onDateSelected = { selected ->
                selectedDate.value = selected
                showDatePicker = false
            },
            onDismiss = {
                showDatePicker = false
            }
        )
    }
}
