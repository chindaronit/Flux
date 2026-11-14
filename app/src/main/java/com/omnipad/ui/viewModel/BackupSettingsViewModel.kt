package com.omnipad.ui.viewModel

import androidx.lifecycle.viewModelScope
import com.omnipad.other.crypto.BackupCredentialsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupSettingsViewModel @Inject constructor(
    private val credentialsStore: BackupCredentialsStore
) : androidx.lifecycle.ViewModel() {

    val hasBackupPassword: StateFlow<Boolean> = credentialsStore.hasPasswordFlow()

    fun setPassword(password: CharArray, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            credentialsStore.setPassword(password)
            password.fill('\u0000')
            onDone()
        }
    }

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