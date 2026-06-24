package com.flux.ui.common

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flux.R
import com.flux.data.model.RecurrenceRule
import com.flux.other.icons
import com.flux.other.workspaceIconList
import com.flux.ui.screens.events.formatCustom
import com.flux.ui.screens.events.formatMonthly
import com.flux.ui.screens.events.formatOnce
import com.flux.ui.screens.events.formatYearly
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeIconSheet(
    isVisible: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                items(workspaceIconList) { item ->
                    Text(
                        item.title,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        item.icons.forEach { index ->
                            IconButton(
                                {
                                    onConfirm(index)
                                    onDismiss()
                                }
                            ) {
                                Icon(icons[index], null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecurrenceRule.label(): String = when (this) {
    is RecurrenceRule.Once  -> stringResource(R.string.Once)
    is RecurrenceRule.Custom   -> stringResource(R.string.Custom)
    is RecurrenceRule.Weekly  -> stringResource(R.string.Weekly)
    is RecurrenceRule.Monthly -> stringResource(R.string.Monthly)
    is RecurrenceRule.Yearly  -> stringResource(R.string.Yearly)
    else -> ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceBottomSheet(
    isVisible: Boolean,
    sheetState: SheetState,
    startDateTime: Long,
    onDismiss: () -> Unit,
    currentRule: RecurrenceRule,
    onRuleChange: (RecurrenceRule, Long) -> Unit
) {
    if (!isVisible) return
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateTime by remember { mutableLongStateOf(startDateTime) }
    var tempRule by remember(currentRule) { mutableStateOf(currentRule) }

    val options = listOf(
        RecurrenceRule.Once,
        RecurrenceRule.Weekly(),
        RecurrenceRule.Monthly,
        RecurrenceRule.Yearly,
        RecurrenceRule.Custom()
    )

    if (showDatePicker) {
        DatePickerModal(onDateSelected = { newDateMillis ->
            if (newDateMillis != null) {
                val timeOfDay = selectedDateTime % DateUtils.DAY_IN_MILLIS
                selectedDateTime = newDateMillis + timeOfDay
            }
        }, onDismiss = { showDatePicker = false })
    }

    ModalBottomSheet(
        modifier = Modifier.heightIn(min = 300.dp),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        LazyColumn(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)) {
                item {
                    Row(
                        Modifier.padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Repeat, null)
                        Text(
                            stringResource(R.string.repeat),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(options.size) { index ->
                            val option = options[index]
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = option::class == tempRule::class,
                                    onClick = { tempRule = option }
                                )
                                Text(option.label(), modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }

                item {
                    when (val rule = tempRule) {
                        is RecurrenceRule.Once -> {
                            Row(Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatOnce(selectedDateTime))
                                IconButton({ showDatePicker=true }, colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )) {
                                    Icon(Icons.Default.Edit, null)
                                }
                            }
                        }

                        is RecurrenceRule.Custom -> {
                            OutlinedTextField(
                                value = rule.everyXDays.toString(),
                                onValueChange = { new -> new.toIntOrNull()?.let { tempRule = rule.copy(everyXDays = it) } },
                                modifier = Modifier.padding(start = 8.dp),
                                label = { Text(formatCustom(rule)) },
                                singleLine = true
                            )
                        }

                        is RecurrenceRule.Weekly -> {
                            val weekdays = listOf(
                                stringResource(R.string.monday_short),
                                stringResource(R.string.tuesday_short),
                                stringResource(R.string.wednesday_short),
                                stringResource(R.string.thursday_short),
                                stringResource(R.string.friday_short),
                                stringResource(R.string.saturday_short),
                                stringResource(R.string.sunday_short)
                            )

                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                maxItemsInEachRow = 7
                            ) {
                                weekdays.forEachIndexed { index, day ->
                                    val isSelected = index in rule.daysOfWeek

                                    Card(
                                        onClick = {},
                                        colors = CardDefaults.cardColors(
                                            containerColor =
                                                if (isSelected)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),

                                            contentColor =
                                                if (isSelected)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Text(
                                            text = day,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodyMedium,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 8.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        is RecurrenceRule.Monthly -> {
                            Row(Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatMonthly(selectedDateTime))
                                IconButton({ showDatePicker=true }, colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
                                )) {
                                    Icon(Icons.Default.Edit, null)
                                }
                            }
                        }

                        is RecurrenceRule.Yearly -> {
                            Row(Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatYearly(selectedDateTime))
                                IconButton({ showDatePicker=true }, colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
                                )) {
                                    Icon(Icons.Default.Edit, null)
                                }
                            }
                        }

                        else -> {}
                    }
                }

                // Buttons
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = onDismiss) {
                            Text(stringResource(R.string.Dismiss))
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = {
                                onRuleChange(tempRule, adjustStartDate(tempRule, selectedDateTime))
                                onDismiss()
                            }
                        ) {
                            Text(stringResource(R.string.Confirm))
                        }
                    }
                }
            }
        }
    }

    fun adjustStartDate(rule: RecurrenceRule, startDateTime: Long): Long {
        return when (rule) {
            is RecurrenceRule.Once -> startDateTime
            is RecurrenceRule.Monthly -> startDateTime
            is RecurrenceRule.Yearly -> startDateTime
            is RecurrenceRule.Custom -> startDateTime
            is RecurrenceRule.Weekly -> {
                val cal = Calendar.getInstance().apply { timeInMillis = startDateTime }

                // Calendar days: Sunday = 1, Monday = 2, ... Saturday = 7
                val today = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // shift so Monday=0, Sunday=6

                if (today in rule.daysOfWeek) {
                    startDateTime
                } else {
                    // find next closest match
                    var offset = 1
                    while (true) {
                        val nextDay = (today + offset) % 7
                        if (nextDay in rule.daysOfWeek) {
                            cal.add(Calendar.DAY_OF_YEAR, offset)
                            return cal.timeInMillis
                        }
                        offset++
                    }
                }
            }

            else -> {}
        } as Long
    }
