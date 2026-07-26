package com.flux.ui.viewModel

import androidx.lifecycle.viewModelScope
import com.flux.other.BackupFrequency
import com.flux.other.crypto.BackupCredentialsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupSettingsViewModel @Inject constructor(
    private val credentialsStore: BackupCredentialsStore
) : androidx.lifecycle.ViewModel() {

    val hasBackupPassword: StateFlow<Boolean> = credentialsStore.hasPasswordFlow()

    /**
     * Single source of truth for "auto-backup is currently misconfigured."
     * True whenever a non-NEVER frequency is set but no password exists on this device —
     * covers migration (pre-existing users), post-import, and live toggling identically.
     */
    suspend fun isAutoBackupUnsafe(currentFrequencyDays: Int): Boolean =
        currentFrequencyDays != BackupFrequency.NEVER.days && credentialsStore.getPassword() == null

    fun setPassword(password: CharArray, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            credentialsStore.setPassword(password)
            password.fill('\u0000')
            onDone()
        }
    }

    /** Explicit rotation: intentionally orphans any backup encrypted with the old password. */
    fun changePassword(newPassword: CharArray, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            credentialsStore.setPassword(newPassword)
            newPassword.fill('\u0000')
            onDone()
        }
    }

    fun disableEncryption(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            credentialsStore.setPassword(null)
            onDone()
        }
    }
}