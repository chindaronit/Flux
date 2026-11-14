package com.flux.other

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.flux.data.database.BackupMediaFile
import com.flux.data.database.FluxBackup
import com.flux.data.database.FluxDatabase
import com.flux.other.crypto.BackupEncryptor
import com.flux.other.crypto.BackupEncryptionSession
import com.flux.other.crypto.BackupFileFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object FullBackupArchive {
    private const val MAX_CONCURRENT_MEDIA_COPIES = 4
    private val backupFolderTimestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    private const val MANIFEST_FILE = "backup.json"
    private const val COMPLETE_MARKER = ".complete"
    private const val MEDIA_DIRECTORY = "media"
    private const val IMAGE_DIRECTORY = "images"
    private const val AUDIO_DIRECTORY = "audio"
    private const val VIDEO_DIRECTORY = "videos"
    private const val DOCUMENT_DIRECTORY = "documents"
    private const val RESTORE_STAGING_DIRECTORY = "RestoreStaging"

    private val mediaDirectories = listOf(
        IMAGE_DIRECTORY to Constants.File.FLUX_IMAGES,
        AUDIO_DIRECTORY to Constants.File.FLUX_AUDIO,
        VIDEO_DIRECTORY to Constants.File.FLUX_VIDEOS,
        DOCUMENT_DIRECTORY to Constants.File.FLUX_DOCUMENTS
    )

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    class PasswordRequiredException : Exception()

    suspend fun create(
        context: Context,
        db: FluxDatabase,
        storageRootUri: Uri,
        encryptor: BackupEncryptor,
        password: CharArray?
    ) {
        val root = DocumentFile.fromTreeUri(context, storageRootUri)
            ?: throw IOException("The storage root is unavailable")
        create(AndroidBackupDocument(context, root), RoomBackupDataStore(context, db), encryptor, password)
    }

    internal suspend fun create(
        root: BackupDocument,
        dataStore: BackupDataStore,
        encryptor: BackupEncryptor,
        password: CharArray?
    ) {
        val fluxDirectory = findOrCreateDirectory(root, Constants.File.FLUX)
        val backupParent = findOrCreateDirectory(fluxDirectory, Constants.File.FLUX_BACKUP)
        val snapshot = dataStore.loadBackup()

        val timestamp = LocalDateTime.now().format(backupFolderTimestamp)
        val backupDirectory = createUniqueDirectory(backupParent, "FullBackup-$timestamp")
        val encryptionSession = password?.let { encryptor.createSession(it) }
        val mediaDirectory = findOrCreateDirectory(backupDirectory, MEDIA_DIRECTORY)
        val outputDirectories = mediaDirectories.associate { (archiveName, _) ->
            archiveName to findOrCreateDirectory(mediaDirectory, archiveName)
        }
        val archivedNames = mutableMapOf<Pair<String, String>, String>()
        val mediaFiles = mutableListOf<BackupMediaFile>()
        val copyTasks = mutableListOf<MediaCopyTask>()
        val sourceFluxDirectory = root.findFile(Constants.File.FLUX)
        mediaDirectories.forEach { (archiveName, sourceName) ->
            val sourceDirectory = sourceFluxDirectory?.findFile(sourceName)
            if (sourceDirectory?.isDirectory == true) {
                sourceDirectory.listFiles().filterNot { it.isDirectory }.forEach { source ->
                    val name = requireSafeFileName(source.name)
                    val archivedName = if (password == null) name else "media-${UUID.randomUUID()}"
                    if (archivedNames.put(archiveName to name, archivedName) != null) {
                        throw IOException("Media contains duplicate file names")
                    }
                    val mimeType = source.type ?: "application/octet-stream"
                    mediaFiles += BackupMediaFile(archiveName, name, archivedName, mimeType)
                    copyTasks += MediaCopyTask(
                        source = source,
                        destinationDirectory = outputDirectories.getValue(archiveName),
                        archivedName = archivedName,
                        mimeType = mimeType
                    )
                }
            }
        }

        val coverFiles = snapshot.workspaces.mapNotNull { workspace ->
            if (workspace.cover.isBlank()) return@mapNotNull null

            val coverName = URI(workspace.cover).path
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
                ?: throw IOException("Could not identify the cover image for ${workspace.title}")
            if ((IMAGE_DIRECTORY to coverName) !in archivedNames) {
                throw IOException("Could not find the cover image for ${workspace.title} in Flux/Images")
            }
            workspace.workspaceId to coverName
        }.toMap()

        copyMediaFiles(copyTasks, encryptor, encryptionSession)

        val portableSnapshot = snapshot.copy(
            workspaces = snapshot.workspaces.map { it.copy(cover = "") },
            workspaceCovers = coverFiles,
            mediaFiles = mediaFiles
        )
        writeManifest(
            backupDirectory,
            json.encodeToString(FluxBackup.serializer(), portableSnapshot),
            encryptor,
            encryptionSession
        )
        writeCompleteMarker(backupDirectory)
    }

    private suspend fun copyMediaFiles(
        tasks: List<MediaCopyTask>,
        encryptor: BackupEncryptor,
        encryptionSession: BackupEncryptionSession?
    ) {
        val mode = if (encryptionSession == null) CopyMode.PLAIN else CopyMode.ENCRYPT
        tasks.chunked(MAX_CONCURRENT_MEDIA_COPIES).forEach { batch ->
            coroutineScope {
                batch.map { task ->
                    async(Dispatchers.IO) {
                        copyDocument(
                            task.source,
                            task.destinationDirectory,
                            task.archivedName,
                            encryptor,
                            encryptionSession,
                            mode,
                            task.mimeType
                        )
                    }
                }.awaitAll()
            }
        }
    }

    suspend fun restore(
        context: Context,
        db: FluxDatabase,
        backupFolderUri: Uri,
        storageRootUri: Uri?,
        encryptor: BackupEncryptor,
        password: CharArray?
    ) {
        val backupDirectory = DocumentFile.fromTreeUri(context, backupFolderUri)
            ?: throw IOException("The selected backup folder is unavailable")
        val storageRoot = storageRootUri?.let { uri ->
            DocumentFile.fromTreeUri(context, uri)
        }
        restore(
            AndroidBackupDocument(context, backupDirectory),
            storageRoot?.let { AndroidBackupDocument(context, it) },
            RoomBackupDataStore(context, db),
            encryptor,
            password
        ) {
            MediaCache.clearCaches()
        }
    }

    internal suspend fun restore(
        backupDirectory: BackupDocument,
        storageRoot: BackupDocument?,
        dataStore: BackupDataStore,
        encryptor: BackupEncryptor,
        password: CharArray?,
        onMediaRestored: () -> Unit = {}
    ) {
        val completeMarker = backupDirectory.findFile(COMPLETE_MARKER)
        if (completeMarker != null) {
            val markerContents = completeMarker.openInputStream()
                ?.bufferedReader()
                ?.use { it.readText() }
            if (markerContents != "2") throw IOException("The backup was not completed successfully")
        }
        val manifest = backupDirectory.findFile(MANIFEST_FILE)
            ?: throw IOException("The selected folder does not contain a Flux backup")
        val encryptedManifest = manifest.openInputStream()?.use {
            it.readBytes()
        } ?: throw IOException("Could not read the backup manifest")
        val isEncrypted = BackupFileFormat.isEncrypted(encryptedManifest)
        if (isEncrypted && password == null) throw PasswordRequiredException()
        val decryptedManifest = if (isEncrypted) {
            encryptor.decryptManifest(encryptedManifest, password!!)
        } else {
            null
        }
        val manifestBytes = decryptedManifest?.plaintext ?: encryptedManifest
        val backup = json.decodeFromString<FluxBackup>(manifestBytes.toString(Charsets.UTF_8))
        require(backup.formatVersion in 2..4) {
            "Unsupported backup version: ${backup.formatVersion}"
        }

        val archiveMedia = backupDirectory.findFile(MEDIA_DIRECTORY)
        if (archiveMedia?.isDirectory != true) {
            throw IOException("The backup is missing its media folder")
        }
        val hasMediaFiles = backup.formatVersion >= 3 ||
            backup.workspaceCovers.isNotEmpty() ||
            backup.mediaFiles.isNotEmpty() ||
            mediaDirectories.any { (name, _) ->
                archiveMedia.findFile(name)?.listFiles()?.any { it.isFile } == true
            }
        if (hasMediaFiles && storageRoot == null) {
            throw IOException("Select a storage root before restoring media")
        }

        val restoredMediaUris = if (storageRoot != null) {
            restoreMedia(
                backup,
                archiveMedia,
                storageRoot,
                encryptor,
                decryptedManifest?.session,
                isEncrypted
            )
                .also { onMediaRestored() }
        } else {
            emptyMap()
        }
        val restoredWorkspaces = backup.workspaces.map { workspace ->
            val coverName = backup.workspaceCovers[workspace.workspaceId]
            if (coverName == null) {
                workspace
            } else {
                val coverUri = restoredMediaUris[IMAGE_DIRECTORY to requireSafeFileName(coverName)]
                    ?: throw IOException("The backup is missing workspace cover $coverName")
                workspace.copy(cover = coverUri)
            }
        }

        dataStore.replaceWith(backup.copy(workspaces = restoredWorkspaces))
    }

    private suspend fun restoreMedia(
        backup: FluxBackup,
        archiveMedia: BackupDocument,
        storageRoot: BackupDocument,
        encryptor: BackupEncryptor,
        encryptionSession: BackupEncryptionSession?,
        isEncrypted: Boolean
    ): Map<Pair<String, String>, String> {
        val fluxDirectory = findOrCreateDirectory(storageRoot, Constants.File.FLUX)
        val stagingDirectory = createUniqueDirectory(
            fluxDirectory,
            "$RESTORE_STAGING_DIRECTORY-${System.currentTimeMillis()}"
        )
        try {
            val stagingMedia = findOrCreateDirectory(stagingDirectory, MEDIA_DIRECTORY)
            val archivedMediaFiles = backup.mediaFiles.associateBy {
                it.directory to it.archivedName
            }
            if (archivedMediaFiles.size != backup.mediaFiles.size) {
                throw IOException("Backup contains duplicate media entries")
            }
            val restoredArchiveNames = mutableSetOf<Pair<String, String>>()
            mediaDirectories.forEach { (archiveName, _) ->
                val stagedCategory = findOrCreateDirectory(stagingMedia, archiveName)
                val archiveCategory = archiveMedia.findFile(archiveName)
                if (archiveCategory?.isDirectory == true) {
                    archiveCategory.listFiles().filterNot { it.isDirectory }.forEach { source ->
                        val archivedName = requireSafeFileName(source.name)
                        val key = archiveName to archivedName
                        val mediaEntry = if (backup.mediaFiles.isEmpty()) {
                            null
                        } else {
                            val entry = archivedMediaFiles[key]
                                ?: throw IOException("Backup contains an unlisted media file")
                            restoredArchiveNames += key
                            entry
                        }
                        val name = requireSafeFileName(mediaEntry?.name ?: archivedName)
                        copyDocument(
                            source,
                            stagedCategory,
                            name,
                            encryptor,
                            encryptionSession,
                            if (isEncrypted) CopyMode.DECRYPT else CopyMode.PLAIN,
                            mediaEntry?.mimeType
                        )
                    }
                }
            }
            if (restoredArchiveNames.size != archivedMediaFiles.size) {
                throw IOException("Backup is missing one or more media files")
            }

            val restoredUris = mutableMapOf<Pair<String, String>, String>()
            mediaDirectories.forEach { (archiveName, storageName) ->
                if (archiveName == DOCUMENT_DIRECTORY && backup.formatVersion < 3) {
                    return@forEach
                }
                val target = findOrCreateDirectory(fluxDirectory, storageName)
                deleteChildren(target)
                val stagedCategory = stagingMedia.findFile(archiveName)
                stagedCategory?.listFiles()?.filterNot { it.isDirectory }?.forEach { stagedFile ->
                    val name = requireSafeFileName(stagedFile.name)
                    val restored = copyDocument(
                        stagedFile,
                        target,
                        name,
                        encryptor,
                        null,
                        CopyMode.PLAIN
                    )
                    restoredUris[archiveName to name] = restored.uri
                }
            }

            backup.workspaceCovers.values.forEach { coverName ->
                if ((IMAGE_DIRECTORY to requireSafeFileName(coverName)) !in restoredUris) {
                    throw IOException("The backup is missing workspace cover $coverName")
                }
            }
            return restoredUris
        } finally {
            deleteDirectory(stagingDirectory)
        }
    }

    private fun findOrCreateDirectory(parent: BackupDocument, name: String): BackupDocument {
        parent.findFile(name)?.let { existing ->
            if (existing.isDirectory) return existing
            throw IOException("$name exists but is not a folder")
        }
        return parent.createDirectory(name) ?: throw IOException("Could not create folder $name")
    }

    private fun createUniqueDirectory(parent: BackupDocument, baseName: String): BackupDocument {
        repeat(100) { suffix ->
            val name = if (suffix == 0) baseName else "$baseName-$suffix"
            if (parent.findFile(name) == null) {
                return parent.createDirectory(name)
                    ?: throw IOException("Could not create folder $name")
            }
        }
        throw IOException("Could not create a unique backup folder")
    }

    private suspend fun copyDocument(
        source: BackupDocument,
        destinationDirectory: BackupDocument,
        name: String,
        encryptor: BackupEncryptor,
        encryptionSession: BackupEncryptionSession?,
        mode: CopyMode,
        mimeType: String? = null
    ): BackupDocument {
        val outputFile = destinationDirectory.createFile(
            if (mode == CopyMode.ENCRYPT) "application/octet-stream"
            else mimeType ?: source.type ?: "application/octet-stream",
            requireSafeFileName(name)
        ) ?: throw IOException("Could not create file $name")
        try {
            val input = source.openInputStream()
                ?: throw IOException("Could not read file $name")
            input.use { sourceStream ->
                val output = outputFile.openOutputStream()
                    ?: throw IOException("Could not write file $name")
                output.use { destinationStream ->
                    when (mode) {
                        CopyMode.PLAIN -> sourceStream.copyTo(destinationStream)
                        CopyMode.ENCRYPT -> {
                            val session = encryptionSession
                                ?: throw IllegalStateException("Encryption session is missing")
                            encryptor.encrypt(sourceStream, destinationStream, session)
                        }
                        CopyMode.DECRYPT -> {
                            val session = encryptionSession
                                ?: throw IllegalStateException("Decryption session is missing")
                            encryptor.decrypt(sourceStream, destinationStream, session)
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            outputFile.delete()
            throw exception
        }
        return outputFile
    }

    private suspend fun writeManifest(
        directory: BackupDocument,
        text: String,
        encryptor: BackupEncryptor,
        encryptionSession: BackupEncryptionSession?
    ) {
        val file = directory.createFile(
            if (encryptionSession == null) "application/json" else "application/octet-stream",
            MANIFEST_FILE
        )
            ?: throw IOException("Could not create the backup manifest")
        val output = file.openOutputStream()
            ?: throw IOException("Could not write the backup manifest")
        output.use { destination ->
            val manifestBytes = text.toByteArray(Charsets.UTF_8)
            if (encryptionSession == null) {
                destination.write(manifestBytes)
            } else {
                encryptor.encrypt(ByteArrayInputStream(manifestBytes), destination, encryptionSession)
            }
        }
    }

    private fun writeCompleteMarker(directory: BackupDocument) {
        val marker = directory.createFile("text/plain", COMPLETE_MARKER)
            ?: throw IOException("Could not mark the backup as complete")
        marker.openOutputStream()?.bufferedWriter()?.use { writer ->
            writer.write("2")
        } ?: throw IOException("Could not mark the backup as complete")
    }

    private fun deleteChildren(directory: BackupDocument) {
        directory.listFiles().forEach { child ->
            if (child.isDirectory) deleteChildren(child)
            if (!child.delete()) throw IOException("Could not replace ${child.name}")
        }
    }

    private fun deleteDirectory(directory: BackupDocument) {
        deleteChildren(directory)
        if (!directory.delete()) throw IOException("Could not remove ${directory.name}")
    }

    private fun requireSafeFileName(name: String?): String {
        if (name.isNullOrBlank() || name == "." || name == ".." ||
            '/' in name || '\\' in name
        ) {
            throw IOException("Backup contains an invalid file name")
        }
        return name
    }

    private enum class CopyMode {
        PLAIN,
        ENCRYPT,
        DECRYPT
    }

    private data class MediaCopyTask(
        val source: BackupDocument,
        val destinationDirectory: BackupDocument,
        val archivedName: String,
        val mimeType: String
    )
}
