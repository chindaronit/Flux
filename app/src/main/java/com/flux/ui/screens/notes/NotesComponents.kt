package com.flux.ui.screens.notes

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.LabelImportant
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.outlined.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.LabelImportant
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AddChart
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DataArray
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.ReportGmailerrorred
import androidx.compose.material.icons.outlined.StrikethroughS
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import com.flux.R
import com.flux.data.model.LabelModel
import com.flux.data.model.NotesModel
import com.flux.other.AudioRecorder
import com.flux.other.Constants
import com.flux.other.ExportType
import com.flux.other.HeaderNode
import com.flux.other.parseMarkdownContent
import com.flux.ui.screens.settings.ActionType
import com.flux.ui.screens.settings.CircleWrapper
import com.flux.ui.screens.settings.SettingOption
import com.flux.ui.screens.settings.shapeManager
import com.flux.ui.theme.completed
import com.flux.ui.theme.failed
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.platform.LocalConfiguration
import com.flux.ui.common.CategoryRow
import com.flux.ui.common.FilterCategory
import com.flux.ui.common.FilterOption
import com.flux.ui.common.MultiOptionRow
import com.flux.ui.common.OptionRow
import com.flux.ui.common.SelectionType
import kotlin.collections.filter

// ------------- Dialogs -------------
@Composable
fun AddLabelDialog(
    initialValue: String,
    onConfirmation: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var label by remember { mutableStateOf(initialValue) }

    AlertDialog(
        icon = {
            CircleWrapper(MaterialTheme.colorScheme.primary) {
                val icon = if (initialValue.isBlank()) Icons.Default.Add else Icons.Default.Edit
                Icon(
                    icon,
                    contentDescription = "Add/Edit Icon",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        title = {
            Text(
                text = if (initialValue.isBlank()) stringResource(R.string.Add_Label) else stringResource(
                    R.string.Edit_Label
                )
            )
        },
        text = {
            OutlinedTextField(
                value = label, onValueChange = { label = it }, singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onConfirmation(label)
                        onDismissRequest()
                    }
                ))
        },
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation(label)
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text(stringResource(R.string.Confirm)) }
        },
        dismissButton = { TextButton(onClick = { onDismissRequest() }) { Text(stringResource(R.string.Dismiss)) } }
    )
}

@Composable
fun SelectLabelDialog(
    currNoteLabelIds: List<String>,
    labels: List<LabelModel>,
    onAddLabel: () -> Unit,
    onConfirmation: (List<String>) -> Unit,
    onDismissRequest: () -> Unit
) {
    val selectedLabelIds = remember {
        mutableStateListOf<String>().apply {
            addAll(currNoteLabelIds)
        }
    }

    AlertDialog(
        icon = {
            CircleWrapper(MaterialTheme.colorScheme.primary) {
                Icon(
                    Icons.AutoMirrored.Filled.Label,
                    contentDescription = "Label Icon",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        title = { Text(text = stringResource(R.string.Select_Label)) },
        text = {
            LabelCheckBoxList(
                labels.filter { selectedLabelIds.contains(it.labelId) },
                labels,
                onChecked = { selectedLabelIds.add(it.labelId) },
                onAddLabel = {
                    onAddLabel()
                    onDismissRequest()
                },
                onUnChecked = { selectedLabelIds.remove(it.labelId) })
        },
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation(selectedLabelIds)
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.Confirm))
            }
        },
        dismissButton = { TextButton(onClick = { onDismissRequest() }) { Text(stringResource(R.string.Dismiss)) } }
    )
}

