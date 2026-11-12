package com.flux.other

import com.flux.data.database.FluxBackup
import java.io.InputStream
import java.io.OutputStream

internal interface BackupDocument {
    val name: String?
    val isDirectory: Boolean
    val isFile: Boolean
    val type: String?
    val uri: String

    fun findFile(name: String): BackupDocument?
    fun listFiles(): List<BackupDocument>
    fun createDirectory(name: String): BackupDocument?
    fun createFile(mimeType: String, name: String): BackupDocument?
    fun openInputStream(): InputStream?
    fun openOutputStream(): OutputStream?
    fun delete(): Boolean
}

internal interface BackupDataStore {
    suspend fun loadBackup(): FluxBackup
    suspend fun replaceWith(backup: FluxBackup)
}
