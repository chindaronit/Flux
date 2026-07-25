package com.flux.other.crypto

import com.flux.R
import androidx.annotation.StringRes

sealed class BackupCryptoException(
    @param:StringRes val messageRes: Int,
    vararg val formatArgs: Any,
    cause: Throwable? = null
) : Exception(null, cause) {

    class WrongPasswordOrCorrupted(cause: Throwable) : BackupCryptoException(
        R.string.backup_wrong_password_or_corrupted,
        cause = cause
    )

    class UnsupportedVersion(version: Int) : BackupCryptoException(
        R.string.unsupported_backup_version,
        version
    )
}