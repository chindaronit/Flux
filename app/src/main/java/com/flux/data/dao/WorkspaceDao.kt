package com.flux.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flux.data.model.WorkspaceModel
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT EXISTS(SELECT 1 FROM WorkspaceModel WHERE workspaceId = :workspaceId)")
    suspend fun exists(workspaceId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkspace(space: WorkspaceModel)

    @Insert(onConflict=OnConflictStrategy.REPLACE)
    suspend fun upsertWorkspaces(spaces: List<WorkspaceModel>)

    @Query("SELECT workspaceId FROM WorkspaceModel WHERE passKey IS NULL OR passKey = ''")
    fun observePublicWorkspaceIds(): Flow<List<String>>

    @Delete
    suspend fun deleteWorkspace(workspace: WorkspaceModel)

    @Query("SELECT * FROM WorkspaceModel")
    fun loadAllWorkspaces(): Flow<List<WorkspaceModel>>

    @Query("SELECT * FROM WorkspaceModel")
    suspend fun getAll(): List<WorkspaceModel>
}