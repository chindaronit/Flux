package com.flux.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flux.data.model.LabelModel
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Query("SELECT EXISTS(SELECT 1 FROM LabelModel WHERE labelId= :labelId)")
    suspend fun exists(labelId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLabel(label: LabelModel)

    @Delete
    suspend fun deleteLabel(label: LabelModel)

    @Query("DELETE FROM LabelModel WHERE workspaceId = :workspaceId")
    suspend fun deleteAllWorkspaceLabels(workspaceId: String)

    @Query("SELECT * FROM LabelModel")
    fun loadAllLabels(): Flow<List<LabelModel>>

    @Query("SELECT * FROM LabelModel")
    suspend fun getAll(): List<LabelModel>
}
