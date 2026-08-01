package com.flux.di

import com.flux.other.crypto.AesGcmBackupEncryptor
import com.flux.other.crypto.BackupCredentialsStore
import com.flux.other.crypto.BackupEncryptor
import com.flux.other.crypto.EncryptedPrefsBackupCredentialsStore
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