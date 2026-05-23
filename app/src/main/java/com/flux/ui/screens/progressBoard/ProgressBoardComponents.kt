package com.flux.ui.screens.progressBoard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.flux.R
import com.flux.data.model.ProgressBoardModel
import com.flux.other.icons
import com.flux.ui.common.ChangeIconSheet
import com.flux.ui.common.DatePickerModal
import com.flux.ui.common.convertMillisToDate
import com.flux.ui.screens.events.getTextFieldColors
import com.flux.ui.screens.settings.shapeManager
import com.flux.ui.theme.completed
import com.flux.ui.theme.failed
import com.flux.ui.theme.pending

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBoardItemSheet(
    isVisible: Boolean,
    sheetState: SheetState,
    progressBoardItem: ProgressBoardModel,
    onDismiss: () -> Unit,
    onConfirm: (ProgressBoardModel) -> Unit,
    onDelete: (ProgressBoardModel) -> Unit
) {
    val focusRequesterDesc = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedStatus by remember(progressBoardItem) { mutableIntStateOf(progressBoardItem.status) }
    var startDate by remember(progressBoardItem) { mutableLongStateOf(progressBoardItem.startDate) }
    var endDate by remember(progressBoardItem) { mutableLongStateOf(progressBoardItem.endDate) }
    var showDateSelector by remember { mutableStateOf(false) }
    var isSelectingStartDate by remember { mutableStateOf(true) }
    var title by remember(progressBoardItem) { mutableStateOf(progressBoardItem.title) }
    var description by remember(progressBoardItem) { mutableStateOf(progressBoardItem.description ) }
    var isChangeIcon by remember { mutableStateOf(false) }
    val iconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newIcon by remember(progressBoardItem) { mutableIntStateOf(progressBoardItem.icon) }

    if(showDateSelector){
        DatePickerModal({
            if (isSelectingStartDate) startDate=it?:-1L
            else endDate=it?:-1L
        }) {
            showDateSelector=false
        }
    }

    ChangeIconSheet (isChangeIcon, iconSheetState, { isChangeIcon=false }) {
        newIcon=it
        isChangeIcon=false
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                IconButton(
                    { isChangeIcon=true },
                    modifier = Modifier.align(Alignment.CenterHorizontally).size(56.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(icons[newIcon], null, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.Title)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    colors = getTextFieldColors(),
                    keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusRequesterDesc.requestFocus() })
                )

                TextField(
                    value = description,
                    onValueChange = { description=it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 16.dp)
                        .focusRequester(focusRequesterDesc),
                    placeholder = { Text(stringResource(R.string.Description)) },
                    singleLine = true,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                    colors = getTextFieldColors(),
                    keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                )

                LazyRow(Modifier.fillMaxWidth().padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        BoardStatusItem(selectedStatus==0, stringResource(R.string.not_started), failed) {
                            selectedStatus=0
                        }
                    }
                    item {
                        BoardStatusItem(selectedStatus==1, stringResource(R.string.in_progress), pending){
                            selectedStatus=1
                        }
                    }
                    item {
                        BoardStatusItem(selectedStatus==2, stringResource(R.string.Completed), completed){
                            selectedStatus=2
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoardTimeCard(Icons.Default.Timelapse, stringResource(R.string.start)) {
                        showDateSelector=true
                        isSelectingStartDate=true
                    }
                    Spacer(Modifier.width(2.dp))
                    Text(
                        if(startDate==-1L) stringResource(R.string.empty) else convertMillisToDate(startDate),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .clickable{
                                showDateSelector=true
                                isSelectingStartDate=true
                            }
                            .padding(vertical = 4.dp, horizontal = 8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoardTimeCard(Icons.Default.Flag, stringResource(R.string.target)) {
                        showDateSelector=true
                        isSelectingStartDate=false
                    }
                    Text(
                        if(endDate==-1L) stringResource(R.string.empty) else convertMillisToDate(endDate),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .clickable{
                                showDateSelector=true
                                isSelectingStartDate=false
                            }
                            .padding(vertical = 4.dp, horizontal = 8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if(progressBoardItem.title.isNotBlank()){
                        FilledTonalButton(
                            onClick = {
                                keyboardController?.hide()
                                onDismiss()
                                onDelete(progressBoardItem)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        enabled = title.isNotBlank(),
                        onClick = {
                            keyboardController?.hide()
                            onConfirm(
                                progressBoardItem.copy(
                                    title= title,
                                    description = description,
                                    icon = newIcon,
                                    startDate = startDate,
                                    endDate = endDate,
                                    status = selectedStatus
                                )
                            )
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

@Composable
fun BoardStatusItem(isSelected: Boolean, status: String, color: Color, onClick: () -> Unit){
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
            containerColor = if(isSelected) color.copy(0.5f) else color.copy(0.1f)
        )
    ) {
        Row(
            Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(12.dp)
                    .clip(RoundedCornerShape(50))
                    .then(
                        if (isSelected) {
                            Modifier.background(color)
                        } else {
                            Modifier.border(2.dp, color, RoundedCornerShape(50))
                        }
                    )
            )
            Text(
                status,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@Composable
fun BoardTimeCard(icon: ImageVector, title: String, onClick: () -> Unit){
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            Modifier.padding(vertical = 2.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp).alpha(0.75f))
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(4.dp).alpha(0.8f)
            )
        }
    }
}

@Composable
fun BoardContainer(
    containerColor: Color,
    status: String,
    radius: Int,
    items: List<ProgressBoardModel>,
    onClick: (ProgressBoardModel) -> Unit
){
    Card(
        modifier = Modifier.width(300.dp),
        shape = shapeManager(radius = radius * 2),
        onClick = { },
        colors = CardDefaults.cardColors(containerColor = containerColor.copy(0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BoardStatusItem(true, status, containerColor){}
            Spacer(Modifier.height(4.dp))
            items.forEach { item->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = shapeManager(radius = radius * 2),
                    onClick = { onClick(item) },
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icons[item.icon],
                            null,
                            modifier = Modifier.alpha(0.8f)
                        )
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(item.title, style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (item.startDate != -1L) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(Icons.Default.Timelapse, null, modifier = Modifier
                                            .size(16.dp)
                                            .alpha(0.75f))
                                        Text(
                                            convertMillisToDate(item.startDate),
                                            modifier = Modifier.alpha(0.75f),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                if (item.endDate != -1L) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(Icons.Default.Flag, null, modifier = Modifier
                                            .size(16.dp)
                                            .alpha(0.75f))
                                        Text(
                                            convertMillisToDate(item.endDate),
                                            modifier = Modifier.alpha(0.75f),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
