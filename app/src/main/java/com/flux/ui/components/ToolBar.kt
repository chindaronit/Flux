package com.flux.ui.components

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.navigation.NavRoutes
import com.flux.other.canScheduleReminder
import com.flux.other.isNotificationPermissionGranted
import com.flux.other.openAppNotificationSettings
import com.flux.other.requestExactAlarmPermission
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun SpacesToolBar(
    title: String,
    icon: ImageVector,
    isEmptyWorkspace: Boolean,
    onMainClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
    ) {
        if (isEmptyWorkspace) {
            Row(
                modifier = Modifier
                    .clickable { onEditClick() }
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.Add_Spaces_Content_Desc),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.Add_Space),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onMainClick() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = stringResource(R.string.Space_Content_Desc),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.Space_Content_Desc),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                VerticalDivider(Modifier.fillMaxHeight())

                // Right section (Edit icon)
                Row(
                    modifier = Modifier.clickable { onEditClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Edit,
                        modifier = Modifier.padding(6.dp),
                        contentDescription = stringResource(R.string.Edit_Space_Content_Desc),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun TodoToolBar(navController: NavController, workspaceId: String) {
    IconButton({ navController.navigate(NavRoutes.TodoDetail.withArgs(workspaceId, "")) }) {
        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun JournalToolBar(navController: NavController, workspaceId: String) {
    IconButton({ navController.navigate(NavRoutes.EditJournal.withArgs(workspaceId, "")) }) {
        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU, Build.VERSION_CODES.S)
@Composable
fun EventToolBar(navController: NavController, workspaceId: String, context: Context, selectedDate: Long, isMonthlyView: Boolean, onClick: (Boolean) -> Unit) {
    Row {
        IconButton({ onClick(!isMonthlyView) }) {
            Icon(
                if (isMonthlyView) Icons.Default.CalendarViewDay else Icons.Default.CalendarViewMonth,
                null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        IconButton({
            if (!canScheduleReminder(context)) {
                Toast.makeText(
                    context,
                    context.getText(R.string.Reminder_Permission),
                    Toast.LENGTH_SHORT
                ).show()
                requestExactAlarmPermission(context)
            }
            if (!isNotificationPermissionGranted(context)) {
                Toast.makeText(
                    context,
                    context.getText(R.string.Notification_Permission),
                    Toast.LENGTH_SHORT
                ).show()
                openAppNotificationSettings(context)
            }
            if (canScheduleReminder(context) && isNotificationPermissionGranted(context)) {
                val localDate = LocalDate.ofEpochDay(selectedDate)
                val currentTime = LocalTime.now()
                val zonedDateTime = ZonedDateTime.of(localDate, currentTime, ZoneId.systemDefault())
                val selectedDateMillis = zonedDateTime.toInstant().toEpochMilli()

                navController.navigate(NavRoutes.NewEvent.withArgs(workspaceId, "", selectedDateMillis))
            }
        }) { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU, Build.VERSION_CODES.S)
@Composable
fun HabitToolBar(context: Context, onAddClick: () -> Unit) {
    IconButton({
        if (!canScheduleReminder(context)) {
            Toast.makeText(
                context,
                context.getText(R.string.Reminder_Permission),
                Toast.LENGTH_SHORT
            ).show()
            requestExactAlarmPermission(context)
        }
        if (!isNotificationPermissionGranted(context)) {
            Toast.makeText(
                context,
                context.getText(R.string.Notification_Permission),
                Toast.LENGTH_SHORT
            ).show()
            openAppNotificationSettings(context)
        }
        if (canScheduleReminder(context) && isNotificationPermissionGranted(context)) {
            onAddClick()
        }
    }) { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) }
}

@Composable
fun NotesToolBar(
    navController: NavController,
    workspaceId: String,
    query: String,
    isGridView: Boolean,
    onImportNote: () -> Unit,
    onChangeView: () -> Unit,
    onSearch: (String) -> Unit
) {
    var onSearchClicked by remember { mutableStateOf(false) }

    if (!onSearchClicked) {
        Row {
            IconButton({ onSearchClicked = true }) {
                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onChangeView) {
                val icon = when {
                    isGridView -> Icons.Default.ViewStream
                    else -> Icons.Default.GridView
                }
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onImportNote) {
                Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.primary)
            }
            IconButton({
                navController.navigate(
                    NavRoutes.NoteDetails.withArgs(
                        workspaceId,
                        ""
                    )
                )
            }) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    } else {
        NotesSearchBar(
            query = query,
            onQueryChange = { onSearch(it) },
            onCloseClicked = { onSearchClicked = false },
            modifier = Modifier.width(200.dp)
        )
    }
}