package com.omnipad.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.omnipad.data.model.ProgressBoardModel
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressBoardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBoardItem(item: ProgressBoardModel)

    @Query("SELECT * FROM ProgressBoardModel")
    fun getProgressBoardData(): Flow<List<ProgressBoardModel>>

    @Delete
    suspend fun deleteBoardItem(item: ProgressBoardModel)

    @Query("Delete FROM ProgressBoardModel where workspaceId = :workspaceId")
    suspend fun deleteBoardItemsByWorkspace(workspaceId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM ProgressBoardModel WHERE itemId = :id)")
    suspend fun exists(id: String): Boolean

    @Query("SELECT * FROM ProgressBoardModel")
    fun getAllBoardItems(): List<ProgressBoardModel>

    @Query("DELETE FROM ProgressBoardModel")
    suspend fun deleteAll()
}