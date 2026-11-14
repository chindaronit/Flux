package com.omnipad.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity
data class ProgressBoardModel(
    @PrimaryKey
    val itemId: String = UUID.randomUUID().toString(),
    val workspaceId: String = "",
    val title: String = "",
    val description: String = "",
    val startDate: Long = -1L,
    val endDate: Long = -1L,
    val icon: Int = 7,
    val status: Int = 0
)