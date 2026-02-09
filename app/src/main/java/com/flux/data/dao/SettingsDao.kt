package com.flux.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flux.data.model.SettingsModel
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: SettingsModel)

    @Query("SELECT * FROM SettingsModel LIMIT 1")
    fun loadSettings(): Flow<SettingsModel?>

    @Query("SELECT * FROM SettingsModel LIMIT 1")
    fun loadSetting(): SettingsModel?
}