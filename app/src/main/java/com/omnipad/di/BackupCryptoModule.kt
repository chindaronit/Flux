package com.omnipad.di

import com.omnipad.other.crypto.AesGcmBackupEncryptor
import com.omnipad.other.crypto.BackupCredentialsStore
import com.omnipad.other.crypto.BackupEncryptor
import com.omnipad.other.crypto.EncryptedPrefsBackupCredentialsStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupCryptoModule {

    @Binds
    @Singleton
    abstract fun bindBackupCredentialsStore(
        impl: EncryptedPrefsBackupCredentialsStore
    ): BackupCredentialsStore

    companion object {
        @Provides
        @Singleton
        fun provideBackupEncryptor(): BackupEncryptor = AesGcmBackupEncryptor()
    }
}