@Composable
fun LabelCheckBoxList(
    checkedLabel: List<LabelModel>,
    labels: List<LabelModel>,
    onChecked: (LabelModel) -> Unit,
    onUnChecked: (LabelModel) -> Unit,
    onAddLabel: () -> Unit
) {
    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
        item {
            Card(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .padding(vertical = 2.dp)
                    .clickable(onClick = onAddLabel),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircleWrapper(color = MaterialTheme.colorScheme.primary) {
                            Icon(
                                Icons.Default.NewLabel,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = stringResource(R.string.Add_Label),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onAddLabel) { }
                }
            }
        }
        items(labels) { label ->
            val isChecked = checkedLabel.contains(label)
            Card(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .padding(vertical = 2.dp)
                    .clickable {
                        if (isChecked) {
                            onUnChecked(label)
                        } else {
                            onChecked(label)
                        }
                    },
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    Modifier
                        .padding(vertical = 6.dp, horizontal = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircleWrapper(color = MaterialTheme.colorScheme.primary) {
                            Icon(
                                Icons.AutoMirrored.Outlined.LabelImportant,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = label.value,
                            fontWeight = FontWeight.Bold,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            modifier = Modifier.width(150.dp)
                        )
                    }
                    Checkbox(checked = isChecked, onCheckedChange = {
                        if (isChecked) {
                            onUnChecked(label)
                        } else {
                            onChecked(label)
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun ShareDialog(
    isSharing: Boolean,
    onConfirm: (ExportType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if(isSharing) stringResource(R.string.share_as) else stringResource(R.string.Export_As),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Text Export
                ExportCard(Icons.Default.TextFields, stringResource(R.string.Plain_Text)) { onConfirm(ExportType.TXT) }

                // Markdown
                ExportCard(Icons.Default.FilePresent, stringResource(R.string.Markdown)) { onConfirm(ExportType.MARKDOWN) }

                // Image
                ExportCard(Icons.Default.Image, stringResource(R.string.image)) { onConfirm(ExportType.IMAGE) }

                // HTML Export
                ExportCard(Icons.Default.Code, stringResource(R.string.html)) { onConfirm(ExportType.HTML) }
            }
        }
    }
}

@Composable
fun ExportCard(icon: ImageVector, title: String, onClick: () -> Unit){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50)),
        shape = RoundedCornerShape(50),
        onClick = { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Icon(icon, null) }
            Text( title)
        }
    }
}

@Composable
fun LinkDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (name: String, uri: String) -> Unit
) {

    var name by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }

    val linkError by remember {
        derivedStateOf {
            if (link.isNotEmpty()) {
                // Email is considered erroneous until it completely matches EMAIL_ADDRESS.
                !android.util.Patterns.WEB_URL.matcher(link).matches()
            } else {
                false
            }
        }
    }

    AlertDialog(
        title = { Text(stringResource(R.string.link)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    singleLine = true,
                    isError = linkError,
                    label = { Text(stringResource(R.string.url)) },
                    placeholder = { Text("https://www.google.com") },
                    supportingText = { if (linkError) { Text(text = "Incorrect Text") } },
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = true,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    if (!linkError) {
                        name = name.trim()
                        link = link.trim()
                        if (!link.startsWith("http://") && !link.startsWith("https://")
                            && link.startsWith("www.")
                        ) {
                            link = "https://$link"
                        }
                        onConfirm(name, link)
                        onDismissRequest()
                    }
                }
            ) {
                Text(stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    )
}

@Composable
fun TableDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (row: Int, column: Int) -> Unit
) {

    var row by remember { mutableStateOf("") }
    var column by remember { mutableStateOf("") }

    var rowError by remember { mutableStateOf(false) }
    var columnError by remember { mutableStateOf(false) }

    AlertDialog(
        title = { Text(stringResource(R.string.table)) },
        text = {
            Column {
                OutlinedTextField(
                    value = row,
                    onValueChange = {
                        row = if (it.length > 3) it.substring(0, 3) else it
                        rowError = !row.all { char -> char.isDigit() }
                    },
                    isError = rowError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    label = { Text(stringResource(R.string.row)) })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = column,
                    onValueChange = {
                        column = if (it.length > 3) it.substring(0, 3) else it
                        columnError = !column.all { char -> char.isDigit() }
                    },
                    isError = columnError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    label = { Text(stringResource(R.string.column)) }
                )
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    if (row.isBlank()) { rowError = true }
                    if (column.isBlank()) { columnError = true }
                    if (!rowError && !columnError) {
                        row = row.trim()
                        column = column.trim()
                        onConfirm(row.toInt(), column.toInt())
                        onDismissRequest()
                    }
                }
            ) {
                Text(stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(text = stringResource(id = android.R.string.cancel)) } }
    )
}

