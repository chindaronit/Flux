package com.flux.other.crypto

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface BackupCredentialsStore {
    suspend fun getPassword(): CharArray?
    suspend fun setPassword(password: CharArray?)
    fun hasPasswordFlow(): StateFlow<Boolean>
}

/**
 * The ONE place the backup password lives. Deliberately outside Room and outside
 * FluxBackup/SettingsModel — it must never be written into an exported backup file.
 * Backed by Android Keystore via EncryptedSharedPreferences: protects against file
 * extraction / offline attacks, not against a fully unlocked device in an attacker's hand.
 * Lost on uninstall — by design, there is no recovery path.
 */

@Singleton
class EncryptedPrefsBackupCredentialsStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) : BackupCredentialsStore {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "flux_backup_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _hasPassword = MutableStateFlow(prefs.contains(KEY_PASSWORD))
    override fun hasPasswordFlow(): StateFlow<Boolean> = _hasPassword.asStateFlow()

    override suspend fun getPassword(): CharArray? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_PASSWORD, null)?.toCharArray()
    }

    override suspend fun setPassword(password: CharArray?) = withContext(Dispatchers.IO) {
        if (password == null) {
            prefs.edit { remove(KEY_PASSWORD) }
        } else {
            prefs.edit { putString(KEY_PASSWORD, String(password)) }
        }
        _hasPassword.value = (password != null)
    }

    private companion object {
        const val KEY_PASSWORD = "backup_password"
    }
}