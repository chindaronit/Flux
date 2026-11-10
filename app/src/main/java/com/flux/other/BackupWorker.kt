package com.flux.other

import android.content.Context
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.util.Log
import com.flux.R
import com.flux.di.DataModule
import com.flux.other.crypto.BackupCredentialsStore
import com.flux.other.crypto.BackupEncryptor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class BackupFrequency(val days: Int, val textRes: Int) {
    NEVER(0, R.string.never),
    DAILY(1, R.string.daily),
    WEEKLY(7, R.string.weekly),
    MONTHLY(30, R.string.monthly);
}

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val credentialsStore: BackupCredentialsStore,
    private val backupEncryptor: BackupEncryptor
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val context = applicationContext
            val database = DataModule.provideFluxDatabase(context)
            val storageRootUri = database.settingsDao.loadSetting()
                ?.storageRootUri
                ?.toUri()
                ?: throw IllegalStateException("Storage root not selected")
            val password = credentialsStore.getPassword()
            try {
                FullBackupArchive.create(context, database, storageRootUri, backupEncryptor, password)
            } finally {
                password?.fill('\u0000')
            }
            Result.success()
        } catch (exception: Exception) {
            Log.e("BackupWorker", "Full backup failed", exception)
            Result.failure()
        }
    }
}
