package com.flux.ui.screens.progressBoard

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.ProgressBoardModel
import com.flux.other.icons
import com.flux.ui.common.ChangeIconSheet
import com.flux.ui.common.DateOnlyPickerModal
import com.flux.ui.common.DeleteAlert
import com.flux.ui.common.DiscardChangesDialog
import com.flux.ui.common.convertMillisToDate
import com.flux.ui.events.ProgressBoardEvents
import com.flux.ui.screens.events.getTextFieldColors
import com.flux.ui.theme.completed
import com.flux.ui.theme.failed
import com.flux.ui.theme.pending

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProgressItem(
    navController: NavController,
    boardItem: ProgressBoardModel,
    onEvent: (ProgressBoardEvents) -> Unit
){
    val context = LocalContext.current
    val originalItem = remember { boardItem }
    val focusRequesterDesc = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedStatus by remember(boardItem) { mutableIntStateOf(boardItem.status) }
    var startDate by remember(boardItem) { mutableLongStateOf(boardItem.startDate) }
    var endDate by remember(boardItem) { mutableLongStateOf(boardItem.endDate) }
    var showDateSelector by remember { mutableStateOf(false) }
    var isSelectingStartDate by remember { mutableStateOf(true) }
    var title by remember(boardItem) { mutableStateOf(boardItem.title) }
    var description by remember(boardItem) { mutableStateOf(boardItem.description ) }
    var isChangeIcon by remember { mutableStateOf(false) }
    val iconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newIcon by remember(boardItem) { mutableIntStateOf(boardItem.icon) }
    val startDateString = stringResource(R.string.start_date_after_target_error)
    val targetDateString = stringResource(R.string.target_date_before_start_error)
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isNew = boardItem.title.isEmpty()
    val topBarTitle = if (isNew) {
        "Add New Item"
    } else {
        "Edit Item"
    }

    ChangeIconSheet (isChangeIcon, iconSheetState, { isChangeIcon=false }) {
        newIcon=it
        isChangeIcon=false
    }

    fun saveIfPossible(): Boolean {
        val candidate = originalItem.copy(
            title = title,
            description = description,
            startDate = startDate,
            endDate = endDate,
            icon = newIcon,
            status = selectedStatus
        )
        val hasContent = candidate != originalItem
        if (!hasContent) { return true }

        if (title.isBlank()) {
            showDiscardDialog = true
            return false
        }

        onEvent(ProgressBoardEvents.UpsertProgressItem(candidate))
        return true
    }

    BackHandler { if (saveIfPossible()) { navController.popBackStack() } }

    if(showDeleteDialog){
        DeleteAlert({
            showDeleteDialog=false
        }, {
            navController.popBackStack()
            onEvent(ProgressBoardEvents.DeleteProgressItem(boardItem))
            showDeleteDialog=false
        })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.surfaceContainerLow),
                title = { Text(topBarTitle) },
                navigationIcon = {
                    IconButton({ if (saveIfPossible()) { navController.popBackStack() } }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton({ showDeleteDialog=true }) {
                        Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(innerPadding)) {
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
        }
    }

    if(showDateSelector){
        DateOnlyPickerModal(
            initialSelectedDateMillis = if (isSelectingStartDate) {
                if (startDate == -1L) System.currentTimeMillis() else startDate
            } else {
                if (endDate == -1L) System.currentTimeMillis() else endDate
            },
            onDateSelected = { picked ->
                val selectedDate = picked ?: -1L

                if (isSelectingStartDate) {
                    if (
                        endDate != -1L &&
                        selectedDate != -1L &&
                        selectedDate > endDate
                    ) {
                        Toast.makeText(
                            context,
                            startDateString,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        startDate = selectedDate
                    }
                } else {
                    if (
                        startDate != -1L &&
                        selectedDate != -1L &&
                        selectedDate < startDate
                    ) {
                        Toast.makeText(
                            context,
                            targetDateString,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        endDate = selectedDate
                    }
                }
            },
            onDismiss = {
                showDateSelector = false
            }
        )
    }

    if (showDiscardDialog) {
        DiscardChangesDialog({
            showDiscardDialog = false
            navController.popBackStack()
        }) {
            showDiscardDialog=false
        }
    }
}