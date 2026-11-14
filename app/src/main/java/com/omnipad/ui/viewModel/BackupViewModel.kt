package com.omnipad.ui.viewModel

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnipad.data.database.OmniPadDatabase
import com.omnipad.other.FullBackupArchive
import com.omnipad.other.crypto.BackupCredentialsStore
import com.omnipad.other.crypto.BackupCryptoException
import com.omnipad.other.crypto.BackupEncryptor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BackupOperation {
    BACKUP,
    RESTORE
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val db: OmniPadDatabase,
    private val backupEncryptor: BackupEncryptor,
    private val credentialsStore: BackupCredentialsStore
) : ViewModel() {

    private val _backupResult = MutableSharedFlow<Result<Unit>>()
    val backupResult = _backupResult.asSharedFlow()

    private val _passwordRequired = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val passwordRequired = _passwordRequired.asSharedFlow()

    private val _operation = MutableStateFlow<BackupOperation?>(null)
    val operation = _operation.asStateFlow()

    fun exportBackup(context: Context) {
        if (_operation.value != null) return
        _operation.value = BackupOperation.BACKUP
        viewModelScope.launch(Dispatchers.IO) {
            runBackupOperation {
                val storageRootUri = db.settingsDao.loadSetting()
                    ?.storageRootUri
                    ?.toUri()
                    ?: error("Storage root not selected")
                val password = credentialsStore.getPassword()
                try {
                    FullBackupArchive.create(context, db, storageRootUri, backupEncryptor, password)
                } finally {
                    password?.fill('\u0000')
                }
            }
        }
    }

    fun importBackup(context: Context, backupFolderUri: Uri) {
        if (_operation.value != null) return
        _operation.value = BackupOperation.RESTORE
        viewModelScope.launch(Dispatchers.IO) {
            attemptImport(context, backupFolderUri, credentialsStore.getPassword())
        }
    }

    fun importBackupWithPassword(context: Context, backupFolderUri: Uri, password: CharArray) {
        if (_operation.value != null) {
            password.fill('\u0000')
            return
        }
        _operation.value = BackupOperation.RESTORE
        viewModelScope.launch(Dispatchers.IO) {
            attemptImport(context, backupFolderUri, password)
        }
    }

    private suspend fun attemptImport(context: Context, backupFolderUri: Uri, password: CharArray?) {
        try {
            val storageRootUri = db.settingsDao.loadSetting()
                ?.storageRootUri
                ?.toUri()
            FullBackupArchive.restore(
                context,
                db,
                backupFolderUri,
                storageRootUri,
                backupEncryptor,
                password
            )
            _backupResult.emit(Result.success(Unit))
        } catch (exception: FullBackupArchive.PasswordRequiredException) {
            _passwordRequired.emit(backupFolderUri)
        } catch (exception: BackupCryptoException.WrongPasswordOrCorrupted) {
            _passwordRequired.emit(backupFolderUri)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            _backupResult.emit(Result.failure(exception))
        } finally {
            password?.fill('\u0000')
            _operation.value = null
        }
    }

    private suspend fun runBackupOperation(operation: suspend () -> Unit) {
        try {
            operation()
            _backupResult.emit(Result.success(Unit))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            _backupResult.emit(Result.failure(exception))
        } finally {
            _operation.value = null
        }
    }
}
