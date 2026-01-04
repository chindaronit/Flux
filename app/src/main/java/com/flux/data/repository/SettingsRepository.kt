package com.flux.data.repository

import android.net.Uri
import com.flux.data.model.SettingsModel
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun upsertSettings(settings: SettingsModel)
    fun loadSettings(): Flow<SettingsModel?>
    suspend fun hasValidStorageRoot(): Boolean
    suspend fun saveStorageRoot(uri: Uri)
    suspend fun getStorageRoot(): Uri
}