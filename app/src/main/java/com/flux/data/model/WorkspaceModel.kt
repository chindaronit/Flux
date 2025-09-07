package com.flux.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import com.flux.R
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class WorkspaceModel(
    @PrimaryKey
    val workspaceId: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val colorInd: Int = 0,
    val cover: String = "",
    val icon: Int = 48,
    val passKey: String = "",
    val isPinned: Boolean = false,
    val selectedSpaces: List<Int> = emptyList()
)

data class Space(
    val id: Int,
    val title: String,
    val icon: ImageVector
)

@Composable
fun getSpacesList(): List<Space> {
    return listOf(
        Space(1, stringResource(R.string.Notes), Icons.AutoMirrored.Default.Notes),
        Space(2, stringResource(R.string.To_Do), Icons.Default.TaskAlt),
        Space(3, stringResource(R.string.Events), Icons.Default.Event),
        Space(4, stringResource(R.string.Calendar), Icons.Default.CalendarMonth),
        Space(5, stringResource(R.string.Journal), Icons.Default.AutoStories),
        Space(6, stringResource(R.string.Habits), Icons.Default.EventAvailable),
        Space(7, stringResource(R.string.Analytics), Icons.Default.Analytics)
    )
}