package com.flux.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.flux.data.dao.SettingsDao
import com.flux.data.model.SettingsModel
import com.flux.di.IODispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dao: SettingsDao,
    @param:ApplicationContext private val context: Context,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher
) : SettingsRepository {

    override suspend fun upsertSettings(settings: SettingsModel) {
        return withContext(ioDispatcher) { dao.upsertSettings(settings) }
    }

    override fun loadSettings(): Flow<SettingsModel?> { return dao.loadSettings() }

    override suspend fun hasValidStorageRoot(): Boolean =
        withContext(ioDispatcher) {
            val settings = dao.loadSettings().firstOrNull() ?: return@withContext false
            val uriString = settings.storageRootUri ?: return@withContext false
            val uri = uriString.toUri()

            context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission }
        }

    override suspend fun saveStorageRoot(uri: Uri) =
        withContext(ioDispatcher) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val current = dao.loadSettings().firstOrNull() ?: SettingsModel()
            dao.upsertSettings(current.copy(storageRootUri = uri.toString()))
        }

    override suspend fun getStorageRoot(): Uri =
        withContext(ioDispatcher) {
            val uriString = dao.loadSettings()
                .firstOrNull()
                ?.storageRootUri
                ?: error("Storage root not selected")

            uriString.toUri()
        }
}