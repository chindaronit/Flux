package com.flux.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class LabelModel(
    @PrimaryKey
    val labelId: String = UUID.randomUUID().toString(),
    val value: String = "",
    val workspaceId: String = ""
)