@Composable
fun ListDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (list: List<String>) -> Unit
) {

    val list = remember { mutableStateListOf("") }
    var ordered by remember { mutableStateOf(false) }

    AlertDialog(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.list))

                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        onClick = { ordered = false },
                        selected = !ordered
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                                contentDescription = "Unordered"
                            )
                        }
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        onClick = { ordered = true },
                        selected = ordered
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FormatListNumbered,
                                contentDescription = "Ordered"
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())) {
                list.forEachIndexed { index, str ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            modifier = Modifier.weight(1f),
                            value = str,
                            singleLine = true,
                            onValueChange = { list[index] = it },
                            placeholder = { Text(stringResource(R.string.item_value)) },
                            prefix = { Text(text = if (ordered) "${(index + 1)}. " else "• ") },
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedTextColor = MaterialTheme.colorScheme.primary,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        IconButton(onClick = { list.removeAt(index) }) {
                            Icon(
                                Icons.Filled.Remove,
                                contentDescription = "Remove Task"
                            )
                        }
                    }
                }

                TextButton(onClick = { list.add("") }) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SubdirectoryArrowRight, null)
                        Text(stringResource(R.string.Add_Item))
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    val result = list.mapIndexed { index, it -> if (ordered) "$index. $it" else "- $it" }
                    onConfirm(result)
                    onDismissRequest()
                }
            ) { Text(stringResource(id = android.R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(text = stringResource(id = android.R.string.cancel)) } }
    )
}

@Serializable
data class TaskItem(
    var task: String = "",
    var checked: Boolean = false
)

@Composable
fun TaskDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (taskList: List<TaskItem>) -> Unit
) {
    val taskList = remember { mutableStateListOf(TaskItem("", false)) }

    AlertDialog(
        title = { Text(stringResource(R.string.task)) },
        text = {
            Column(modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())) {
                taskList.forEachIndexed { index, taskState ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = taskState.checked,
                            onCheckedChange = { taskList[index] = taskList[index].copy(checked = it) }
                        )

                        TextField(
                            modifier = Modifier.weight(1f),
                            value = taskState.task,
                            singleLine = true,
                            onValueChange = { taskList[index] = taskList[index].copy(task = it) },
                            placeholder = { Text(stringResource(R.string.Title)) },
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedTextColor = MaterialTheme.colorScheme.primary,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        IconButton(onClick = { taskList.removeAt(index) }) {
                            Icon(
                                Icons.Filled.Remove,
                                contentDescription = "Remove Task"
                            )
                        }
                    }
                }

                TextButton(onClick = { taskList.add(TaskItem("", false)) }) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SubdirectoryArrowRight, null)
                        Text(stringResource(R.string.Add_Item))
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(taskList)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    )
}

@Composable
fun RecordAudioDialog(
    context: Context,
    recorder: AudioRecorder,
    onSave: (Uri?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var isRecording by remember { mutableStateOf(false) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    val hasRecorded = recordedFile != null
    var recordedTime by remember { mutableLongStateOf(0L) }
    val permissionRequired = stringResource(R.string.permission_required)

    // Permission launcher
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                recorder.startRecording()
                isRecording = true
            } else {
                Toast.makeText(context, permissionRequired, Toast.LENGTH_SHORT).show()
            }
        }

    Dialog(
        onDismissRequest = {
            if (isRecording) { recorder.deleteRecording() }
            onDismissRequest()
        }
    ) {
        Card(modifier = Modifier.size(320.dp)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isRecording&& !hasRecorded) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.record_audio), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))

                    Spacer(Modifier.height(32.dp))
                    // RECORD/ STOP
                    IconButton({
                        if (recorder.hasMicPermission(context)) {
                            recorder.startRecording()
                            isRecording = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }, colors = IconButtonDefaults.iconButtonColors(
                        containerColor = failed,
                        contentColor = Color.White
                    ), modifier = Modifier.size(120.dp),
                        shape = CurlyCornerShape(14.0, 6)
                    ) {
                        Icon(Icons.Default.Mic, null, modifier = Modifier.size(75.dp))
                    }
                    Spacer(Modifier.height(32.dp))
                    Text(stringResource(R.string.tap_to_start), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Light))
                }

                if(isRecording && !hasRecorded) {
                    StudioRecorderUI(recorder) {
                        recordedTime=it
                        recordedFile = recorder.stopRecording()
                        isRecording = false
                    }
                }

                if(hasRecorded){
                    Spacer(Modifier.height(8.dp))
                    Text(formatDuration(recordedTime), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(16.dp))

                    // RECORD/ STOP
                    IconButton({}, colors = IconButtonDefaults.iconButtonColors(
                        containerColor = failed,
                        contentColor = Color.White
                    ), modifier = Modifier.size(120.dp),
                        shape = CurlyCornerShape(curlCount = 8, curlAmplitude = 12.0)) {
                        Icon(Icons.Default.AudioFile, null, modifier = Modifier.size(80.dp))
                    }
                    Spacer(Modifier.height(12.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Retry
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                recorder.deleteRecording()
                                recordedFile=null
                                recorder.startRecording()
                                isRecording = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = failed,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Text(stringResource(R.string.retry), modifier = Modifier.padding(start = 8.dp))
                        }

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onSave(recordedFile?.toUri())
                                onDismissRequest()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = completed,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Verified, null)
                            Text(stringResource(R.string.save), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

class CurlyCornerShape(
    private val curlAmplitude: Double = 16.0,
    private val curlCount: Int = 12,
) : CornerBasedShape(
    topStart = ZeroCornerSize,
    topEnd = ZeroCornerSize,
    bottomEnd = ZeroCornerSize,
    bottomStart = ZeroCornerSize
) {

    /**
     * Calculates the x and y coordinates of a point on the curly circle at a given angle.
     *
     * @param centerX The x-coordinate of the center of the circle.
     * @param centerY The y-coordinate of the center of the circle.
     * @param baseRadius The base radius of the circle.
     * @param amplitude The amplitude of the sine wave.
     * @param angle The angle in radians.
     * @return A Pair containing the x and y coordinates of the point.
     */
    private fun calculateCurlyCirclePoint(
        centerX: Double,
        centerY: Double,
        baseRadius: Double,
        amplitude: Double,
        angle: Double,
    ): Pair<Double, Double> {
        // Calculate the radius with the sine wave applied.
        val radius = baseRadius + amplitude * sin(curlCount * angle)
        // Calculate x and y coordinates using polar coordinates.
        val x = centerX + radius * cos(angle)
        val y = centerY + radius * sin(angle)
        return Pair(x, y)
    }

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection
    ): Outline {
        val centerX = size.width / 2.0
        val centerY = size.height / 2.0
        val baseRadius = centerX - curlAmplitude
        val path = Path()

        // Start at the rightmost point
        val startPoint = calculateCurlyCirclePoint(centerX, centerY, baseRadius, curlAmplitude, 0.0)
        path.moveTo(startPoint.first.toFloat(), startPoint.second.toFloat())

        // Iterate through 360 degrees to draw the curly circle
        for (angleDegrees in 1..360) {
            // Convert the angle to radians.
            val angleRadians = Math.toRadians(angleDegrees.toDouble())

            // calculate the current point
            val currentPoint =
                calculateCurlyCirclePoint(centerX, centerY, baseRadius, curlAmplitude, angleRadians)

            path.lineTo(currentPoint.first.toFloat(), currentPoint.second.toFloat())
        }

        path.close()
        return Outline.Generic(path)
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CurlyCornerShape = CurlyCornerShape(
        curlAmplitude = this.curlAmplitude,
        curlCount = this.curlCount
    )
}

@Composable
fun StudioRecorderUI(
    recorder: AudioRecorder,
    onStop: (Long) -> Unit
) {
    val amplitudes = remember { MutableList(120) { 0.02f }.toMutableStateList() }
    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        var smoothedAmp = 0f

        while (true) {

            val raw = recorder.getAmplitude() / 32767f

            // Low-pass smoothing filter
            smoothedAmp = (raw * 0.2f) + (smoothedAmp * 0.8f)

            // Silence floor so waveform never vanishes
            val visualAmp = maxOf(smoothedAmp, 0.02f)

            amplitudes.add(visualAmp)

            if (amplitudes.size > 200) {
                amplitudes.removeAt(0)
            }

            elapsedMs = System.currentTimeMillis() - start

            delay(50)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TIMELINE + WAVE
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)) {
            TimelineWaveform(amplitudes, elapsedMs)

            // CENTER RED CURSOR
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .align(Alignment.Center)
                    .background(Color.Red)
            )
        }

        Spacer(Modifier.height(6.dp))

        // TIMER
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier
                .size(12.dp)
                .background(Color.Red, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(text = formatDuration(elapsedMs), fontSize = 22.sp)
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = {onStop(elapsedMs)},
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    Icons.Default.StopCircle,
                    contentDescription = null,
                    tint = failed,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun TimelineWaveform(
    amplitudes: List<Float>,
    elapsedMs: Long
) {
    val onSurface = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(160.dp)) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        val pxPerSecond = 80f
        val samplesPerSecond = 20f
        val pxPerSample = pxPerSecond / samplesPerSecond

        val secondsElapsed = elapsedMs / 1000f

        // ==============================
        // WAVEFORM (PAST → LEFT)
        // ==============================

        amplitudes.forEachIndexed { index, rawAmp ->

            val x = centerX - (amplitudes.size - index) * (pxPerSample * 1.6f)

            if (x >= 0f) {

                // Boost amplitude visually
                val amp = rawAmp.coerceIn(0f, 1f)

                val visualHeight =
                    (amp * height * 1.2f).coerceAtMost(height * 0.95f)

                val halfBar = visualHeight / 2f

                // Dynamic alpha for clarity
                val alpha =
                    0.3f + (amp * 0.7f)

                drawLine(
                    color = onSurface.copy(alpha = alpha),
                    start = Offset(x, centerY - halfBar),
                    end = Offset(x, centerY + halfBar),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }

        // ==============================
        // FUTURE PLACEHOLDER (RIGHT DOTS)
        // ==============================

        var futureX = centerX + pxPerSample

        while (futureX < width) {

            drawLine(
                color = Color.Gray.copy(alpha = 0.4f),
                start = Offset(futureX, centerY - 4f),
                end = Offset(futureX, centerY + 4f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )

            futureX += pxPerSample * 2
        }

        // ==============================
        // TIMELINE TICKS + LABELS
        // ==============================

        val offsetPx = (secondsElapsed * pxPerSecond) % pxPerSecond

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
        }

        // Prevent duplicate labels
        val drawnSeconds = mutableSetOf<Int>()

        // -------- PAST (LEFT) --------

        var pastTickIndex = 0
        var tickX = centerX - offsetPx

        while (tickX > 0f) {
            val timeAtTick = secondsElapsed - pastTickIndex
            val secondInt = timeAtTick.toInt()

            // Tick line
            drawLine(
                color = Color.Gray,
                start = Offset(tickX, 0f),
                end = Offset(tickX, 24f),
                strokeWidth = 2f
            )

            // Draw label only if:
            // 1. Time exists (>= 0)
            // 2. Even second
            // 3. Not already drawn
            if (secondInt >= 0 &&
                secondInt % 2 == 0 &&
                drawnSeconds.add(secondInt)
            ) {
                drawContext.canvas.nativeCanvas.drawText(
                    formatTimelineLabel(secondInt),
                    tickX,
                    48f,
                    paint
                )
            }

            tickX -= pxPerSecond
            pastTickIndex++
        }

        // -------- FUTURE (RIGHT) --------

        var futureTickIndex = 1
        var futureTickX =
            centerX + (pxPerSecond - offsetPx)

        while (futureTickX < width) {

            val futureSecond =
                (secondsElapsed + futureTickIndex).toInt()

            drawLine(
                color = onSurface,
                start = Offset(futureTickX, 0f),
                end = Offset(futureTickX, 24f),
                strokeWidth = 2f
            )

            if (futureSecond % 2 == 0 &&
                drawnSeconds.add(futureSecond)
            ) {
                drawContext.canvas.nativeCanvas.drawText(
                    formatTimelineLabel(futureSecond),
                    futureTickX,
                    48f,
                    paint
                )
            }

            futureTickX += pxPerSecond
            futureTickIndex++
        }

        // ==============================
        // CURSOR
        // ==============================

        drawLine(
            color = failed,
            start = Offset(centerX, 0f),
            end = Offset(centerX, height),
            strokeWidth = 3f
        )
    }
}

fun formatTimelineLabel(seconds: Int): String {

    val min = seconds / 60
    val sec = seconds % 60

    return "%02d:%02d".format(min, sec)
}

fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60

    return "%02d:%02d".format(min, sec)
}

// ------------- Bottom Sheet -------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlineBottomSheet(
    isVisible: Boolean,
    outline: HeaderNode,
    sheetState: SheetState,
    onHeaderClick: (IntRange) -> Unit,
    onDismiss: () -> Unit
) {

    var isAllExpanded by rememberSaveable { mutableStateOf(true) }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            LazyColumn(Modifier
                .fillMaxWidth()
                .heightIn(300.dp)) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.Outline),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = { isAllExpanded = !isAllExpanded }
                        ) {
                            Icon(
                                imageVector = if (isAllExpanded) Icons.Outlined.UnfoldLess
                                else Icons.Outlined.UnfoldMore,
                                contentDescription = "fold",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider()
                }
                items(outline.children) { header ->
                    HeaderItem(
                        header = header,
                        depth = 0,
                        onHeaderClick = onHeaderClick,
                        parentExpanded = isAllExpanded
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderItem(
    header: HeaderNode,
    depth: Int,
    parentExpanded: Boolean,
    onHeaderClick: (IntRange) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(parentExpanded) { expanded = parentExpanded }
    Row(
        modifier = Modifier
            .padding(start = (depth * 8).dp)
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .clickable {
                onHeaderClick(header.range)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (header.children.isNotEmpty()) {
            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = {
                    if (header.children.isNotEmpty()) {
                        expanded = !expanded
                    }
                }
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropDown
                    else Icons.AutoMirrored.Filled.ArrowRight,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = null
                )
            }
        } else {
            Spacer(modifier = Modifier.width(32.dp))
        }

        Text(
            text = header.title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
    }

    if (expanded) {
        header.children.forEach { child ->
            HeaderItem(
                header = child,
                depth = depth + 1,
                onHeaderClick = onHeaderClick,
                parentExpanded = parentExpanded
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesInfoBottomSheet(
    words: Int,
    lines: Int,
    wordsWithoutPunctuations: Int,
    paragraph: Int,
    characters: Int,
    lastEdited: String,
    isVisible: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                item {
                    SettingOption(
                        radius = shapeManager(isFirst = true, radius = 32),
                        icon = Icons.Default.Edit,
                        title = stringResource(R.string.Last_Edited),
                        description = lastEdited,
                        actionType = ActionType.None
                    )
                }

                item {
                    SettingOption(
                        radius = shapeManager(radius = 32),
                        icon = Icons.Default.Numbers,
                        title = stringResource(R.string.Word_Count),
                        description = words.toString(),
                        actionType = ActionType.None
                    )
                }

                item {
                    SettingOption(
                        radius = shapeManager(radius = 32),
                        icon = Icons.Default.FormatQuote,
                        title = stringResource(R.string.words_excluding_punctuations),
                        description = wordsWithoutPunctuations.toString(),
                        actionType = ActionType.None
                    )
                }

                item {
                    SettingOption(
                        radius = shapeManager(radius = 32),
                        icon = Icons.Default.Abc,
                        title = stringResource(R.string.Character_Count),
                        description = characters.toString(),
                        actionType = ActionType.None
                    )
                }

                item {
                    SettingOption(
                        radius = shapeManager(radius = 32),
                        icon = Icons.AutoMirrored.Filled.List,
                        title = stringResource(R.string.lines),
                        description = lines.toString(),
                        actionType = ActionType.None
                    )
                }

                item {
                    SettingOption(
                        radius = shapeManager(radius = 32, isLast = true),
                        icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                        title = stringResource(R.string.paragraph),
                        description = paragraph.toString(),
                        actionType = ActionType.None
                    )
                }
            }
        }
    }
}

fun buildFilterCategories(labels: List<LabelModel>): List<FilterCategory> {
    return listOf(
        FilterCategory(
            name = "Sort By",
            options = listOf(
                FilterOption("latest", "Latest"),
                FilterOption("oldest", "Oldest")
            ),
            type = SelectionType.SINGLE
        ),
        FilterCategory(
            name = "Labels",
            options = labels.map {
                FilterOption(it.labelId, it.value)
            },
            type = SelectionType.MULTIPLE
        ),
        FilterCategory(
            name = "Pinned",
            options = listOf(
                FilterOption("pinned", "Pinned"),
                FilterOption("unpinned", "Unpinned")
            ),
            type = SelectionType.SINGLE
        ),
        FilterCategory(
            name = "View",
            options = listOf(
                FilterOption("list", "List View"),
                FilterOption("grid", "Grid View")
            ),
            type = SelectionType.SINGLE
        )
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesFilterSheet(
    filterState: FilterState,
    labels: List<LabelModel>,
    sheetState: SheetState,
    onDismiss: () -> Unit = {},
    onApply: (Map<String, String>, Map<String, Set<String>>) -> Unit
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.4f
    val categories = remember(labels) { buildFilterCategories(labels) }
    var selectedCategory by remember { mutableStateOf(categories.first()) }

    val singleSelections = remember(filterState) {
        mutableStateMapOf<String, String>().apply {
            filterState.sort?.let { put("Sort By", it) }
            filterState.view?.let { put("View", it) }
            filterState.pinned?.let { put("Pinned", it) }
        }
    }

    val multiSelections = remember(filterState, categories) {
        mutableStateMapOf<String, SnapshotStateList<String>>().apply {
            categories
                .filter { it.type == SelectionType.MULTIPLE }
                .forEach { category ->
                    val list = mutableStateListOf<String>()

                    if (category.name == "Labels") {
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
                                Text("No options available")
                            }
                        }
                    }

                    items(selectedCategory.options) { option ->

                        when (selectedCategory.type) {

                            SelectionType.SINGLE -> {
                                val selected =
                                    singleSelections[selectedCategory.name]

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
                        singleSelections["View"] = "grid"
                        multiSelections.values.forEach { it.clear() }
                    }
                ) {
                    Text("Reset")
                }

                Button(
                    modifier = Modifier.weight(2f),
                    onClick = {
                        onApply(
                            singleSelections.toMap(),
                            multiSelections.mapValues { it.value.toSet() }
                        )
                        onDismiss()
                    }
                ) {
                    Text("Apply")
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

// ------------- Card -------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesPreviewCard(
    modifier: Modifier = Modifier,
    radius: Int,
    isSelected: Boolean,
    note: NotesModel,
    labels: List<String>,
    onClick: (String) -> Unit,
    onLongPressed: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)),
        modifier = modifier
            .clip(shapeManager(isBoth = true, radius = radius / 2))
            .combinedClickable(
                onClick = { onClick(note.notesId) },
                onLongClick = onLongPressed
            ),
        shape = shapeManager(isBoth = true, radius = radius / 2),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .alpha(0.75f)
                    .padding(horizontal = 12.dp)
                    .padding(top = 12.dp)
            )

            Text(
                text = parseMarkdownContent(note.description),
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 12,
                modifier = Modifier
                    .alpha(0.9f)
                    .padding(horizontal = 12.dp)
            )

            val maxVisibleLabels = 2
            val visibleLabels = labels.take(maxVisibleLabels)
            val extraCount = labels.size - maxVisibleLabels

            if (visibleLabels.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    visibleLabels.forEach { label ->
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
                                    label,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (extraCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text(
                                text = "+$extraCount",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomIconButton(
    enabled: Boolean = true,
    imageVector: ImageVector? = null,
    painter: Int? = null,
    contentDescription: String?=null,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
    ) {
        if (imageVector != null) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription
            )
        } else {
            Icon(
                painter = painterResource(id = painter!!),
                contentDescription = contentDescription
            )
        }
    }
}

@Composable
fun MarkdownEditorRow(
    canUndo: Boolean,
    canRedo: Boolean,
    onEdit: (String) -> Unit,
    onTableButtonClick: () -> Unit,
    onListButtonClick: () -> Unit,
    onTaskButtonClick: () -> Unit,
    onLinkButtonClick: () -> Unit,
    onImageButtonClick: () -> Unit,
    onAudioButtonClick: () -> Unit,
    onRecordAudioClick: () -> Unit,
    onVideoButtonClick: () -> Unit
) {

    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var isAlertExpanded by rememberSaveable { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .navigationBarsPadding()
            .height(48.dp)
            .border(
                BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(50)
            )
            .clip(RoundedCornerShape(50))
            .horizontalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomIconButton(
            enabled = canUndo,
            painter = R.drawable.undo,
            contentDescription = "Undo"
        ) {
            onEdit(Constants.Editor.UNDO)
        }

        CustomIconButton(
            enabled = canRedo,
            painter = R.drawable.redo,
            contentDescription = "Redo"
        ) {
            onEdit(Constants.Editor.REDO)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.Title,
            contentDescription = "Heading Level"
        ) {
            isExpanded = !isExpanded
        }

        AnimatedVisibility(visible = isExpanded) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {

                CustomIconButton(
                    painter = R.drawable.format_h1,
                    contentDescription = "H1"
                ) {
                    onEdit(Constants.Editor.H1)
                }

                CustomIconButton(
                    painter = R.drawable.format_h2,
                    contentDescription = "H2"
                ) {
                    onEdit(Constants.Editor.H2)
                }

                CustomIconButton(
                    painter = R.drawable.format_h3,
                    contentDescription = "H3"
                ) {
                    onEdit(Constants.Editor.H3)
                }

                CustomIconButton(
                    painter = R.drawable.format_h4,
                    contentDescription = "H4"
                ) {
                    onEdit(Constants.Editor.H4)
                }

                CustomIconButton(
                    painter = R.drawable.format_h5,
                    contentDescription = "H5"
                ) {
                    onEdit(Constants.Editor.H5)
                }

                CustomIconButton(
                    painter = R.drawable.format_h6,
                    contentDescription = "H6"
                ) {
                    onEdit(Constants.Editor.H6)
                }
            }
        }

        CustomIconButton(
            imageVector = Icons.Outlined.FormatBold,
            contentDescription = "Bold"
        ) {
            onEdit(Constants.Editor.BOLD)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.FormatItalic,
            contentDescription = "Italic"
        ) {
            onEdit(Constants.Editor.ITALIC)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.FormatUnderlined,
            contentDescription = "Underline"
        ) {
            onEdit(Constants.Editor.UNDERLINE)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.StrikethroughS,
            contentDescription = "Strike Through"
        ) {
            onEdit(Constants.Editor.STRIKETHROUGH)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.FormatPaint,
            contentDescription = "Mark"
        ) {
            onEdit(Constants.Editor.MARK)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.Code,
            contentDescription = "Code"
        ) {
            onEdit(Constants.Editor.INLINE_CODE)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.DataArray,
            contentDescription = "Brackets"
        ) {
            onEdit(Constants.Editor.INLINE_BRACKETS)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.DataObject,
            contentDescription = "Braces"
        ) {
            onEdit(Constants.Editor.INLINE_BRACES)
        }

        CustomIconButton(
            imageVector = Icons.AutoMirrored.Outlined.FormatIndentIncrease,
            contentDescription = "Tab"
        ) {
            onEdit(Constants.Editor.TAB)
        }

        CustomIconButton(
            imageVector = Icons.AutoMirrored.Outlined.FormatIndentDecrease,
            contentDescription = "unTab"
        ) {
            onEdit(Constants.Editor.UN_TAB)
        }

        CustomIconButton(
            painter = R.drawable.function,
            contentDescription = "Math"
        ) {
            onEdit(Constants.Editor.INLINE_MATH)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.FormatQuote,
            contentDescription = "Quote"
        ) {
            onEdit(Constants.Editor.QUOTE)
        }

        CustomIconButton(
            imageVector = Icons.AutoMirrored.Outlined.Label,
            contentDescription = "Alert",
        ) {
            isAlertExpanded = !isAlertExpanded
        }

        AnimatedVisibility(visible = isAlertExpanded) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                CustomIconButton(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Note Alert",
                ) {
                    onEdit(Constants.Editor.NOTE)
                }

                CustomIconButton(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = "Tip Alert",
                ) {
                    onEdit(Constants.Editor.TIP)
                }

                CustomIconButton(
                    imageVector = Icons.Outlined.Feedback,
                    contentDescription = "Important Alert",
                ) {
                    onEdit(Constants.Editor.IMPORTANT)

                }
                CustomIconButton(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = "Warning Alert",
                ) {
                    onEdit(Constants.Editor.WARNING)
                }

                CustomIconButton(
                    imageVector = Icons.Outlined.ReportGmailerrorred,
                    contentDescription = "Caution Alert",
                ) {
                    onEdit(Constants.Editor.CAUTION)
                }
            }
        }

        CustomIconButton(
            imageVector = Icons.Outlined.HorizontalRule,
            contentDescription = "Horizontal Rule",
        ) {
            onEdit(Constants.Editor.RULE)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.TableChart,
            contentDescription = "Table",
            onClick = onTableButtonClick
        )

        CustomIconButton(
            imageVector = Icons.Outlined.AddChart,
            contentDescription = "Mermaid Diagram",
        ) {
            onEdit(Constants.Editor.DIAGRAM)
        }

        CustomIconButton(
            imageVector = Icons.AutoMirrored.Outlined.List,
            contentDescription = "List",
            onClick = onListButtonClick
        )

        CustomIconButton(
            imageVector = Icons.Outlined.CheckBox,
            contentDescription = "Task List",
            onClick = onTaskButtonClick
        )

        CustomIconButton(
            imageVector = Icons.Outlined.Link,
            contentDescription = "Link",
            onClick = onLinkButtonClick
        )

        CustomIconButton(
            imageVector = Icons.Outlined.Mic,
            contentDescription = "Audio",
            onClick = onRecordAudioClick
        )

        CustomIconButton(
            imageVector = Icons.Outlined.AudioFile,
            contentDescription = "Audio",
            onClick = onAudioButtonClick
        )

        CustomIconButton(
            imageVector = Icons.Outlined.VideoFile,
            contentDescription = "Video",
            onClick = onVideoButtonClick
        )

        CustomIconButton(
            imageVector = Icons.Outlined.Image,
            contentDescription = "Image",
            onClick = onImageButtonClick
        )
    }
}